package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import org.rouplex.platform.io.Receiver;
import org.rouplex.platform.io.Sender;
import org.rouplex.platform.io.Throttle;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoRequester {
    private static final Logger logger = Logger.getLogger(EchoRequester.class.getSimpleName());
    private static final Random random = new Random();

    final VertxWorkerServiceProvider benchmarkServiceProvider;
    final CreateTcpClientBatchRequest request;
    final long timeCreatedNano = System.nanoTime();
    final ByteBuffer sendBuffer;

    long closeTimestamp;
    Sender<ByteBuffer> sender;
    Throttle receiveThrottle;
    EchoReporter echoReporter;

    EchoRequester(VertxWorkerServiceProvider benchmarkServiceProvider,
                  CreateTcpClientBatchRequest createTcpClientBatchRequest, Vertx vertx) {

        this.benchmarkServiceProvider = benchmarkServiceProvider;
        this.request = createTcpClientBatchRequest;
        this.sendBuffer = ByteBuffer.allocate(createTcpClientBatchRequest.getMaxPayloadSize());

        try {
            switch (createTcpClientBatchRequest.getProvider()) {
                case VERTX_IO:
                    NetClientOptions options = new NetClientOptions().setSsl(createTcpClientBatchRequest.isSsl()).setTrustAll(true);

                    vertx.createNetClient(options).connect(1234, "localhost", res -> {
                        if (res.succeeded()) {
                            NetSocket sock = res.result();
                            sock.handler(buff -> {
                                System.out.println("client receiving " + buff.toString("UTF-8"));
                            });

                            // Now send some data
                            for (int i = 0; i < 10; i++) {
                                String str = "hello " + i + "\n";
                                System.out.println("Net client sending: " + str);
                                sock.write(str);
                            }
                        } else {
                            System.out.println("Failed to connect " + res.cause());
                        }
                    });

                    break;
                default:
                    throw new Exception("Internal error");
            }

            if (createTcpClientBatchRequest.isSsl()) {
                switch (createTcpClientBatchRequest.getProvider()) {
                    case VERTX_IO:
                        Vertx vertx = Vertx.vertx(new VertxOptions().setClustered(false));
                        NetClientOptions options = new NetClientOptions();
                        .setSsl(true).setTrustAll(true);
                        vertx.createNetClient(options
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



            tcpBinder.newRouplexTcpClientBuilder()
                    .withSocketChannel(socketChannel)
                    .withRemoteAddress(createTcpClientBatchRequest.getHostname(), createTcpClientBatchRequest.getPort())
                    .withSecure(createTcpClientBatchRequest.isSsl(), null) // value ignored in current context since channel is provided
                    .withSendBufferSize(createTcpClientBatchRequest.getSocketSendBufferSize())
                    .withReceiveBufferSize(createTcpClientBatchRequest.getSocketReceiveBufferSize())
                    .withAttachment(this)
                    .buildAsync();

            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name("connection.started")).mark();
        } catch (Exception e) {
            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name("connection.start.failed")).mark();
            benchmarkServiceProvider.benchmarkerMetrics.meter(
                    MetricRegistry.name("connection.start.failed.EEE", e.getMessage())).mark();

            logger.warning(String.format("Failed creating EchoRequester. Cause: %s %s",
                e.getClass().getSimpleName(), e.getMessage()));

            throw new RuntimeException(e);
        }

        logger.info("Created EchoRequester");
    }

    void startSendingThenClose(NetSocket netSocket) throws IOException {
        echoReporter = new EchoReporter(
                request, benchmarkServiceProvider.benchmarkerMetrics, EchoRequester.class, netSocket);

        long connectionNano = System.nanoTime() - timeCreatedNano;
        echoReporter.connectionTime.update(connectionNano, TimeUnit.NANOSECONDS);
        echoReporter.connectionEstablished.mark();
        echoReporter.liveConnections.mark();
        int clientId = benchmarkServiceProvider.incrementalId.incrementAndGet();
        //netSocket.setDebugId("C" + clientId );
        serializeClientId(clientId, sendBuffer.array());

        closeTimestamp = System.currentTimeMillis() + request.getMinClientLifeMillis() +
                random.nextInt(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis());

        int sendBufferSize = (request.getMaxPayloadSize() -1) * 1;
        sender = netSocket.obtainSendChannel(new Throttle() {
            @Override
            public void resume() {
                // nothing really
            }
        }, sendBufferSize);

        netSocket.endHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                echoReporter.receivedEos.mark();
            }
        });

        netSocket.handler(new Handler<Buffer>() {
            @Override
            public void handle(Buffer event) {
                echoReporter.receivedBytes.mark(event.length());
                echoReporter.receivedSizes.update(event.length());
            }
        });

        netSocket.exceptionHandler(new Handler<Throwable>() {
            @Override
            public void handle(Throwable event) {
                echoReporter.receivedDisconnect.mark();
            }
        });

        netSocket.closeHandler(new Handler<Void>() {
            @Override
            public void handle(Void event) {
                // maybe receivedEos?
            }
        });

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

                benchmarkServiceProvider.scheduledExecutor.schedule(new Runnable() {
                    @Override
                    public void run() {
                        keepSendingThenClose();
                    }
                }, delay, TimeUnit.MILLISECONDS);
            }
        } catch (Exception e) {
            echoReporter.sendFailures.mark();
            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
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
