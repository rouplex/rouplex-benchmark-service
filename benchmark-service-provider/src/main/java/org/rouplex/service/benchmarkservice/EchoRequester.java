package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.Timer;
import org.rouplex.platform.rr.ReceiveChannel;
import org.rouplex.platform.rr.SendChannel;
import org.rouplex.platform.rr.Throttle;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmarkservice.tcp.StartTcpClientsRequest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */

public class EchoRequester {
    final ScheduledExecutorService scheduledExecutor;
    final StartTcpClientsRequest request;

    final int maxSendBufferSize;
    final long closeTimestamp;

    final SendChannel<ByteBuffer> sendChannel;
    final List<ByteBuffer> sendBuffers = new ArrayList<>();

    final Throttle receiveThrottle;

    final EchoReporter echoReporter;
    final Random random = new Random();

    final int clientId;

    int currentSendBufferSize;
    Timer.Context pauseTimer;

    EchoRequester(BenchmarkServiceProvider benchmarkServiceProvider, RouplexTcpClient rouplexTcpClient) throws IOException {
        this.request = (StartTcpClientsRequest) rouplexTcpClient.getAttachment();
        rouplexTcpClient.setAttachment(this);

        scheduledExecutor = benchmarkServiceProvider.scheduledExecutor;

        echoReporter = new EchoReporter(request, benchmarkServiceProvider.benchmarkerMetrics, EchoRequester.class);
        clientId = benchmarkServiceProvider.incrementalId.incrementAndGet();
        echoReporter.setClientId(clientId);

        try {
            echoReporter.setTcpClient(rouplexTcpClient);

            maxSendBufferSize = request.maxPayloadSize * 1;
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

            echoReporter.connected();

            keepSendingThenClose();
        } catch (RuntimeException e) {
            // RuntimeException (UnresolvedAddressException)
            echoReporter.unconnected();
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

    private static void serializeClientId(int clientId, byte[] buffer) {
        for (int i = 3; i >= 0; i--) {
            buffer[i] = (byte) (clientId & 0xFF);
            clientId >>>= 8;
        }
    }
}
