package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.io.Receiver;
import org.rouplex.platform.io.Sender;
import org.rouplex.platform.io.Throttle;
import org.rouplex.platform.tcp.RouplexTcpBroker;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpClientListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoRequester {
    private static final Logger logger = Logger.getLogger(EchoRequester.class.getSimpleName());
    private static final AtomicInteger incrementalId = new AtomicInteger();
    private static final Random random = new Random();

    final CreateTcpClientBatchRequest request;
    final MetricRegistry metricRegistry;
    final ScheduledExecutorService scheduledExecutor;
    final long timeCreatedNano = System.nanoTime();
    final ByteBuffer sendBuffer;

    long closeTimestamp;
    Sender<ByteBuffer> sender;
    Throttle receiveThrottle;
    EchoReporter echoReporter;

    EchoRequester(CreateTcpClientBatchRequest createTcpClientBatchRequest, MetricRegistry metricRegistry,
                  ScheduledExecutorService scheduledExecutor, RouplexTcpBroker rouplexTcpBroker) {

        this.request = createTcpClientBatchRequest;
        this.metricRegistry = metricRegistry;
        this.scheduledExecutor = scheduledExecutor;
        this.sendBuffer = ByteBuffer.allocate(createTcpClientBatchRequest.getMaxPayloadSize());

        try {
            SocketChannel socketChannel;
            if (createTcpClientBatchRequest.isSsl()) {
                switch (createTcpClientBatchRequest.getProvider()) {
                    case ROUPLEX_NIOSSL:
                        socketChannel = org.rouplex.nio.channels.SSLSocketChannel.open(RouplexTcpClient.buildRelaxedSSLContext());
                        break;
                    case THIRD_PARTY_SSL:
                        // socketChannel = thirdPartySsl.SSLSocketChannel.open(RouplexTcpClient.buildRelaxedSSLContext());
                        // break;
                    case CLASSIC_NIO:
                    default:
                        throw new Exception("This provider cannot provide ssl communication");
                }
            } else {
                socketChannel = SocketChannel.open();
            }

            RouplexTcpClientListener rouplexTcpClientListener = new RouplexTcpClientListener() {
                @Override
                public void onConnected(RouplexTcpClient rouplexTcpClient) {
                    try {
                        startSendingThenClose(rouplexTcpClient);
                    } catch (Exception e) {
                        //logExceptionTraceTemp(e);
                        metricRegistry.meter(MetricRegistry.name("BenchmarkInternalError.onConnected")).mark();
                        metricRegistry.meter(MetricRegistry.name("BenchmarkInternalError.onConnected.EEE",
                            e.getClass().getSimpleName(), e.getMessage())).mark();

                        logger.warning(String.format("Failed handling onConnected(). Cause: %s: %s",
                            e.getClass().getSimpleName(), e.getMessage()));
                    }
                }

                @Override
                public void onConnectionFailed(RouplexTcpClient rouplexTcpClient, Exception reason) {
                    metricRegistry.meter(MetricRegistry.name("connection.failed")).mark();
                    metricRegistry.meter(MetricRegistry.name("connection.failed.EEE",
                        reason.getClass().getSimpleName(), reason.getMessage())).mark();

                    logger.warning(String.format("Failed connecting EchoRequester. Cause: %s: %s",
                        reason.getClass().getSimpleName(), reason.getMessage()));
                }

                @Override
                public void onDisconnected(RouplexTcpClient rouplexTcpClient,
                                           Exception optionalReason, boolean drainedChannels) {
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

            rouplexTcpBroker.newRouplexTcpClientBuilder()
                .withSocketChannel(socketChannel)
                .withRemoteAddress(createTcpClientBatchRequest.getHostname(), createTcpClientBatchRequest.getPort())
                .withSecure(createTcpClientBatchRequest.isSsl(), null) // value ignored in current context since channel is provided
                .withRouplexTcpClientListener(rouplexTcpClientListener)
                .withSendBufferSize(createTcpClientBatchRequest.getSocketSendBufferSize())
                .withReceiveBufferSize(createTcpClientBatchRequest.getSocketReceiveBufferSize())
                .withAttachment(this)
                .buildAsync();

            metricRegistry.meter(MetricRegistry.name("connection.started")).mark();
        } catch (Exception e) {
            metricRegistry.meter(MetricRegistry.name("connection.start.failed")).mark();
            metricRegistry.meter(MetricRegistry.name("connection.start.failed.EEE", e.getMessage())).mark();

            logger.warning(String.format("Failed creating EchoRequester. Cause: %s %s",
                e.getClass().getSimpleName(), e.getMessage()));

            throw new RuntimeException(e);
        }

        logger.info("Created EchoRequester");
    }

    void startSendingThenClose(RouplexTcpClient rouplexTcpClient) throws IOException {
        echoReporter = new EchoReporter(request, metricRegistry, EchoRequester.class, rouplexTcpClient);

        long connectionNano = System.nanoTime() - timeCreatedNano;
        // poor man's implementation for bucketizing: just divide in buckets of power of 10 in millis (from nanos)
        echoReporter.connectionTime.update(connectionNano, TimeUnit.NANOSECONDS);
        echoReporter.connectionEstablished.mark();
        echoReporter.liveConnections.mark();
        int clientId = incrementalId.incrementAndGet();
        rouplexTcpClient.setDebugId("C" + clientId );
        serializeClientId(clientId, sendBuffer.array());

        closeTimestamp = System.currentTimeMillis() + request.getMinClientLifeMillis() +
                random.nextInt(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis());

        int sendBufferSize = (request.getMaxPayloadSize() -1) * 1;
        sender = rouplexTcpClient.obtainSendChannel(new Throttle() {
            @Override
            public void resume() {
                // nothing really
            }
        }, sendBufferSize);

        receiveThrottle = rouplexTcpClient.assignReceiveChannel(new Receiver<byte[]>() {
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

        keepSendingThenClose();
    }

    void keepSendingThenClose() {
        int payloadSize = System.currentTimeMillis() >= closeTimestamp ? 0 /*EOS*/ : request.getMinPayloadSize() +
            random.nextInt(request.getMaxPayloadSize() - request.getMinPayloadSize());

        sendBuffer.position(0);
        sendBuffer.limit(payloadSize);

        try {
            sender.send(sendBuffer);

            if (payloadSize == 0) {
                echoReporter.sentEos.mark();
            } else {
                echoReporter.sentSizes.update(sendBuffer.position());
                echoReporter.sentBytes.mark(sendBuffer.position());
                echoReporter.discardedSendBytes.mark(sendBuffer.limit() - sendBuffer.position());
            }

            if (payloadSize != 0) {
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
        } catch (Exception e) {
            echoReporter.sendFailures.mark();
            metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "sendFailures", "EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

            logger.warning(String.format("Failed sending. Cause: %s: %s",
                e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private static void serializeClientId(int clientId, byte[] buffer) {
        if (buffer.length > 3) {
            for (int i = 3; i >= 0; i--) {
                buffer[i] = (byte) (clientId & 0xFF);
                clientId >>>= 8;
            }
        }
    }
}
