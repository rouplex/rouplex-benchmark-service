package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.rouplex.platform.io.Receiver;
import org.rouplex.platform.io.Sender;
import org.rouplex.platform.io.Throttle;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoRequester {
    private static final Logger logger = Logger.getLogger(EchoRequester.class.getSimpleName());
    final BenchmarkWorkerServiceProvider benchmarkServiceProvider;
    final StartTcpClientsRequest request;

    int maxSendBufferSize;
    long closeTimestamp;

    Sender<ByteBuffer> sender;
    final List<ByteBuffer> sendBuffers = new ArrayList<ByteBuffer>();

    Throttle receiveThrottle;

    EchoReporter echoReporter;
    final Random random = new Random();

    int clientId;

    int currentSendBufferSize;
    Timer.Context pauseTimer;
    long timeCreatedNano = System.nanoTime();

    EchoRequester(BenchmarkWorkerServiceProvider benchmarkServiceProvider,
                  StartTcpClientsRequest startTcpClientsRequest, RouplexTcpBinder tcpBinder) {

        this.benchmarkServiceProvider = benchmarkServiceProvider;
        this.request = startTcpClientsRequest;

        try {
            SocketChannel socketChannel;
            if (startTcpClientsRequest.isSsl()) {
                switch (startTcpClientsRequest.getProvider()) {
                    case ROUPLEX_NIOSSL:
                        socketChannel = org.rouplex.nio.channels.SSLSocketChannel.open(RouplexTcpClient.buildRelaxedSSLContext());
                        break;
                    case SCALABLE_SSL:
                        socketChannel = scalablessl.SSLSocketChannel.open(RouplexTcpClient.buildRelaxedSSLContext());
                        break;
                    case CLASSIC_NIO:
                    default:
                        throw new Exception("This provider cannot provide ssl communication");
                }
            } else {
                socketChannel = SocketChannel.open();
            }

            RouplexTcpClient.newBuilder()
                    .withRouplexTcpBinder(tcpBinder)
                    .withSocketChannel(socketChannel)
                    .withRemoteAddress(startTcpClientsRequest.getHostname(), startTcpClientsRequest.getPort())
                    .withSecure(startTcpClientsRequest.isSsl(), null) // value ignored in current context since channel is provided
                    .withSendBufferSize(startTcpClientsRequest.getSocketSendBufferSize())
                    .withReceiveBufferSize(startTcpClientsRequest.getSocketReceiveBufferSize())
                    .withAttachment(this)
                    .buildAsync();

            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name("connection.started")).mark();
        } catch (Exception e) {
            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name("connection.start.failed")).mark();
            benchmarkServiceProvider.benchmarkerMetrics.meter(
                    MetricRegistry.name("connection.start.failed.EEE", e.getMessage())).mark();
            logger.info("Failed creating EchoRequester");
            throw new RuntimeException(e);
        }

        logger.info("Created EchoRequester");
    }

    void startSendingThenClose(RouplexTcpClient rouplexTcpClient) throws IOException {
        echoReporter = new EchoReporter(
                request, benchmarkServiceProvider.benchmarkerMetrics, EchoRequester.class, rouplexTcpClient);

        echoReporter.connectionTime.update(System.nanoTime() - timeCreatedNano, TimeUnit.NANOSECONDS);
        echoReporter.connectionEstablished.mark();
        echoReporter.liveConnections.mark();
        clientId = benchmarkServiceProvider.incrementalId.incrementAndGet();

        maxSendBufferSize = request.maxPayloadSize * 1;
        closeTimestamp = System.currentTimeMillis() + request.minClientLifeMillis +
                random.nextInt(request.maxClientLifeMillis - request.minClientLifeMillis);

        sender = rouplexTcpClient.hookSendChannel(new Throttle() {
            @Override
            public void resume() {
                pauseTimer.stop(); // this should be reporting the time paused
                send();
            }
        });

        receiveThrottle = rouplexTcpClient.hookReceiveChannel(new Receiver<byte[]>() {
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

            if (payloadSize > 0 && !send()) {
                return;
            }

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
    }

    private boolean send() {
        try {
            while (true) {
                synchronized (sendBuffers) {
                    if (sendBuffers.isEmpty()) {
                        break;
                    }
                    ByteBuffer sendBuffer = sendBuffers.get(0);

                    int position = sendBuffer.position();
                    sender.send(sendBuffer);
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

            return true;
        } catch (Exception e) {
            echoReporter.sendFailures.mark();
            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                    "sendFailures", "EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

            return false;
        }
    }

    private static void serializeClientId(int clientId, byte[] buffer) {
        for (int i = 3; i >= 0; i--) {
            buffer[i] = (byte) (clientId & 0xFF);
            clientId >>>= 8;
        }
    }
}
