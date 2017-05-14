package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpClientListener;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmark.Util;
import org.rouplex.service.benchmark.management.SnapCounter;
import org.rouplex.service.benchmark.management.SnapHistogram;
import org.rouplex.service.benchmark.management.SnapMeter;
import org.rouplex.service.benchmark.management.SnapTimer;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkWorkerServiceProvider implements BenchmarkWorkerService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkWorkerServiceProvider.class.getSimpleName());

    private static BenchmarkWorkerService benchmarkWorkerService;
    public static BenchmarkWorkerService get() throws Exception {
        synchronized (BenchmarkWorkerServiceProvider.class) {
            if (benchmarkWorkerService == null) {
                benchmarkWorkerService = new BenchmarkWorkerServiceProvider();
            }

            return benchmarkWorkerService;
        }
    }

    final Map<Provider, RouplexTcpBinder> sharedTcpBinders = new HashMap<Provider, RouplexTcpBinder>();
    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final AtomicInteger incrementalId = new AtomicInteger();
    final Random random = new Random();
    Set<Closeable> closeables = new HashSet<Closeable>();

    Map<String, Object> jobs = new HashMap<String, Object>(); // add implementation later
    Map<String, RouplexTcpServer> tcpServers = new HashMap<String, RouplexTcpServer>();

    final RouplexTcpClientListener rouplexTcpClientListener = new RouplexTcpClientListener() {
        @Override
        public void onConnected(RouplexTcpClient rouplexTcpClient) {
            try {
                if (rouplexTcpClient.getRouplexTcpServer() == null) {
                    ((EchoRequester) rouplexTcpClient.getAttachment()).startSendingThenClose(rouplexTcpClient);
                } else {
                    new EchoResponder((StartTcpServerRequest) rouplexTcpClient.getRouplexTcpServer().getAttachment(),
                            BenchmarkWorkerServiceProvider.this, rouplexTcpClient);
                }
            } catch (Exception e) {
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnected")).mark();
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnected.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onConnected(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }

        @Override
        public void onConnectionFailed(RouplexTcpClient rouplexTcpClient, Exception reason) {
            try {
                benchmarkerMetrics.meter(MetricRegistry.name("connection.failed")).mark();
                benchmarkerMetrics.meter(MetricRegistry.name("connection.failed.EEE",
                        reason.getClass().getSimpleName(), reason.getMessage())).mark();

                logger.warning(String.format("Failed connecting EchoRequester. Cause: %s: %s",
                        reason.getClass().getSimpleName(), reason.getMessage()));
            } catch (Exception e) {
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnectionFailed")).mark();
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnectionFailed.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onConnectionFailed(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }

        @Override
        public void onDisconnected(RouplexTcpClient rouplexTcpClient,
                                   Exception optionalReason, boolean drainedChannels) {
            try {
                EchoReporter echoReporter = rouplexTcpClient.getRouplexTcpServer() == null
                        ? ((EchoRequester) rouplexTcpClient.getAttachment()).echoReporter
                        : ((EchoResponder) rouplexTcpClient.getAttachment()).echoReporter;

                echoReporter.liveConnections.mark(-1);
                (drainedChannels ? echoReporter.disconnectedOk : echoReporter.disconnectedKo).mark();

                logger.info(String.format("Disconnected %s. Drained: %s",
                        rouplexTcpClient.getAttachment().getClass().getSimpleName(), drainedChannels));
            } catch (Exception e) {
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onDisconnected")).mark();
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onDisconnected.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onDisconnected(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    };

    BenchmarkWorkerServiceProvider() throws Exception {
        sharedTcpBinders.put(Provider.CLASSIC_NIO, createRouplexTcpBinder(Provider.CLASSIC_NIO));

        try {
            sharedTcpBinders.put(Provider.ROUPLEX_NIOSSL, createRouplexTcpBinder(Provider.ROUPLEX_NIOSSL));
        } catch (Exception e) {
            // we tried
        }

        try {
            sharedTcpBinders.put(Provider.SCALABLE_SSL, createRouplexTcpBinder(Provider.SCALABLE_SSL));
        } catch (Exception e) {
            // we tried
        }

        addCloseable(JmxReporter.forRegistry(benchmarkerMetrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build()
        ).start();
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        Util.checkNonNullArg(request.getProvider(), "Provider");

        if (request.getOptionalJobId() == null) {
            request.setOptionalJobId(UUID.randomUUID().toString());
        }

        try {
            logger.info(String.format("Creating EchoServer at %s:%s", request.getHostname(), request.getPort()));
            RouplexTcpBinder tcpBinder = request.isUseSharedBinder()
                    ? sharedTcpBinders.get(request.getProvider()) : createRouplexTcpBinder(request.getProvider());

            ServerSocketChannel serverSocketChannel;

            if (request.isSsl()) {
                switch (request.getProvider()) {
                    case ROUPLEX_NIOSSL:
                        serverSocketChannel = org.rouplex.nio.channels
                                .SSLServerSocketChannel.open(SSLContext.getDefault());
                        break;
                    case SCALABLE_SSL:
                        serverSocketChannel = scalablessl
                                .SSLServerSocketChannel.open(SSLContext.getDefault());
                        break;
                    case CLASSIC_NIO:
                    default:
                        throw new Exception("This provider cannot provide ssl communication");
                }
            } else {
                serverSocketChannel = ServerSocketChannel.open();
            }

            RouplexTcpServer rouplexTcpServer = RouplexTcpServer.newBuilder()
                    .withRouplexTcpBinder(tcpBinder)
                    .withServerSocketChannel(serverSocketChannel)
                    .withLocalAddress(request.getHostname(), request.getPort())
                    .withSecure(request.isSsl(), null) // value ignored in current context since channel is provided
                    .withBacklog(request.getBacklog())
                    .withSendBufferSize(request.getSocketSendBufferSize())
                    .withReceiveBufferSize(request.getSocketReceiveBufferSize())
                    .withAttachment(request)
                    .build();

            InetSocketAddress isa = (InetSocketAddress) rouplexTcpServer.getLocalAddress(true);
            logger.info(String.format("Created EchoServer at %s:%s",
                    isa.getAddress().getHostAddress().replace('.', '-'), isa.getPort()));

            String hostPort = String.format("%s:%s", isa.getHostName().replace('.', '-'), isa.getPort());
            tcpServers.put(hostPort, rouplexTcpServer);

            StartTcpServerResponse response = new StartTcpServerResponse();
            response.setJobId(request.getOptionalJobId());
            response.setHostaddress(isa.getAddress().getHostAddress());
            response.setHostname(isa.getHostName());
            response.setPort(isa.getPort());
            return response;
        } catch (Exception e) {
            logger.warning(String.format("Failed creating EchoServer at %s:%s. Cause: %s: %s",
                    request.getHostname(), request.getPort(), e.getClass().getSimpleName(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        String hostPort = String.format("%s:%s", request.getHostname().replace('.', '-'), request.getPort());
        RouplexTcpServer tcpServer = tcpServers.remove(hostPort);

        if (tcpServer == null) {
            throw new IOException("There is no EchoServer listening at " + hostPort);
        }

        tcpServer.close();
        return new StopTcpServerResponse();
    }

    @Override
    public PollTcpEndPointStateResponse pollTcpServerState(PollTcpEndPointStateRequest request) throws Exception {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public StartTcpClientsResponse startTcpClients(final StartTcpClientsRequest request) throws Exception {
        Util.checkNonNullArg(request.getProvider(), "Provider");

        Util.checkNonNegativeArg(request.getClientCount(), "ClientCount");
        Util.checkNonNegativeArg(request.getMinClientLifeMillis(), "MinClientLifeMillis");
        Util.checkNonNegativeArg(request.getMinDelayMillisBeforeCreatingClient(), "MinDelayMillisBeforeCreatingClient");
        Util.checkNonNegativeArg(request.getMinDelayMillisBetweenSends(), "MinDelayMillisBetweenSends");
        Util.checkNonNegativeArg(request.getMinPayloadSize(), "MinPayloadSize");

        Util.checkPositiveArgDiff(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis(),
                "MinClientLifeMillis", "MaxClientLifeMillis");
        Util.checkPositiveArgDiff(request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient(),
                "MinDelayMillisBeforeCreatingClient", "MaxDelayMillisBeforeCreatingClient");
        Util.checkPositiveArgDiff(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends(),
                "MinDelayMillisBetweenSends", "MaxDelayMillisBetweenSends");
        Util.checkPositiveArgDiff(request.getMaxPayloadSize() - request.getMinPayloadSize(),
                "MinPayloadSize", "MaxPayloadSize");

        if (request.getOptionalJobId() == null) {
            request.setOptionalJobId(UUID.randomUUID().toString());
        }

        InetSocketAddress inetSocketAddress = new InetSocketAddress(request.getHostname(), request.getPort());
        if (inetSocketAddress.isUnresolved()) {
            throw new IllegalArgumentException(String.format("Unresolved address %s", inetSocketAddress));
        }

        logger.info(String.format("Creating %s EchoClients for server at %s:%s",
                request.clientCount, request.getHostname(), request.getPort()));

        final RouplexTcpBinder tcpBinder = request.isUseSharedBinder()
                ? sharedTcpBinders.get(request.getProvider()) : createRouplexTcpBinder(request.getProvider());

        for (int cc = 0; cc < request.clientCount; cc++) {
            long startClientMillis = request.minDelayMillisBeforeCreatingClient + random.nextInt(
                    request.maxDelayMillisBeforeCreatingClient - request.minDelayMillisBeforeCreatingClient);
            logger.info(String.format("Scheduled creation of EchoRequester in %s milliseconds.", startClientMillis));

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    new EchoRequester(BenchmarkWorkerServiceProvider.this, request, tcpBinder);
                }
            }, startClientMillis, TimeUnit.MILLISECONDS);
        }

        StartTcpClientsResponse response = new StartTcpClientsResponse();
        response.setJobId(request.getOptionalJobId());
        return response;
    }

    @Override
    public PollTcpEndPointStateResponse pollTcpClientsState(PollTcpEndPointStateRequest request) throws Exception {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public GetMetricsResponse getMetricsResponse(GetMetricsRequest request) throws Exception {
        SortedMap<String, SnapCounter> counters = new TreeMap<String, SnapCounter>();
        for (Map.Entry<String, Counter> entry : benchmarkerMetrics.getCounters().entrySet()) {
            counters.put(entry.getKey(), metricsUtil.snapCounter(entry.getValue()));
        }

        SortedMap<String, SnapMeter> meters = new TreeMap<String, SnapMeter>();
        for (Map.Entry<String, Meter> entry : benchmarkerMetrics.getMeters().entrySet()) {
            meters.put(entry.getKey(), metricsUtil.snapMeter(entry.getValue()));
        }

        SortedMap<String, SnapHistogram> histograms = new TreeMap<String, SnapHistogram>();
        for (Map.Entry<String, Histogram> entry : benchmarkerMetrics.getHistograms().entrySet()) {
            histograms.put(entry.getKey(), metricsUtil.snapHistogram(entry.getValue()));
        }

        SortedMap<String, SnapTimer> timers = new TreeMap<String, SnapTimer>();
        for (Map.Entry<String, Timer> entry : benchmarkerMetrics.getTimers().entrySet()) {
            timers.put(entry.getKey(), metricsUtil.snapTimer(entry.getValue()));
        }

        GetMetricsResponse response = new GetMetricsResponse();
        response.setCounters(counters);
        response.setMeters(meters);
        response.setHistograms(histograms);
        response.setTimers(timers);
        return response;
    }

    private RouplexTcpBinder createRouplexTcpBinder(Provider provider) throws Exception {
        Selector selector = null;

        switch (provider) {
            case CLASSIC_NIO:
                selector = java.nio.channels.Selector.open();
                break;
            case ROUPLEX_NIOSSL:
                selector = org.rouplex.nio.channels.SSLSelector.open();
                break;
            case SCALABLE_SSL:
                selector = scalablessl.SSLSelector.open(SSLContext.getDefault());
                break;
        }

        RouplexTcpBinder rouplexTcpBinder = new RouplexTcpBinder(selector);
        rouplexTcpBinder.setRouplexTcpClientListener(rouplexTcpClientListener);
        return addCloseable(rouplexTcpBinder);
    }

//    protected boolean closeAndRemove(String closeableId) throws IOException {
//        Closeable closeable = closeables.remove(closeableId);
//        if (closeable == null) {
//            return false;
//        }
//
//        closeable.close();
//        return true;
//    }

    protected <T extends Closeable> T addCloseable(T t) {
        synchronized (scheduledExecutor) {
            if (isClosed()) {
                throw new IllegalStateException("Already closed.");
            }

            closeables.add(t);
            return t;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (scheduledExecutor) {
            for (Closeable closeable : closeables) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            closeables = null;
            scheduledExecutor.shutdownNow();
        }
    }

    public boolean isClosed() {
        synchronized (scheduledExecutor) {
            return closeables == null;
        }
    }
}