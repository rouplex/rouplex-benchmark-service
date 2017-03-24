package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.rouplex.platform.rr.NotificationListener;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapCounter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapHistogram;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapTimer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BenchmarkServiceProvider implements BenchmarkService, Closeable {
    private static Logger logger = Logger.getLogger(BenchmarkServiceProvider.class.getSimpleName());

    static BenchmarkServiceProvider benchmarkServiceProvider;

    public static BenchmarkServiceProvider get() throws IOException {
        synchronized (BenchmarkServiceProvider.class) {
            if (benchmarkServiceProvider == null) {
                benchmarkServiceProvider = new BenchmarkServiceProvider();
            }

            return benchmarkServiceProvider;
        }
    }

    final RouplexTcpBinder classicTcpBinder;
    final RouplexTcpBinder niosslTcpBinder;
    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();

    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final JmxReporter jmxReporter = JmxReporter.forRegistry(benchmarkerMetrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    final AtomicInteger incrementalId = new AtomicInteger();
    final Random random = new Random();
    Map<String, Closeable> closeables = new HashMap<>();

    final NotificationListener<RouplexTcpClient> clientConnectedListener = new NotificationListener<RouplexTcpClient>() {
        @Override
        public void onEvent(RouplexTcpClient rouplexTcpClient) {
            try {
                if (rouplexTcpClient.getRouplexTcpServer() == null) {
                    new EchoRequester(BenchmarkServiceProvider.this, rouplexTcpClient);
                } else {
                    new EchoResponder(BenchmarkServiceProvider.this, rouplexTcpClient);
                }
            } catch (IOException ioe) {
                benchmarkerMetrics.meter(MetricRegistry.name("EEE", ioe.getMessage()));
            }
        }
    };

    final NotificationListener<RouplexTcpClient> clientClosedListener = new NotificationListener<RouplexTcpClient>() {
        @Override
        public void onEvent(RouplexTcpClient rouplexTcpClient) {
            if (rouplexTcpClient.getRouplexTcpServer() == null) {
                ((EchoRequester) rouplexTcpClient.getAttachment()).echoReporter.disconnected();
            } else {
                ((EchoResponder) rouplexTcpClient.getAttachment()).echoReporter.disconnected();
            }
        }
    };

    BenchmarkServiceProvider() throws IOException {
        classicTcpBinder = addCloseable(new RouplexTcpBinder(Selector.open(), null));
        classicTcpBinder.setRouplexTcpClientConnectedListener(clientConnectedListener);
        classicTcpBinder.setRouplexTcpClientClosedListener(clientClosedListener);

        niosslTcpBinder = addCloseable(new RouplexTcpBinder());
        niosslTcpBinder.setRouplexTcpClientConnectedListener(clientConnectedListener);
        niosslTcpBinder.setRouplexTcpClientClosedListener(clientClosedListener);

        addCloseable(jmxReporter).start();
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        logger.info(String.format("Creating EchoServer at %s:%s", request.getHostname(), request.getPort()));
        RouplexTcpServer rouplexTcpServer;
        RouplexTcpBinder tcpBinder = request.isUseSharedBinder() ? request.isUseNiossl()
                ? niosslTcpBinder : classicTcpBinder : null;

        try {
            rouplexTcpServer = RouplexTcpServer.newBuilder()
                    .withLocalAddress(request.getHostname(), request.getPort())
                    .withRouplexTcpBinder(tcpBinder)
                    .withSecure(request.isSsl(), null)
                    .withBacklog(request.getBacklog())
                    .withAttachment(request)
                    .build();

            if (tcpBinder == null) {
                rouplexTcpServer.getRouplexTcpBinder().setRouplexTcpClientConnectedListener(clientConnectedListener);
                rouplexTcpServer.getRouplexTcpBinder().setRouplexTcpClientClosedListener(clientClosedListener);
            }

            InetSocketAddress isa = (InetSocketAddress) rouplexTcpServer.getLocalAddress();
            logger.info(String.format("Created EchoServer at %s:%s",
                    isa.getAddress().getHostAddress().replace('.', '-'), isa.getPort()));

            InetSocketAddress inetSocketAddress = (InetSocketAddress) rouplexTcpServer.getLocalAddress();
            String hostPort = String.format("%s:%s", inetSocketAddress.getHostName().replace('.', '-'), inetSocketAddress.getPort());
            addCloseable(hostPort, rouplexTcpServer);

            StartTcpServerResponse response = new StartTcpServerResponse();
            response.setHostaddress(inetSocketAddress.getAddress().getHostAddress());
            response.setHostname(inetSocketAddress.getHostName());
            response.setPort(inetSocketAddress.getPort());
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
        InetSocketAddress inetSocketAddress = new InetSocketAddress(request.getHostname(), request.getPort());
        if (inetSocketAddress.isUnresolved()) {
            throw new IllegalArgumentException(String.format("Unresolved address %s", inetSocketAddress));
        }

        logger.info(String.format("Creating %s EchoClients for server at %s:%s",
                request.clientCount, request.getHostname(), request.getPort()));

        RouplexTcpBinder tcpBinder;
        if (request.isUseSharedBinder()) {
            tcpBinder = request.isUseNiossl() ? niosslTcpBinder : classicTcpBinder;
        } else {
            // differently from server, we create a tcpBinder for all the client batch. We expect a tcpBinder to handle
            // hundreds of thousands of clients (with one tcp connection each)
            tcpBinder = addCloseable(request.isUseNiossl() ? new RouplexTcpBinder() : new RouplexTcpBinder(Selector.open()));
            tcpBinder.setRouplexTcpClientConnectedListener(clientConnectedListener);
            tcpBinder.setRouplexTcpClientClosedListener(clientClosedListener);
        }

        for (int cc = 0; cc < request.clientCount; cc++) {
            long startClientMillis = request.minDelayMillisBeforeCreatingClient + random.nextInt(
                    request.maxDelayMillisBeforeCreatingClient - request.minDelayMillisBeforeCreatingClient);
            logger.info(String.format("Scheduled creation of EchoRequester in %s milliseconds.", startClientMillis));

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    EchoReporter echoReporter = new EchoReporter(request, benchmarkerMetrics, EchoRequester.class);
                    echoReporter.connecting();

                    try {
                        RouplexTcpClient.newBuilder()
                                .withRouplexTcpBinder(tcpBinder)
                                .withRemoteAddress(request.getHostname(), request.getPort())
                                .withSecure(request.isSsl(), null)
                                .withAttachment(request)
                                .buildAsync();
                    } catch (Exception e) {
                        echoReporter.unconnected();
                    }
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