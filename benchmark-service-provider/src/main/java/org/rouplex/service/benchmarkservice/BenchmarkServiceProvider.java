package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.rouplex.nio.channels.spi.SSLSelector;
import org.rouplex.platform.rr.NotificationListener;
import org.rouplex.platform.rr.ReceiveChannel;
import org.rouplex.platform.rr.SendChannel;
import org.rouplex.platform.rr.Throttle;
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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
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
    Map<String, Closeable> closeables = new HashMap<>();
    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    final AtomicInteger incrementalId = new AtomicInteger();
    final Random random = new Random();

    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final JmxReporter jmxReporter = JmxReporter.forRegistry(benchmarkerMetrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    final NotificationListener<RouplexTcpClient> clientConnectedListener = new NotificationListener<RouplexTcpClient>() {
        @Override
        public void onEvent(RouplexTcpClient rouplexTcpClient) {
            if (rouplexTcpClient.getRouplexTcpServer() == null) {
                ((EchoRequester) rouplexTcpClient.getAttachment()).echoReporter.connected();
            } else {
                try {
                    (new EchoResponder(rouplexTcpClient)).echoReporter.connected();
                } catch (IOException ioe) {
                    benchmarkerMetrics.meter(MetricRegistry.name("EEE", ioe.getMessage()));
                }
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

        niosslTcpBinder = addCloseable(new RouplexTcpBinder(SSLSelector.open(), null));
        niosslTcpBinder.setRouplexTcpClientConnectedListener(clientConnectedListener);
        niosslTcpBinder.setRouplexTcpClientClosedListener(clientClosedListener);

        addCloseable(jmxReporter).start();
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        RouplexTcpBinder tcpBinder = request.isUseSharedBinder() ? request.isUseNiossl()
                ? niosslTcpBinder : classicTcpBinder : null;

        EchoServer echoServer = new EchoServer(request, tcpBinder);
        InetSocketAddress inetSocketAddress = (InetSocketAddress) echoServer.rouplexTcpServer.getLocalAddress();

        String hostPort = String.format("%s:%s", inetSocketAddress.getHostName().replace('.', '-'), inetSocketAddress.getPort());
        addCloseable(hostPort, echoServer);

        StartTcpServerResponse response = new StartTcpServerResponse();
        response.setHostaddress(inetSocketAddress.getAddress().getHostAddress());
        response.setHostname(inetSocketAddress.getHostName());
        response.setPort(inetSocketAddress.getPort());
        return response;
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

        RouplexTcpBinder tcpBinder;
        if (request.isUseSharedBinder()) {
            tcpBinder = request.isUseNiossl() ? niosslTcpBinder : classicTcpBinder;
        } else {
            // differently from server, we create a tcpBinder for all the client batch. We expect a tcpBinder to handle
            // hundreds of thousands of clients (with one tcp connection each)
            tcpBinder = addCloseable(new RouplexTcpBinder(request.isUseNiossl() ? SSLSelector.open() : Selector.open()));
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
                    try {
                        new EchoRequester(request, tcpBinder);
                    } catch (Exception e) {
                        // already dealt with
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

    class EchoServer implements Closeable {
        final RouplexTcpServer rouplexTcpServer;

        EchoServer(StartTcpServerRequest request, RouplexTcpBinder tcpBinder) throws Exception {
            logger.info(String.format("Creating EchoServer at %s:%s", request.getHostname(), request.getPort()));

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

                logger.info(String.format("Created EchoServer at %s",
                        getFormattedAddress(rouplexTcpServer.getLocalAddress())));
            } catch (Exception e) {
                logger.warning(String.format("Failed creating EchoServer at %s:%s. Cause: %s. %s",
                        request.getHostname(), request.getPort(), e.getClass(), e.getMessage()));
                throw e;
            }
        }

        @Override
        public void close() throws IOException {
            rouplexTcpServer.close();
        }
    }

    public class EchoResponder {
        final SendChannel<ByteBuffer> sendChannel;
        final Throttle receiveThrottle;

        final EchoReporter echoReporter;
        Timer.Context pauseTimer;

        ByteBuffer sendBuffer;
        Integer clientId;

        EchoResponder(final RouplexTcpClient rouplexTcpClient) throws IOException {
            rouplexTcpClient.setAttachment(this);

            echoReporter = new EchoReporter(rouplexTcpClient);
            echoReporter.created();

            sendChannel = rouplexTcpClient.hookSendChannel(new Throttle() {
                @Override
                public void resume() {
                    pauseTimer.stop(); // this should be reporting the time paused
                    send();
                }
            });

            receiveThrottle = rouplexTcpClient.hookReceiveChannel(new ReceiveChannel<byte[]>() {
                @Override
                public boolean receive(byte[] payload) {
                    if (payload == null) {
                        echoReporter.receivedDisconnect.mark();
                        return true;
                    }

                    if (payload.length == 0) {
                        echoReporter.receivedEos.mark();
                    } else {
                        if (clientId == null) {
                            clientId = deserializeClientId(payload);
                            echoReporter.setClientId(clientId);
                        }

                        echoReporter.receivedSizes.update(payload.length);
                        echoReporter.receivedBytes.mark(payload.length);
                    }

                    sendBuffer = ByteBuffer.wrap(payload);
                    return send();
                }
            }, true);
        }

        private boolean send() {
            try {
                int position = sendBuffer.position();
                sendChannel.send(sendBuffer); // echo as many as possible

                if (sendBuffer.hasRemaining()) {
                    pauseTimer = echoReporter.sendPauseTime.time();
                } else {
                    if (sendBuffer.capacity() == 0) {
                        echoReporter.sentEos.mark();
                    } else {
                        int sentSize = sendBuffer.position() - position;
                        echoReporter.sentBytes.mark(sentSize);
                        echoReporter.sentSizes.update(sentSize);
                    }
                }

                return !sendBuffer.hasRemaining();
            } catch (IOException ioe) {
                echoReporter.sendFailures.mark();
                return false;
            }
        }
    }

    public class EchoRequester {
        final RouplexTcpClient rouplexTcpClient;
        final StartTcpClientsRequest request;
        final int clientId;
        final SendChannel<ByteBuffer> sendChannel;
        final Throttle receiveThrottle;

        final List<ByteBuffer> sendBuffers = new ArrayList<>();
        final int maxSendBufferSize;
        int currentSendBufferSize;
        long closeTimestamp;

        final EchoReporter echoReporter;
        Timer.Context pauseTimer;

        EchoRequester(StartTcpClientsRequest request, RouplexTcpBinder tcpBinder) throws IOException {
            echoReporter = new EchoReporter(request);
            echoReporter.creating();

            try {
                this.request = request;
                this.rouplexTcpClient = RouplexTcpClient.newBuilder()
                        .withRouplexTcpBinder(tcpBinder)
                        .withRemoteAddress(request.getHostname(), request.getPort())
                        .withSecure(request.isSsl(), null)
                        .withAttachment(this)
                        .build();

                echoReporter.setClientAddress(rouplexTcpClient.getLocalAddress());
                this.clientId = incrementalId.incrementAndGet();
                echoReporter.setClientId(clientId);

                this.maxSendBufferSize = request.maxPayloadSize * 1;
                closeTimestamp = System.currentTimeMillis() + request.minClientLifeMillis +
                        random.nextInt(request.maxClientLifeMillis - request.minClientLifeMillis);

                sendChannel = rouplexTcpClient.hookSendChannel(new Throttle() {
                    @Override
                    public void resume() {
                        pauseTimer.stop(); // this should be reporting the time paused
                        send();
                    }
                });

                receiveThrottle = rouplexTcpClient.hookReceiveChannel(new ReceiveChannel<byte[]>() {
                    @Override
                    public boolean receive(byte[] payload) {
                        if (payload == null) {
                            echoReporter.receivedDisconnect.mark();
                        } else if (payload.length == 0) {
                            echoReporter.receivedEos.mark();
                        } else {
                            echoReporter.receivedBytes.mark(payload.length);
                            echoReporter.receivedSizes.update(payload.length);
                        }

                        return true;
                    }
                }, true);

                echoReporter.created();

                keepSendingThenClose();
            } catch (Exception e) {
                // IOException | RuntimeException (UnresolvedAddressException)
                echoReporter.uncreated();
                throw e;
            }
        }

        void keepSendingThenClose() {
            if (System.currentTimeMillis() >= closeTimestamp) {
                synchronized (sendBuffers) {
                    sendBuffers.add(ByteBuffer.allocate(0)); // send EOS
                }
                send();
            } else {
                int payloadSize = request.minPayloadSize + random.nextInt(request.maxPayloadSize - request.minPayloadSize);
                synchronized (sendBuffers) {
                    int remaining = maxSendBufferSize - currentSendBufferSize;
                    int discarded = payloadSize - remaining;
                    if (discarded > 0) {
                        echoReporter.discardedSendBytes.mark(discarded);
                        payloadSize = remaining;
                    }

                    if (payloadSize > 0) {
                        ByteBuffer bb = ByteBuffer.allocate(payloadSize);
                        serializeClientId(clientId, bb.array());
                        sendBuffers.add(bb);
                        currentSendBufferSize += payloadSize;
                        echoReporter.sendBufferFilled.update(currentSendBufferSize);
                    }
                }

                if (payloadSize > 0) {
                    send();
                }

                long delay = request.getMinDelayMillisBetweenSends() +
                        random.nextInt(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends());

                delay = Math.max(0, Math.min(delay, closeTimestamp - System.currentTimeMillis()));

                scheduledExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        keepSendingThenClose();
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
        }

        private void send() {
            try {
                while (true) {
                    synchronized (sendBuffers) {
                        if (sendBuffers.isEmpty()) {
                            break;
                        }
                        ByteBuffer sendBuffer = sendBuffers.get(0);

                        int position = sendBuffer.position();
                        sendChannel.send(sendBuffer);
                        int payloadSize = sendBuffer.position() - position;

                        currentSendBufferSize -= payloadSize;
                        if (sendBuffer.hasRemaining()) {
                            pauseTimer = echoReporter.sendPauseTime.time();
                            break;
                        } else {
                            if (sendBuffer.capacity() == 0) {
                                echoReporter.sentEos.mark();
                            } else {
                                echoReporter.sentSizes.update(payloadSize);
                                echoReporter.sentBytes.mark(payloadSize);
                            }
                            sendBuffers.remove(0);
                        }
                    }
                }
            } catch (IOException ioe) {
                echoReporter.sendFailures.mark();
            }
        }
    }

    class EchoReporter {
        final Request request;

        Meter creating;
        Meter created;
        Meter uncreated;
        Meter connected;
        Meter disconnected;

        Meter sentBytes;
        Meter sentEos;
        Meter sendFailures;
        Histogram sentSizes;
        Histogram sendBufferFilled;
        Meter discardedSendBytes;
        Timer sendPauseTime;

        Meter receivedBytes;
        Meter receivedEos;
        Meter receivedDisconnect;
        Histogram receivedSizes;

        String aggregatedId;
        String completeId;

        EchoReporter(Request request) throws IOException {
            this.request = request;
            MetricsAggregation ma = request.getMetricsAggregation();

            String remoteSa = request.getHostname().replace('.', '-');
            String aggregatedRemoteSa = ma.isAggregateServerAddresses() ? "A" : remoteSa;
            String aggregatedRemoteSp = ma.isAggregateServerPorts() ? "A" : "" + request.getPort();

            completeId = String.format("%s.%s.%s.%s:%s",
                    request.isUseNiossl() ? "N" : "C",
                    request.isSsl() ? "S" : "P",
                    EchoRequester.class.getSimpleName(),
                    remoteSa, request.getPort());

            aggregatedId = String.format("%s.%s.%s.%s:%s",
                    request.isUseNiossl() ? "N" : "C",
                    request.isSsl() ? "S" : "P",
                    EchoRequester.class.getSimpleName(),
                    aggregatedRemoteSa, aggregatedRemoteSp);

            init();
        }

        EchoReporter(RouplexTcpClient rouplexTcpClient) throws IOException {
            request = (Request) rouplexTcpClient.getRouplexTcpServer().getAttachment();
            MetricsAggregation ma = request.getMetricsAggregation();

            InetSocketAddress localIsa = (InetSocketAddress) rouplexTcpClient.getLocalAddress();
            String localSa = localIsa.getAddress().getHostAddress().replace('.', '-');
            String aggregatedLocalSa = ma.isAggregateServerAddresses() ? "A" : localSa;
            String aggregatedLocalSp = ma.isAggregateServerPorts() ? "A" : "" + localIsa.getPort();

            InetSocketAddress remoteIsa = (InetSocketAddress) rouplexTcpClient.getRemoteAddress();
            String remoteSa = remoteIsa.getAddress().getHostAddress().replace('.', '-');
            String aggregatedRemoteSa = ma.isAggregateServerAddresses() ? "A" : remoteSa;
            String aggregatedRemoteSp = ma.isAggregateServerPorts() ? "A" : "" + remoteIsa.getPort();

            completeId = String.format("%s.%s.%s.%s:%s::%s:%s",
                    request.isUseNiossl() ? "N" : "C",
                    request.isSsl() ? "S" : "P",
                    EchoResponder.class.getSimpleName(),
                    localSa, localIsa.getPort(),
                    remoteSa, remoteIsa.getPort());

            aggregatedId = String.format("%s.%s.%s.%s:%s::%s:%s",
                    request.isUseNiossl() ? "N" : "C",
                    request.isSsl() ? "S" : "P",
                    EchoResponder.class.getSimpleName(),
                    aggregatedLocalSa, aggregatedLocalSp,
                    aggregatedRemoteSa, aggregatedRemoteSp);

            init();
        }

        private void init() {
            creating = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "creating"));
            created = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "created"));
            uncreated = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "uncreated"));
            connected = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connected"));
            disconnected = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "disconnected"));
        }

        void setClientAddress(SocketAddress socketAddress) {
            MetricsAggregation ma = request.getMetricsAggregation();
            InetSocketAddress localIsa = (InetSocketAddress) socketAddress;
            String localSa = localIsa.getAddress().getHostAddress().replace('.', '-');
            String aggregatedLocalSa = ma.isAggregateServerAddresses() ? "A" : localSa;
            String aggregatedLocalSp = ma.isAggregateServerPorts() ? "A" : "" + localIsa.getPort();

            aggregatedId += "::" + aggregatedLocalSa + ":" + aggregatedLocalSp;
            completeId += "::" + localSa + ":" + localIsa.getPort();
        }

        void setClientId(int clientId) {
            String hexClientId = String.format("%04X", clientId);
            completeId += "." + hexClientId;
            aggregatedId += request.getMetricsAggregation().isAggregateClientCounters() ? ".A" : "." + hexClientId;

            sentBytes = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "sentBytes"));
            sentEos = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "sentEos"));
            sendFailures = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "sendFailures"));
            sentSizes = benchmarkerMetrics.histogram(MetricRegistry.name(aggregatedId, "sentSizes"));
            sendBufferFilled = benchmarkerMetrics.histogram(MetricRegistry.name(aggregatedId, "sendBufferFilled"));
            discardedSendBytes = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "discardedSendBytes"));
            sendPauseTime = benchmarkerMetrics.timer(MetricRegistry.name(aggregatedId, "sendPauseTime"));

            receivedBytes = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "receivedBytes"));
            receivedEos = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "receivedEos"));
            receivedDisconnect = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "receivedDisconnect"));
            receivedSizes = benchmarkerMetrics.histogram(MetricRegistry.name(aggregatedId, "receivedSizes"));
        }

        void creating() {
            creating.mark();
            logger.info(String.format("Creating %s", completeId));
        }

        void created() {
            created.mark();
            logger.info(String.format("Created %s", completeId));
        }

        void uncreated() {
            uncreated.mark();
            logger.info(String.format("Uncreated %s", completeId));
        }

        void connected() {
            connected.mark();
            logger.info(String.format("Connected %s", completeId));
        }

        void disconnected() {
            disconnected.mark();
            logger.info(String.format("Disconnected %s", completeId));
        }
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

    private static String getFormattedAddress(SocketAddress socketAddress) {
        InetSocketAddress isa = (InetSocketAddress) socketAddress;
        return String.format("%s:%s", isa.getAddress().getHostAddress().replace('.', '-'), isa.getPort());
    }

    private static void serializeClientId(int clientId, byte[] buffer) {
        for (int i = 3; i >= 0; i--) {
            buffer[i] = (byte) (clientId & 0xFF);
            clientId >>>= 8;
        }
    }

    private static int deserializeClientId(byte[] buffer) {
        int clientId = 0;
        for (int i = 0; i < 4; i++) {
            clientId <<= 8;
            clientId |= buffer[i];
        }

        return clientId;
    }
}