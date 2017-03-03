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
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class BenchmarkServiceProvider implements BenchmarkService, Closeable {
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

    final NotificationListener<RouplexTcpClient> clientAddedListener = new NotificationListener<RouplexTcpClient>() {
        @Override
        public void onEvent(RouplexTcpClient rouplexTcpClient) {
            if (rouplexTcpClient.getRouplexTcpServer() == null) {
                ((EchoRequester) rouplexTcpClient.getAttachment()).addedClients.mark();
            } else {
                try {
                    (new EchoResponder(rouplexTcpClient)).addedClients.mark();
                } catch (IOException ioe) {
                }
            }
        }
    };

    final NotificationListener<RouplexTcpClient> clientRemovedListener = new NotificationListener<RouplexTcpClient>() {
        @Override
        public void onEvent(RouplexTcpClient rouplexTcpClient) {
            if (rouplexTcpClient.getRouplexTcpServer() == null) {
                ((EchoRequester) rouplexTcpClient.getAttachment()).removedClients.mark();
            } else {
                ((EchoResponder) rouplexTcpClient.getAttachment()).removedClients.mark();
            }
        }
    };

    BenchmarkServiceProvider() throws IOException {
        classicTcpBinder = addCloseable(new RouplexTcpBinder(Selector.open(), null));
        classicTcpBinder.setRouplexTcpClientAddedListener(clientAddedListener);
        classicTcpBinder.setRouplexTcpClientRemovedListener(clientRemovedListener);

        niosslTcpBinder = addCloseable(new RouplexTcpBinder(SSLSelector.open(), null));
        niosslTcpBinder.setRouplexTcpClientAddedListener(clientAddedListener);
        niosslTcpBinder.setRouplexTcpClientRemovedListener(clientRemovedListener);

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
        RouplexTcpBinder tcpBinder;
        if (request.isUseSharedBinder()) {
            tcpBinder = request.isUseNiossl() ? niosslTcpBinder : classicTcpBinder;
        } else {
            // differently from server, we create a tcpBinder for all the client batch. We expect a tcpBinder to handle
            // hundreds of thousands of clients (with one tcp connection each)
            tcpBinder = addCloseable(new RouplexTcpBinder(request.isUseNiossl() ? SSLSelector.open() : Selector.open()));
            tcpBinder.setRouplexTcpClientAddedListener(clientAddedListener);
            tcpBinder.setRouplexTcpClientRemovedListener(clientRemovedListener);
        }

        for (int cc = 0; cc < request.clientCount; cc++) {
            long startClientMillis = request.minDelayMillisBeforeCreatingClient +
                    random.nextInt(request.maxDelayMillisBeforeCreatingClient - request.minDelayMillisBeforeCreatingClient);

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        new EchoRequester(request, tcpBinder);
                    } catch (IOException ioe) {
                        // already metered
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
            rouplexTcpServer = RouplexTcpServer.newBuilder()
                    .withLocalAddress(request.getHostname(), request.getPort())
                    .withRouplexTcpBinder(tcpBinder)
                    .withSecure(request.isSsl(), null)
                    .withAttachment(request)
                    .build();

            if (tcpBinder == null) {
                rouplexTcpServer.getRouplexTcpBinder().setRouplexTcpClientAddedListener(clientAddedListener);
                rouplexTcpServer.getRouplexTcpBinder().setRouplexTcpClientRemovedListener(clientRemovedListener);
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

        final Meter addedClients;
        final Meter removedClients;
        final Meter receivedBytes;
        final Meter sentBytes;
        final Histogram inSizes;
        final Histogram outSizes;
        final Meter sendFailures;
        final Timer pauseTime;
        Timer.Context pauseTimer;

        ByteBuffer sendBuffer;

        EchoResponder(RouplexTcpClient rouplexTcpClient) throws IOException {
            rouplexTcpClient.setAttachment(this);
            StartTcpServerRequest request = (StartTcpServerRequest) rouplexTcpClient.getRouplexTcpServer().getAttachment();
            InetSocketAddress inetSocketAddress = (InetSocketAddress) rouplexTcpClient.getRouplexTcpServer().getLocalAddress();

            String responderPrefix = String.format("%s.%s.%s.%s:%s",
                    request.isUseNiossl() ? "N" : "C",
                    request.isSsl() ? "S" : "P",
                    EchoResponder.class.getSimpleName(),
                    inetSocketAddress.getHostName().replace('.', '-'),
                    inetSocketAddress.getPort());

            addedClients = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "connected"));
            removedClients = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "disconnected"));
            receivedBytes = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "received"));
            sentBytes = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "sent"));
            inSizes = benchmarkerMetrics.histogram(MetricRegistry.name(responderPrefix, "inSizes"));
            outSizes = benchmarkerMetrics.histogram(MetricRegistry.name(responderPrefix, "outSizes"));
            sendFailures = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "sendFailures"));
            pauseTime = benchmarkerMetrics.timer(MetricRegistry.name(responderPrefix, "pauseTime"));

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
                        sendBuffer = null;
                    } else {
                        inSizes.update(payload.length);
                        receivedBytes.mark(payload.length);
                        sendBuffer = ByteBuffer.wrap(payload);
                    }

                    return send();
                }
            });
        }

        private boolean send() {
            try {
                if (sendBuffer == null) {
                    return sendChannel.send(null);
                }

                int position = sendBuffer.position();
                boolean sent = sendChannel.send(sendBuffer); // echo
                int sentSize = sendBuffer.position() - position;
                sentBytes.mark(sentSize);
                outSizes.update(sentSize);

                if (!sent) {
                    pauseTimer = pauseTime.time();
                }
                return sent;
            } catch (IOException ioe) {
                sendFailures.mark();
                return false;
            }
        }
    }

    public class EchoRequester implements Closeable {
        final RouplexTcpClient rouplexTcpClient;
        final StartTcpClientsRequest request;
        final int clientId;
        final SendChannel<ByteBuffer> sendChannel;
        final List<byte[]> receivedBuffers = new ArrayList<>();
        final Throttle receiveThrottle;

        final List<ByteBuffer> sendBuffers = new ArrayList<>();
        final int maxSendBufferSize;
        int currentSendBufferSize;
        long sent;
        long received;
        long closeTimestamp;

        final Meter createdRequesters;
        final Meter uncreatedRequesters;
        final Meter addedClients;
        final Meter removedClients;
        final Meter receivedBytes;
        final Meter sentBytes;
        final Histogram sendBufferFilled;
        final Meter discardedSendBytes;
        final Meter sizeCheckFailures;
        final Meter sendFailures;
        final Timer pauseTime;
        Timer.Context pauseTimer;

        EchoRequester(StartTcpClientsRequest request, RouplexTcpBinder tcpBinder) throws IOException {
            String responderPrefix = String.format("%s.%s.%s",
                    request.isUseNiossl() ? "N" : "C",
                    request.isSsl() ? "S" : "P",
                    EchoRequester.class.getSimpleName());

            createdRequesters = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "created"));
            uncreatedRequesters = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "uncreated"));
            addedClients = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "connected"));
            removedClients = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "disconnected"));
            sentBytes = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "sent"));
            sendBufferFilled = benchmarkerMetrics.histogram(MetricRegistry.name(responderPrefix, "sendBufferFilled"));
            discardedSendBytes = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "discardedSendBytes"));
            receivedBytes = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "received"));
            sizeCheckFailures = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "sizeCheckFailures"));
            sendFailures = benchmarkerMetrics.meter(MetricRegistry.name(responderPrefix, "sendFailures"));
            pauseTime = benchmarkerMetrics.timer(MetricRegistry.name(responderPrefix, "pauseTime"));

            try {
                this.request = request;
                this.rouplexTcpClient = RouplexTcpClient.newBuilder()
                        .withRouplexTcpBinder(tcpBinder)
                        .withRemoteAddress(request.getHostname(), request.getPort())
                        .withSecure(request.isSsl(), null)
                        .withAttachment(this)
                        .build();

                this.clientId = incrementalId.incrementAndGet();

                this.maxSendBufferSize = request.maxPayloadSize * 10;

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
                        receivedBuffers.add(payload);
                        synchronized (sendChannel) {
                            if (payload != null) {
                                received += payload.length;
                                receivedBytes.mark(payload.length);
                            } else {
                                if (received != sent) {
                                    sizeCheckFailures.mark();
                                }
                            }
                        }

                        return true;
                    }
                });

                closeTimestamp = System.currentTimeMillis() + request.minClientLifeMillis +
                        random.nextInt(request.maxClientLifeMillis - request.minClientLifeMillis);

                createdRequesters.mark();
                keepSendingThenClose();
            } catch (IOException ioe) {
                uncreatedRequesters.mark();
                throw ioe;
            }
        }

        void keepSendingThenClose() {
            if (System.currentTimeMillis() >= closeTimestamp) {
                synchronized (sendBuffers) {
                    sendBuffers.add(null); // send EOS
                }
                send();
            } else {
                int payloadSize = request.minPayloadSize + random.nextInt(request.maxPayloadSize - request.minPayloadSize);
                int remaining = maxSendBufferSize - currentSendBufferSize;
                int discarded = payloadSize - remaining;
                if (discarded > 0) {
                    discardedSendBytes.mark(discarded);
                    payloadSize = remaining;
                }

                if (payloadSize > 0) {
                    synchronized (sendBuffers) {
                        ByteBuffer bb = ByteBuffer.allocate(payloadSize);
                        bb.array()[0] = (byte) clientId;
                        sendBuffers.add(bb);
                        currentSendBufferSize += payloadSize;
                    }

                    sendBufferFilled.update(currentSendBufferSize);
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

        private boolean send() {
            try {
                while (true) {
                    ByteBuffer sendBuffer;
                    boolean result;
                    int payloadSize;

                    synchronized (sendBuffers) {
                        if (sendBuffers.isEmpty()) {
                            break;
                        }
                        sendBuffer = sendBuffers.get(0);
                    }

                    if (sendBuffer == null) {
                        result = sendChannel.send(null);
                        payloadSize = 0;
                    } else {
                        int position = sendBuffer.position();
                        result = sendChannel.send(sendBuffer);
                        payloadSize = sendBuffer.position() - position;
                        sentBytes.mark(payloadSize);
                    }

                    synchronized (sendBuffers) {
                        sent += payloadSize;
                        currentSendBufferSize -= payloadSize;
                        if (result) {
                            sendBuffers.remove(0);
                        } else {
                            pauseTimer = pauseTime.time();
                            break;
                        }
                    }
                }

                return true;
            } catch (IOException ioe) {
                sendFailures.mark();
                return false;
            }
        }

        @Override
        public void close() throws IOException {
            rouplexTcpClient.close();
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
}