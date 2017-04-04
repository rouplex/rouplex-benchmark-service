package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.Timer;
import org.rouplex.platform.io.ReceiveChannel;
import org.rouplex.platform.io.SendChannel;
import org.rouplex.platform.io.Throttle;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmarkservice.tcp.Request;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */

public class EchoResponder {
    private static final Logger logger = Logger.getLogger(EchoResponder.class.getSimpleName());
    final SendChannel<ByteBuffer> sendChannel;
    final Throttle receiveThrottle;

    final EchoReporter echoReporter;
    Timer.Context pauseTimer;

    ByteBuffer sendBuffer;
    Integer clientId;

    EchoResponder(Request request, BenchmarkServiceProvider benchmarkServiceProvider, RouplexTcpClient rouplexTcpClient) throws IOException {
        rouplexTcpClient.setAttachment(this);

        echoReporter = new EchoReporter(request,
                benchmarkServiceProvider.benchmarkerMetrics, EchoResponder.class, rouplexTcpClient);

        echoReporter.connected.mark();

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
                    }

                    echoReporter.receivedSizes.update(payload.length);
                    echoReporter.receivedBytes.mark(payload.length);
                }

                sendBuffer = ByteBuffer.wrap(payload);
                return send();
            }
        }, true);

        logger.info("Created EchoResponder");
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

    private static int deserializeClientId(byte[] buffer) {
        int clientId = 0;
        for (int i = 0; i < 4; i++) {
            clientId <<= 8;
            clientId |= buffer[i] & 0xFF;
        }

        return clientId;
    }
}
