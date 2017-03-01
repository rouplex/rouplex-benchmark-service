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

    final RouplexTcpBinder classicRouplexTcpBinder;
    final RouplexTcpBinder niosslRouplexTcpBinder;
    final Map<String, RouplexTcpServer> rouplexTcpServers = new HashMap<String, RouplexTcpServer>();
    final Set<RouplexTcpClient> rouplexTcpClients = new HashSet<RouplexTcpClient>();
    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    final Random random = new Random();

    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final JmxReporter jmxReporter = JmxReporter.forRegistry(benchmarkerMetrics)
            .convertRatesTo(TimeUnit.SECONDS)
            .convertDurationsTo(TimeUnit.MILLISECONDS)
            .build();

    BenchmarkServiceProvider() throws IOException {
        classicRouplexTcpBinder = new RouplexTcpBinder(Selector.open(), null);
        niosslRouplexTcpBinder = new RouplexTcpBinder(SSLSelector.open(), null);
        jmxReporter.start();
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        RouplexTcpBinder tcpBinder = request.isUseSharedBinder() ? request.isUseNiossl()
                ? niosslRouplexTcpBinder : classicRouplexTcpBinder : null;

        RouplexTcpServer rouplexTcpServer = RouplexTcpServer.newBuilder()
                .withLocalAddress(request.getHostname(), request.getPort())
                .withRouplexTcpBinder(tcpBinder)
                .withSecure(request.isSsl(), null)
                .build();

        InetSocketAddress inetSocketAddress = (InetSocketAddress) rouplexTcpServer.getLocalAddress();
        final String hostPort = String.format("%s:%s", inetSocketAddress.getHostName().replace('.', '-'), inetSocketAddress.getPort());
        rouplexTcpServers.put(hostPort, rouplexTcpServer);

        String tag = String.format("%s.%s", request.isUseNiossl() ? "Niossl" : "Classic", request.isSsl() ? "Ssl" : "Plain");
        rouplexTcpServer.getRouplexTcpBinder().setTcpClientAddedListener(new NotificationListener<RouplexTcpClient>() {
            @Override
            public void onEvent(RouplexTcpClient rouplexTcpClient) {
                new EchoServer(rouplexTcpClient, hostPort, tag);
            }
        });

        StartTcpServerResponse response = new StartTcpServerResponse();
        response.setHostname(inetSocketAddress.getHostName());
        response.setPort(inetSocketAddress.getPort());
        return response;
    }

    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        String hostPort = String.format("%s:%s", request.getHostname().replace('.', '-'), request.getPort());
        RouplexTcpServer rouplexTcpServer = rouplexTcpServers.remove(hostPort);
        if (rouplexTcpServer == null) {
            throw new IOException("There is no RouplexTcpServer listening at " + hostPort);
        }

        rouplexTcpServer.close();
        return new StopTcpServerResponse();
    }

    @Override
    public StartTcpClientsResponse startTcpClients(final StartTcpClientsRequest request) throws Exception {
//        //request = new StartTcpClientsRequest(); // careful to take this out
////        request.hostname = "10.0.0.159";
////        request.port = 9999;
////        request.clientCount = 100;
//
//        request.minClientLifeMillis = 10000000;
//        request.minDelayMillisBeforeCreatingClient = 10;
//        request.minDelayMillisBetweenSends = 10;
//        request.minPayloadSize = 10000;
//
//        request.maxClientLifeMillis = 10000001;
//        request.maxDelayMillisBeforeCreatingClient = 1001;
//        request.maxDelayMillisBetweenSends = 101;
//        request.maxPayloadSize = 10001;

        RouplexTcpBinder tcpBinder;
        if (request.isUseSharedBinder()) {
            tcpBinder = request.isUseNiossl() ? niosslRouplexTcpBinder : classicRouplexTcpBinder;
        } else {
            tcpBinder = new RouplexTcpBinder(request.isUseNiossl() ? SSLSelector.open() : Selector.open());
        }

        String tag = String.format("%s.%s", request.isUseNiossl() ? "Niossl" : "Classic", request.isSsl() ? "Ssl" : "Plain");
        final Meter createdClients = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), "created", "client"));
        final Meter failedCreationClients = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), "uncreated", "client"));
        tcpBinder.setTcpClientAddedListener(new NotificationListener<RouplexTcpClient>() {
            @Override
            public void onEvent(RouplexTcpClient rouplexTcpClient) {
                rouplexTcpClients.add(rouplexTcpClient);
                new EchoClient(rouplexTcpClient, request, tag);
            }
        });

        for (int cc = 0; cc < request.clientCount; cc++) {
            long startClientMillis = request.minDelayMillisBeforeCreatingClient +
                    random.nextInt(request.maxDelayMillisBeforeCreatingClient - request.minDelayMillisBeforeCreatingClient);

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    try {
                        RouplexTcpClient.newBuilder()
                                .withRouplexTcpBinder(tcpBinder)
                                .withRemoteAddress(request.getHostname(), request.getPort())
                                .withSecure(request.isSsl(), null)
                                .build();
                        createdClients.mark();
                    } catch (Exception e) {
                        failedCreationClients.mark();
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

    class EchoServer {
        final SendChannel<ByteBuffer> sendChannel;
        final Throttle receiveThrottle;

        final Meter addedClients;
        final Meter removedClients;
        final Meter receivedBytes;
        final Meter sentBytes;
        final Histogram inSizes;
        final Histogram outSizes;
        final Timer pauseTime;
        Timer.Context pauseTimer;

        ByteBuffer sendBuffer;

        EchoServer(RouplexTcpClient rouplexTcpClient, String hostPort, String tag) {
            String clientId = "";//rouplexTcpClient.hashCode() + "";
            addedClients = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "connected", "client"));
            removedClients = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "disconnected", "client"));
            receivedBytes = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "received", "client"));
            sentBytes = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "sent", "client"));
            inSizes = benchmarkerMetrics.histogram(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "inSizes", "client"));
            outSizes = benchmarkerMetrics.histogram(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "outSizes", "client"));
            pauseTime = benchmarkerMetrics.timer(MetricRegistry.name(tag, EchoServer.class.getSimpleName(), hostPort, "pauseTime", "client"));

            addedClients.mark();

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
                        removedClients.mark();
                        return sendChannel.send(null);
                    }

                    inSizes.update(payload.length);
                    receivedBytes.mark(payload.length);
                    sendBuffer = ByteBuffer.wrap(payload);
                    return send();
                }
            });
        }

        private boolean send() {
            int position = sendBuffer.position();
            boolean sent = sendChannel.send(sendBuffer); // echo
            int sentSize = sendBuffer.position() - position;
            sentBytes.mark(sentSize);
            outSizes.update(sentSize);

            if (!sent) {
                pauseTimer = pauseTime.time();
            }
            return sent;
        }
    }

    public class EchoClient {
        final StartTcpClientsRequest request;
        final SendChannel<ByteBuffer> sendChannel;
        final Throttle receiveThrottle;

        final List<ByteBuffer> sendBuffers = new ArrayList<>();
        final int maxSendBufferSize;
        int currentSendBufferSize;
        long sent;
        long received;
        long closeTimestamp;

        final Meter addedClients;
        final Meter removedClients;
        final Meter receivedBytes;
        final Meter sentBytes;
        final Histogram sendBufferSize;
        final Meter discardedSendBytes;
        final Meter sizeCheckFailures;
        final Timer pauseTime;
        Timer.Context pauseTimer;

        EchoClient(RouplexTcpClient rouplexTcpClient, StartTcpClientsRequest request, String tag) {
            this.request = request;
            this.maxSendBufferSize = request.maxPayloadSize * 10;

            String clientId = "";//rouplexTcpClient.hashCode() + "";
            addedClients = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), clientId, "connected"));
            removedClients = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), clientId, "disconnected"));
            sentBytes = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), clientId, "sent"));
            sendBufferSize = benchmarkerMetrics.histogram(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), clientId, "sendBufferSize"));
            discardedSendBytes = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), clientId, "discardedSendBytes"));
            receivedBytes = benchmarkerMetrics.meter(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), clientId, "received"));
            sizeCheckFailures = benchmarkerMetrics.meter(MetricRegistry.name(EchoClient.class.getSimpleName(), "sizeCheckFailures"));
            pauseTime = benchmarkerMetrics.timer(MetricRegistry.name(tag, EchoClient.class.getSimpleName(), "pauseTime"));

            addedClients.mark();

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
                    if (payload != null) {
                        received += payload.length;
                        receivedBytes.mark(payload.length);
                    } else {
                        removedClients.mark();
                        if (received != sent) {
                            sizeCheckFailures.mark();
                        }
                    }

                    return true;
                }
            });

            closeTimestamp = System.currentTimeMillis() + request.minClientLifeMillis +
                    random.nextInt(request.maxClientLifeMillis - request.minClientLifeMillis);

            keepSendingThenClose();
        }

        void keepSendingThenClose() {
            int delay = request.getMinDelayMillisBetweenSends() +
                    random.nextInt(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends());

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    if (System.currentTimeMillis() > closeTimestamp) {
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
                                sendBuffers.add(ByteBuffer.allocate(payloadSize));
                                currentSendBufferSize += payloadSize;
                            }

                            sendBufferSize.update(currentSendBufferSize);
                            send();
                        }

                        keepSendingThenClose();
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
        }

        private void send() {
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
                    sent += payloadSize;
                    sentBytes.mark(payloadSize);
                }

                synchronized (sendBuffers) {
                    currentSendBufferSize -= payloadSize;
                    if (result) {
                        sendBuffers.remove(0);
                    } else {
                        pauseTimer = pauseTime.time();
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void close() throws IOException {
        for (Map.Entry<String, RouplexTcpServer> entry : rouplexTcpServers.entrySet()) {
            try {
                entry.getValue().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (RouplexTcpClient client : rouplexTcpClients) {
            try {
                client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        scheduledExecutor.shutdownNow();
        classicRouplexTcpBinder.close();
        niosslRouplexTcpBinder.close();
        jmxReporter.close();
    }
}