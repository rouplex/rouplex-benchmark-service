package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpConnectorLifecycleListener;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapCounter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapHistogram;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapTimer;

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

public class BenchmarkServiceProvider implements BenchmarkService, Closeable {
    private static Logger logger = Logger.getLogger(BenchmarkServiceProvider.class.getSimpleName());

    static BenchmarkServiceProvider benchmarkServiceProvider;

    public static BenchmarkServiceProvider get() throws Exception {
        synchronized (BenchmarkServiceProvider.class) {
            if (benchmarkServiceProvider == null) {
                benchmarkServiceProvider = new BenchmarkServiceProvider();
            }

            return benchmarkServiceProvider;
        }
    }

    final Map<Provider, RouplexTcpBinder> sharedTcpBinders = new HashMap<Provider, RouplexTcpBinder>();
    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final AtomicInteger incrementalId = new AtomicInteger();
    final Random random = new Random();
    Map<String, Closeable> closeables = new HashMap<>();

    final RouplexTcpConnectorLifecycleListener<RouplexTcpClient>
            rouplexTcpClientLifecycleListener = new RouplexTcpConnectorLifecycleListener<RouplexTcpClient>() {

        @Override
        public void onCreated(RouplexTcpClient rouplexTcpClient) {
            try {
                if (rouplexTcpClient.getRouplexTcpServer() == null) {
                    ((EchoRequester) rouplexTcpClient.getAttachment()).startSendingThenClose();
                } else {
                    new EchoResponder((Request) rouplexTcpClient.getRouplexTcpServer().getAttachment(),
                            BenchmarkServiceProvider.this, rouplexTcpClient);
                }
            } catch (Exception e) {
                benchmarkerMetrics.meter(MetricRegistry.name("EEE", e.getMessage()));
                e.printStackTrace();
                logger.warning(String.format("Failed handling clientConnected.onEvent(). Cause: %s: %s",
                        e.getClass(), e.getMessage()));
            }
        }

        @Override
        public void onDestroyed(RouplexTcpClient rouplexTcpClient, boolean drainedChannels) {
            try {
                Meter meter;

                if (rouplexTcpClient.getRouplexTcpServer() != null) {
                    EchoReporter echoReporter = ((EchoResponder) rouplexTcpClient.getAttachment()).echoReporter;
                    meter = drainedChannels ? echoReporter.disconnectedOk : echoReporter.disconnectedKo;
                    logger.info(String.format("Disconnected [%s] EchoReporter", drainedChannels ? "drained" : "undrained"));
                } else {
                    EchoReporter echoReporter = ((EchoRequester) rouplexTcpClient.getAttachment()).echoReporter;
                    if (echoReporter == null) {
                        meter = benchmarkerMetrics.meter(MetricRegistry.name("connection.failed"));
                        logger.info("Failed connecting EchoRequester");
                    } else {
                        meter = drainedChannels ? echoReporter.disconnectedOk : echoReporter.disconnectedKo;
                        logger.info(String.format("Disconnected [%s] EchoRequester", drainedChannels ? "drained" : "undrained"));
                    }
                }

                meter.mark();
            } catch (Exception e) {
                benchmarkerMetrics.meter(MetricRegistry.name("EEE", e.getMessage()));
                logger.warning(String.format("Failed handling clientClosed.onEvent(). Cause: %s: %s",
                        e.getClass(), e.getMessage()));
            }
        }
    };

