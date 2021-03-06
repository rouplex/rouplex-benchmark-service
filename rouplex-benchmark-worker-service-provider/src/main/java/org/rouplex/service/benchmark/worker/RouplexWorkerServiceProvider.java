package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.commons.utils.ValidationUtils;
import org.rouplex.platform.tcp.RouplexTcpBroker;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpClientListener;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmark.orchestrator.Provider;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class RouplexWorkerServiceProvider implements WorkerService, Closeable {
    public enum ConfigurationKey {
        JmxDomainName
    }

    private static final Logger logger = Logger.getLogger(RouplexWorkerServiceProvider.class.getSimpleName());

    private static RouplexWorkerServiceProvider workerService;
    public static RouplexWorkerServiceProvider get() throws Exception {
        synchronized (RouplexWorkerServiceProvider.class) {
            if (workerService == null) {
                ConfigurationManager configurationManager = new ConfigurationManager();

                configurationManager.putConfigurationEntry(
                    ConfigurationKey.JmxDomainName, "rouplex-benchmark");

                workerService = new RouplexWorkerServiceProvider(configurationManager.getConfiguration());
            }

            return workerService;
        }
    }

    final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final Random random = new Random();
    Set<Closeable> closeables = new HashSet<Closeable>();

    Map<String, Object> jobs = new HashMap<String, Object>(); // add implementation later
    Map<String, RouplexTcpServer> tcpServers = new HashMap<String, RouplexTcpServer>();

    void logExceptionTraceTemp(Exception e) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        e.printStackTrace(ps);
        ps.flush();
        logger.warning(new String(os.toByteArray(), StandardCharsets.UTF_8));
    }

    RouplexWorkerServiceProvider(Configuration configuration) throws Exception {
        String jmxDomainName;
        try {
            jmxDomainName = configuration.get(ConfigurationKey.JmxDomainName);
        } catch (NoSuchElementException e) {
            jmxDomainName = null;
        }

        addCloseable(JmxReporter.forRegistry(benchmarkerMetrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .inDomain(jmxDomainName)
                .build()
        ).start();

        logger.info("Created RouplexWorkerServiceProvider");
    }

    @Override
    public CreateTcpServerResponse createTcpServer(final CreateTcpServerRequest request) throws Exception {
        ValidationUtils.checkNonNullArg(request.getProvider(), "Provider");
        String secure = request.isSsl() ? "secure " : "";

        try {
            logger.info(String.format("Creating %sEchoServer using provider %s at %s:%s",
                secure, request.getProvider(), request.getHostname(), request.getPort()));

            RouplexTcpBroker rouplexTcpBroker = createRouplexTcpBroker(request.getProvider());
            ServerSocketChannel serverSocketChannel;

            if (request.isSsl()) {
                switch (request.getProvider()) {
                    case ROUPLEX_NIOSSL:
                        serverSocketChannel = org.rouplex.nio.channels.SSLServerSocketChannel.open(SSLContext.getDefault());
                        break;
                    case THIRD_PARTY_SSL:
                        // serverSocketChannel = thirdPartySsl.SSLServerSocketChannel.open(SSLContext.getDefault());
                        // break;
                    case CLASSIC_NIO:
                    default:
                        throw new Exception("This provider cannot provide ssl communication");
                }
            } else {
                serverSocketChannel = ServerSocketChannel.open();
            }

            RouplexTcpClientListener rouplexTcpClientListener = new RouplexTcpClientListener() {
                @Override
                public void onConnected(RouplexTcpClient rouplexTcpClient) {
                    try {
                        new EchoResponder(request, RouplexWorkerServiceProvider.this, rouplexTcpClient);
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
                    benchmarkerMetrics.meter(MetricRegistry.name("connection.failed")).mark();
                    benchmarkerMetrics.meter(MetricRegistry.name("connection.failed.EEE",
                        reason.getClass().getSimpleName(), reason.getMessage())).mark();

                    logger.warning(String.format("Failed connecting EchoRequester. Cause: %s: %s",
                        reason.getClass().getSimpleName(), reason.getMessage()));
                }

                @Override
                public void onDisconnected(RouplexTcpClient rouplexTcpClient,
                                           Exception optionalReason, boolean drainedChannels) {
                    EchoReporter echoReporter = ((EchoResponder) rouplexTcpClient.getAttachment()).echoReporter;
                    if (echoReporter == null) {
                        logger.warning("Rouplex internal error: onDisconnected was fired before onConnected");
                    } else {
                        echoReporter.liveConnections.mark(-1);
                        (drainedChannels ? echoReporter.disconnectedOk : echoReporter.disconnectedKo).mark();

                        logger.info(String.format("Disconnected %s. Drained: %s",
                            rouplexTcpClient.getAttachment().getClass().getSimpleName(), drainedChannels));
                    }
                }
            };

            RouplexTcpServer rouplexTcpServer = rouplexTcpBroker.newRouplexTcpServerBuilder()
                    .withServerSocketChannel(serverSocketChannel)
                    .withLocalAddress(request.getHostname(), request.getPort())
                    .withSecure(request.isSsl(), null) // value ignored in current context since channel is provided
                    .withRouplexTcpClientListener(rouplexTcpClientListener)
                    .withBacklog(request.getBacklog())
                    .withSendBufferSize(request.getSocketSendBufferSize())
                    .withReceiveBufferSize(request.getSocketReceiveBufferSize())
                    .withAttachment(request)
                    .build();

            InetSocketAddress isa = (InetSocketAddress) rouplexTcpServer.getLocalAddress();
            logger.info(String.format("Created %sEchoServer using provider %s at %s:%s",
                secure, request.getProvider(), isa.getAddress().getHostAddress().replace('.', '-'), isa.getPort()));

            String serverId = String.format("%s:%s", isa.getHostName().replace('.', '-'), isa.getPort());
            tcpServers.put(serverId, rouplexTcpServer);

            CreateTcpServerResponse response = new CreateTcpServerResponse();
            response.setServerId(serverId);
            response.setHostaddress(isa.getAddress().getHostAddress());
            response.setHostname(isa.getHostName());
            response.setPort(isa.getPort());
            return response;
        } catch (Exception e) {
            logger.warning(String.format("Failed creating %sEchoServer using provider %s at %s:%s. Cause: %s: %s",
                secure, request.getProvider(), request.getHostname(), request.getPort(), e.getClass().getSimpleName(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public void destroyTcpServer(String serverId) throws Exception {
        RouplexTcpServer tcpServer = tcpServers.remove(serverId);

        if (tcpServer == null) {
            throw new IOException("Could not find server [%s] " + serverId);
        }

        tcpServer.close();
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

        final RouplexTcpBroker tcpBroker = createRouplexTcpBroker(request.getProvider());

        for (int cc = 0; cc < request.getClientCount(); cc++) {
            long startClientMillis = request.getMinDelayMillisBeforeCreatingClient() + random.nextInt(
                    request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient());
            logger.info(String.format("Scheduled creation of EchoRequester in %s milliseconds.", startClientMillis));

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    new EchoRequester(request, benchmarkerMetrics, scheduledExecutor, tcpBroker);
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

    private RouplexTcpBroker createRouplexTcpBroker(Provider provider) throws Exception {
        SelectorProvider selectorProvider;

        switch (provider) {
            case CLASSIC_NIO:
                selectorProvider = java.nio.channels.spi.SelectorProvider.provider();
                break;
            case ROUPLEX_NIOSSL:
                selectorProvider = org.rouplex.nio.channels.spi.SSLSelectorProvider.provider();
                break;
            case THIRD_PARTY_SSL:
                // return thirdPartySsl.SSLSelector.open(SSLContext.getDefault());
            default:
                throw new Exception("Provider not found");
        }

        return addCloseable(new RouplexTcpBroker(selectorProvider));
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
            if (closeables == null) {
                return;
            }

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