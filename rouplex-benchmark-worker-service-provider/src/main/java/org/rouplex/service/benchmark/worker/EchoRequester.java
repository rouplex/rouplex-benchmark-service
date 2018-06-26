package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.tcp.TcpClient;
import org.rouplex.platform.tcp.TcpClientLifecycleListener;
import org.rouplex.platform.tcp.TcpReactor;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoRequester {
    private static final Logger logger = Logger.getLogger(EchoRequester.class.getSimpleName());
    private static final AtomicInteger incrementalId = new AtomicInteger();
    private static final Random random = new Random();

    // We can use a shared pool (since the BB used are only temporary containers between read and writes)
    // Not creating a ThreadLocal b/c down't want to promote this pattern at all
    private final static ConcurrentMap<Thread, ByteBuffer> byteBufferPool = new ConcurrentHashMap<>();

    final CreateTcpClientBatchRequest request;
    final MetricRegistry metricRegistry;
    final RouplexScheduledExecutorFixedThreads scheduledExecutor;
    final long timeCreatedNano = System.nanoTime();

    final long avgDelayBetweenWritesNano;
    final long avgWritePacketSize;
    long closeTimestamp;
    Writer writer;
    EchoReporter echoReporter;
    int clientId;

    EchoRequester(CreateTcpClientBatchRequest createTcpClientBatchRequest, MetricRegistry metricRegistry,
                  RouplexScheduledExecutorFixedThreads scheduledExecutor, TcpReactor tcpReactor) {

        this.request = createTcpClientBatchRequest;
        this.metricRegistry = metricRegistry;
        this.scheduledExecutor = scheduledExecutor;

        avgDelayBetweenWritesNano = 1_000_000 *
            (request.getMaxDelayMillisBetweenSends() - 1 + request.getMinDelayMillisBetweenSends()) / 2;
        avgWritePacketSize = (request.getMaxPayloadSize() - 1 + request.getMinPayloadSize()) / 2;

        try {
            SocketChannel socketChannel;
            if (createTcpClientBatchRequest.isSsl()) {
                switch (createTcpClientBatchRequest.getProvider()) {
                    case ROUPLEX_NIOSSL:
                        socketChannel = org.rouplex.nio.channels.SSLSocketChannel.open(TcpClient.buildRelaxedSSLContext());
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

            TcpClientLifecycleListener tcpClientLifecycleListener = new TcpClientLifecycleListener() {
                @Override
                public void onConnected(TcpClient tcpClient) {
                    try {
                        logger.info("Connected EchoRequester");

                        echoReporter = new EchoReporter(request, metricRegistry, "EchoRequester",
                            tcpClient.getLocalAddress(), tcpClient.getRemoteAddress());

                        echoReporter.clientConnectionEstablished.mark();
                        echoReporter.clientConnectionLive.mark();

                        writer = new Writer(tcpClient.getWriteChannel(), echoReporter, false);
                        new Reader(tcpClient.getReadChannel(), echoReporter, null).run();

                        startSendingThenClose();
                    } catch (Exception e) {
                        //logExceptionTraceTemp(e);
                        metricRegistry.meter(MetricRegistry.name("internalError.clientConnection")).mark();
                        metricRegistry.meter(MetricRegistry.name("internalError.clientConnection.EEE",
                            e.getClass().getSimpleName(), e.getMessage())).mark();

                        logger.warning(String.format("Failed handling onConnected(). Cause: %s: %s",
                            e.getClass().getSimpleName(), e.getMessage()));
                    }
                }

                @Override
                public void onConnectionFailed(TcpClient tcpClient, Exception reason) {
                    echoReporter = new EchoReporter(request, metricRegistry, "EchoRequester", null, null);
                    echoReporter.clientConnectionFailed.mark();
                    metricRegistry.meter(MetricRegistry.name("clientConnectionFailed.EEE",
                        reason.getClass().getSimpleName(), reason.getMessage())).mark();

                    logger.warning(String.format("Failed connecting EchoRequester. Cause: %s: %s",
                        reason.getClass().getSimpleName(), reason.getMessage()));
                }

                @Override
                public void onDisconnected(TcpClient tcpClient, Exception optionalReason) {
                    if (echoReporter == null) {
                        logger.warning("Rouplex internal error: onDisconnected was fired before onConnected");
                        metricRegistry.meter(MetricRegistry.name("internalError.clientDisconnected.EEE",
                            optionalReason.getClass().getSimpleName(), optionalReason.getMessage())).mark();
                    } else {
                        echoReporter.clientConnectionLive.mark(-1);
                        if (optionalReason == null) {
                            echoReporter.clientDisconnectedOk.mark();
                        } else {
                            echoReporter.clientDisconnectedKo.mark();
                            metricRegistry.meter(MetricRegistry.name("clientDisconnectedKo.EEE",
                                optionalReason.getClass().getSimpleName(), optionalReason.getMessage())).mark();
                        }

                        if (logger.isLoggable(Level.INFO)) {
                            logger.info(String.format("Disconnected EchoRequester[%s]. OptionalReason: %s",
                                clientId, optionalReason == null ? null : optionalReason.getMessage()));
                        }
                    }
                }
            };

            TcpClient tcpClient = tcpReactor.newTcpClientBuilder()
                .withSocketChannel(socketChannel)
                .withRemoteAddress(createTcpClientBatchRequest.getHostname(), createTcpClientBatchRequest.getPort())
                .withSecure(createTcpClientBatchRequest.isSsl(), null) // value ignored in current context since channel is provided
                .withAttachment(this)
                .withTcpClientLifecycleListener(tcpClientLifecycleListener)
                .build();

            if (createTcpClientBatchRequest.getSocketSendBufferSize() > 0) {
                tcpClient.getSocket().setSendBufferSize(createTcpClientBatchRequest.getSocketSendBufferSize());
            }

            if (createTcpClientBatchRequest.getSocketReceiveBufferSize() > 0) {
                tcpClient.getSocket().setReceiveBufferSize(createTcpClientBatchRequest.getSocketReceiveBufferSize());
            }

            tcpClient.connect();

            metricRegistry.meter(MetricRegistry.name("clientConnectionStarted")).mark();
        } catch (Exception e) {
            metricRegistry.meter(MetricRegistry.name("clientConnectionStarted")).mark();
            metricRegistry.meter(MetricRegistry.name("clientConnectionStarted.EEE", e.getMessage())).mark();

            logger.warning(String.format("Failed creating EchoRequester. Cause: %s %s",
                e.getClass().getSimpleName(), e.getMessage()));

            throw new RuntimeException(e);
        }

        logger.info("Created EchoRequester");
    }

    ByteBuffer borrowByteBuffer() {
        ByteBuffer result = byteBufferPool.get(Thread.currentThread());
        if (result == null) {
            byteBufferPool.put(Thread.currentThread(), result = ByteBuffer.allocateDirect(1_000_000));
        } else {
            result.clear();
        }

        return result;
    }

    void startSendingThenClose() {
        long connectionNano = System.nanoTime() - timeCreatedNano;
        // poor man's implementation for bucketizing: just divide in buckets of power of 10 in millis (from nanos)
        echoReporter.clientConnectionTimes.update(connectionNano, TimeUnit.NANOSECONDS);

        closeTimestamp = System.currentTimeMillis() + request.getMinClientLifeMillis() +
            random.nextInt(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis());

        nextWriteTimestamp = System.nanoTime();
        keepSendingThenClose();
    }

    long nextWriteTimestamp;

    void keepSendingThenClose() {
        ByteBuffer sendBuffer = borrowByteBuffer();

//        if (clientId == 0) {
//            clientId = incrementalId.incrementAndGet();
//            // tcpClient.setDebugId("C" + clientId);
//            serializeClientId(clientId, sendBuffer);
//        }

        try {
            if (System.currentTimeMillis() >= closeTimestamp) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("EchoRequester[%s] shutting down as scheduled", clientId));
                }

                writer.shutdown();
                return;
            }

            int payloadSize = request.getMinPayloadSize() + random.nextInt(request.getMaxPayloadSize() - request.getMinPayloadSize());
            sendBuffer.position(0);
            sendBuffer.limit(payloadSize);

            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("EchoRequester[%s] sending %s bytes", clientId, payloadSize));
            }

            writer.write(sendBuffer);

            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("EchoRequester[%s] sent %s bytes", clientId, sendBuffer.position()));
            }

            nextWriteTimestamp += 1000000 * (request.getMinDelayMillisBetweenSends() +
                random.nextInt(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends()));

            long nowNano = System.nanoTime();
            long delayNano = nextWriteTimestamp - nowNano;
            if (delayNano < 0) {
                echoReporter.wcDelaysNegative.update(-delayNano, TimeUnit.NANOSECONDS);
                long skips = 1 - delayNano / avgDelayBetweenWritesNano;
                echoReporter.wcDiscardedByteAtCpu.mark(avgWritePacketSize * skips);
                nextWriteTimestamp = nowNano;
            } else {
                echoReporter.wcDelaysPositive.update(delayNano, TimeUnit.NANOSECONDS);
            }

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    keepSendingThenClose();
                }
            }, delayNano);

        } catch (IOException ioe) {
            // all reporting has happened inside writer already
        } catch (RejectedExecutionException ree) { // Never seen it happen
            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("EchoRequester[%s] shutting down (lack of system resources: RejectedExecutionException)", clientId));
            }

            try {
                writer.shutdown();
            } catch (IOException ioe) {
                // all reporting has happened inside writer already
            }
        }
    }

    private static void serializeClientId(int clientId, ByteBuffer byteBuffer) {
        byte[] buffer = new byte[4];

        if (buffer.length > 3) {
            for (int i = 3; i >= 0; i--) {
                buffer[i] = (byte) (clientId & 0xFF);
                clientId >>>= 8;
            }
        }

        byteBuffer.put(buffer);
    }
}