    BenchmarkServiceProvider() throws Exception {
        sharedTcpBinders.put(Provider.CLASSIC_NIO, createRouplexTcpBinder(Provider.CLASSIC_NIO));
        sharedTcpBinders.put(Provider.ROUPLEX_NIOSSL, createRouplexTcpBinder(Provider.ROUPLEX_NIOSSL));
        sharedTcpBinders.put(Provider.SCALABLE_SSL, createRouplexTcpBinder(Provider.SCALABLE_SSL));

        addCloseable(JmxReporter.forRegistry(benchmarkerMetrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build()
        ).start();
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        if (request.getProvider() == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        try {
            logger.info(String.format("Creating EchoServer at %s:%s", request.getHostname(), request.getPort()));
            RouplexTcpBinder tcpBinder = request.isUseSharedBinder()
                    ? sharedTcpBinders.get(request.getProvider()) : createRouplexTcpBinder(request.getProvider());

            ServerSocketChannel serverSocketChannel;
            switch (request.getProvider()) {
                case ROUPLEX_NIOSSL:
                    serverSocketChannel = org.rouplex.nio.channels.SSLServerSocketChannel.open(SSLContext.getDefault());
                    break;
                case SCALABLE_SSL:
                    serverSocketChannel = scalablessl.SSLServerSocketChannel.open(SSLContext.getDefault());
                    break;
                case CLASSIC_NIO:
                default:
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
            addCloseable(hostPort, rouplexTcpServer);

            StartTcpServerResponse response = new StartTcpServerResponse();
            response.setHostaddress(isa.getAddress().getHostAddress());
            response.setHostname(isa.getHostName());
            response.setPort(isa.getPort());
            return response;
        } catch (Exception e) {
            logger.warning(String.format("Failed creating EchoServer at %s:%s. Cause: %s. %s",
                    request.getHostname(), request.getPort(), e.getClass(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        String hostPort = String.format("%s:%s", request.getHostname().replace('.', '-'), request.getPort());
        if (!closeAndRemove(hostPort)) {
            throw new IOException("There is no EchoServer listening at " + hostPort);
        }

        return new StopTcpServerResponse();
    }

    @Override
    public StartTcpClientsResponse startTcpClients(final StartTcpClientsRequest request) throws Exception {
        if (request.getProvider() == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        InetSocketAddress inetSocketAddress = new InetSocketAddress(request.getHostname(), request.getPort());
        if (inetSocketAddress.isUnresolved()) {
            throw new IllegalArgumentException(String.format("Unresolved address %s", inetSocketAddress));
        }

        logger.info(String.format("Creating %s EchoClients for server at %s:%s",
                request.clientCount, request.getHostname(), request.getPort()));

        RouplexTcpBinder tcpBinder = request.isUseSharedBinder()
                ? sharedTcpBinders.get(request.getProvider()) : createRouplexTcpBinder(request.getProvider());

        for (int cc = 0; cc < request.clientCount; cc++) {
            long startClientMillis = request.minDelayMillisBeforeCreatingClient + random.nextInt(
                    request.maxDelayMillisBeforeCreatingClient - request.minDelayMillisBeforeCreatingClient);
            logger.info(String.format("Scheduled creation of EchoRequester in %s milliseconds.", startClientMillis));

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    new EchoRequester(BenchmarkServiceProvider.this, request, tcpBinder);
                }
            }, startClientMillis, TimeUnit.MILLISECONDS);
        }

        return new StartTcpClientsResponse();
    }

    @Override
    public GetSnapshotMetricsResponse getSnapshotMetricsResponse(GetSnapshotMetricsRequest request) throws Exception {
        SortedMap<String, SnapCounter> counters = new TreeMap<>();
        for (Map.Entry<String, Counter> entry : benchmarkerMetrics.getCounters().entrySet()) {
            counters.put(entry.getKey(), metricsUtil.snapCounter(entry.getValue()));
        }

        SortedMap<String, SnapMeter> meters = new TreeMap<>();
        for (Map.Entry<String, Meter> entry : benchmarkerMetrics.getMeters().entrySet()) {
            meters.put(entry.getKey(), metricsUtil.snapMeter(entry.getValue()));
        }

        SortedMap<String, SnapHistogram> histograms = new TreeMap<>();
        for (Map.Entry<String, Histogram> entry : benchmarkerMetrics.getHistograms().entrySet()) {
            histograms.put(entry.getKey(), metricsUtil.snapHistogram(entry.getValue()));
        }

        SortedMap<String, SnapTimer> timers = new TreeMap<>();
        for (Map.Entry<String, Timer> entry : benchmarkerMetrics.getTimers().entrySet()) {
            timers.put(entry.getKey(), metricsUtil.snapTimer(entry.getValue()));
        }

        GetSnapshotMetricsResponse response = new GetSnapshotMetricsResponse();
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
        rouplexTcpBinder.setRouplexTcpClientLifecycleListener(rouplexTcpClientLifecycleListener);
        return addCloseable(rouplexTcpBinder);
    }

    protected <T extends Closeable> T addCloseable(T t) {
        return addCloseable(UUID.randomUUID().toString(), t);
    }

    protected boolean closeAndRemove(String closeableId) throws IOException {
        Closeable closeable = closeables.remove(closeableId);
        if (closeable == null) {
            return false;
        }

        closeable.close();
        return true;
    }

    protected <T extends Closeable> T addCloseable(String id, T t) {
        synchronized (scheduledExecutor) {
            if (isClosed()) {
                throw new IllegalStateException("Already closed.");
            }

            closeables.put(id, t);
            return t;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (scheduledExecutor) {
            for (Closeable closeable : closeables.values()) {
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