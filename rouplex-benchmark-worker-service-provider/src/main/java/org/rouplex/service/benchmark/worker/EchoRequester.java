package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.tcp.TcpBroker;
import org.rouplex.platform.tcp.TcpClient;
import org.rouplex.platform.tcp.TcpClientLifecycleListener;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    final CreateTcpClientBatchRequest request;
    final MetricRegistry metricRegistry;
    final ScheduledExecutorService scheduledExecutor;
    final long timeCreatedNano = System.nanoTime();
    final ByteBuffer sendBuffer;

    long closeTimestamp;
    Writer writer;
    EchoReporter echoReporter;
    int clientId;

    EchoRequester(CreateTcpClientBatchRequest createTcpClientBatchRequest, MetricRegistry metricRegistry,
                  ScheduledExecutorService scheduledExecutor, TcpBroker tcpBroker) {

        this.request = createTcpClientBatchRequest;
        this.metricRegistry = metricRegistry;
        this.scheduledExecutor = scheduledExecutor;
        this.sendBuffer = ByteBuffer.allocate(createTcpClientBatchRequest.getMaxPayloadSize() - 1);

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

                        echoReporter = new EchoReporter(request, metricRegistry, EchoRequester.class,
                            tcpClient.getLocalAddress(), tcpClient.getRemoteAddress());

                        echoReporter.clientConnectionEstablished.mark();
                        echoReporter.clientConnectionLive.mark();

                        writer = new Writer(tcpClient.getWriteChannel(), sendBuffer.capacity(), echoReporter, false);
                        new Reader(tcpClient.getReadChannel(), 10000, echoReporter, null).run();

                        startSendingThenClose(tcpClient);
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
                    echoReporter = new EchoReporter(request, metricRegistry, EchoRequester.class, null, null);
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

            TcpClient tcpClient = tcpBroker.newTcpClientBuilder()
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

    void startSendingThenClose(TcpClient tcpClient) {
        long connectionNano = System.nanoTime() - timeCreatedNano;
        // poor man's implementation for bucketizing: just divide in buckets of power of 10 in millis (from nanos)
        echoReporter.clientConnectionTime.update(connectionNano, TimeUnit.NANOSECONDS);

        clientId = incrementalId.incrementAndGet();
        tcpClient.setDebugId("C" + clientId);
        serializeClientId(clientId, sendBuffer.array());

        closeTimestamp = System.currentTimeMillis() + request.getMinClientLifeMillis() +
            random.nextInt(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis());

        keepSendingThenClose();
    }

    void keepSendingThenClose() {
        try {
            if (System.currentTimeMillis() >= closeTimestamp) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("EchoRequester[%s] shutting down", clientId));
                }

                writer.write(null);
                return;
            }

            sendBuffer.position(0);
            int payloadSize = request.getMinPayloadSize() + random.nextInt(request.getMaxPayloadSize() - request.getMinPayloadSize());
            sendBuffer.limit(payloadSize);
            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("EchoRequester[%s] sending %s bytes", clientId, payloadSize));
            }

            writer.write(sendBuffer);

            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("EchoRequester[%s] sent %s bytes", clientId, sendBuffer.position()));
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
        } catch (IOException ioe) {
            // all reporting has happened inside writer already
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
