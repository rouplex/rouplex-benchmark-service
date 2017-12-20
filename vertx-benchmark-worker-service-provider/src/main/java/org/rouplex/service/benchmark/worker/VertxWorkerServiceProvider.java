package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.net.*;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.commons.utils.ValidationUtils;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpServer;

import javax.net.ssl.SSLContext;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
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
public class VertxWorkerServiceProvider implements WorkerService, Closeable {
    public enum ConfigurationKey {
        JmxDomainName
    }

    private static final Logger logger = Logger.getLogger(VertxWorkerServiceProvider.class.getSimpleName());

    private static VertxWorkerServiceProvider workerService;
    public static VertxWorkerServiceProvider get() throws Exception {
        synchronized (VertxWorkerServiceProvider.class) {
            if (workerService == null) {
                ConfigurationManager configurationManager = new ConfigurationManager();

                configurationManager.putConfigurationEntry(
                    ConfigurationKey.JmxDomainName, "rouplex-benchmark");

                workerService = new VertxWorkerServiceProvider(configurationManager.getConfiguration());
            }

            return workerService;
        }
    }

    final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final AtomicInteger incrementalId = new AtomicInteger();
    final Random random = new Random();
    Set<Closeable> closeables = new HashSet<Closeable>();

    final Vertx sharedVertx;
    Map<String, NetServer> netServers = new HashMap<String, NetServer>();


    final Handler<AsyncResult<NetClient>> netClientListener = new Handler<AsyncResult<NetClient>>() {

        @Override
        public void handle(AsyncResult<NetClient> event) {
            if (event.succeeded()) {

            }
        }

        @Override
        public void onConnected(RouplexTcpClient rouplexTcpClient) {
            try {
                if (rouplexTcpClient.getRouplexTcpServer() == null) {
                    ((EchoRequester) rouplexTcpClient.getAttachment()).startSendingThenClose(rouplexTcpClient);
                } else {
                    new EchoResponder((CreateTcpServerRequest) rouplexTcpClient.getRouplexTcpServer().getAttachment(),
                            VertxWorkerServiceProvider.this, rouplexTcpClient);
                }
            } catch (Exception e) {
                //logExceptionTraceTemp(e);
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
                //logExceptionTraceTemp(e);
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
                //logExceptionTraceTemp(e);
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onDisconnected")).mark();
                benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onDisconnected.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onDisconnected(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    };

    VertxWorkerServiceProvider(Configuration configuration) throws Exception {
        sharedVertx = Vertx.vertx(new VertxOptions().setClustered(false));

        addCloseable(JmxReporter.forRegistry(benchmarkerMetrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .inDomain(configuration.get(ConfigurationKey.JmxDomainName))
                .build()
        ).start();

        logger.info("Created VertxWorkerServiceProvider");
    }

    @Override
    public CreateTcpServerResponse createTcpServer(CreateTcpServerRequest request) throws Exception {
        ValidationUtils.checkNonNullArg(request.getProvider(), "Provider");
        String secure = request.isSsl() ? "secure " : "";

        try {
            logger.info(String.format("Creating %sEchoServer using provider %s at %s:%s",
                secure, request.getProvider(), request.getHostname(), request.getPort()));

            Vertx vertx = request.isUseSharedBinder()
                    ? sharedVertx : Vertx.vertx(new VertxOptions().setClustered(false));

            NetServerOptions options = new NetServerOptions()
                .setSendBufferSize(request.getSocketSendBufferSize())
                .setReceiveBufferSize(request.getSocketReceiveBufferSize())
                .setAcceptBacklog(request.getBacklog())
                .setSsl(request.isSsl());

            NetServer netServer = vertx.createNetServer(options).listen(request.getPort(), request.getHostname());

//            InetSocketAddress isa = (InetSocketAddress) request.getLocalAddress();
//            logger.info(String.format("Created %sEchoServer using provider %s at %s:%s",
//                secure, request.getProvider(), isa.getAddress().getHostAddress().replace('.', '-'), isa.getPort()));

            String serverId = String.format("%s:%s", request.getHostname().replace('.', '-'), netServer.actualPort());
            netServers.put(serverId, netServer);

            CreateTcpServerResponse response = new CreateTcpServerResponse();
            response.setServerId(serverId);
            response.setHostaddress(request.getHostname()); // AAA
            response.setHostname(request.getHostname());
            response.setPort(netServer.actualPort());
            return response;
        } catch (Exception e) {
            logger.warning(String.format("Failed creating %sEchoServer using provider %s at %s:%s. Cause: %s: %s",
                secure, request.getProvider(), request.getHostname(), request.getPort(), e.getClass().getSimpleName(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public void destroyTcpServer(String serverId) throws Exception {
        NetServer netServer = netServers.remove(serverId);

        if (netServer == null) {
            throw new IOException("Could not find server [%s] " + serverId);
        }

        netServer.close();
    }

    @Override
    public PollTcpEndPointStateResponse pollTcpServerState(String serverId) throws Exception {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public CreateTcpClientBatchResponse createTcpClientBatch(final CreateTcpClientBatchRequest request) throws Exception {
        ValidationUtils.checkNonNullArg(request.getProvider(), "Provider");

        ValidationUtils.checkNonNegativeArg(request.getClientCount(), "ClientCount");
        ValidationUtils.checkNonNegativeArg(request.getMinClientLifeMillis(), "MinClientLifeMillis");
        ValidationUtils.checkNonNegativeArg(request.getMinDelayMillisBeforeCreatingClient(), "MinDelayMillisBeforeCreatingClient");
        ValidationUtils.checkNonNegativeArg(request.getMinDelayMillisBetweenSends(), "MinDelayMillisBetweenSends");
        ValidationUtils.checkNonNegativeArg(request.getMinPayloadSize(), "MinPayloadSize");

        ValidationUtils.checkPositiveArgDiff(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis(),
                "MinClientLifeMillis", "MaxClientLifeMillis");
        ValidationUtils.checkPositiveArgDiff(request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient(),
                "MinDelayMillisBeforeCreatingClient", "MaxDelayMillisBeforeCreatingClient");
        ValidationUtils.checkPositiveArgDiff(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends(),
                "MinDelayMillisBetweenSends", "MaxDelayMillisBetweenSends");
        ValidationUtils.checkPositiveArgDiff(request.getMaxPayloadSize() - request.getMinPayloadSize(),
                "MinPayloadSize", "MaxPayloadSize");

        InetSocketAddress inetSocketAddress = new InetSocketAddress(request.getHostname(), request.getPort());
        if (inetSocketAddress.isUnresolved()) {
            throw new IllegalArgumentException(String.format("Unresolved address %s", inetSocketAddress));
        }

        String secure = request.isSsl() ? "secure " : "";
        logger.info(String.format("Creating %s %sEchoClients using provider %s for server at %s:%s",
                request.getClientCount(), secure, request.getProvider(), request.getHostname(), request.getPort()));

        final Vertx vertx = request.isUseSharedBinder()
            ? sharedVertx : Vertx.vertx(new VertxOptions().setClustered(false));

        for (int cc = 0; cc < request.getClientCount(); cc++) {
            long startClientMillis = request.getMinDelayMillisBeforeCreatingClient() + random.nextInt(
                    request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient());
            logger.info(String.format("Scheduled creation of EchoRequester in %s milliseconds.", startClientMillis));

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    new EchoRequester(VertxWorkerServiceProvider.this, request, vertx);
                }
            }, startClientMillis, TimeUnit.MILLISECONDS);
        }

        CreateTcpClientBatchResponse response = new CreateTcpClientBatchResponse();
        response.setClientClusterId(UUID.randomUUID().toString());
        return response;
    }

    @Override
    public GetMetricsResponse getMetricsResponse() throws Exception {
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