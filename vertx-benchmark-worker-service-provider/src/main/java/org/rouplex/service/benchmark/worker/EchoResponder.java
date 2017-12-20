package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.io.Receiver;
import org.rouplex.platform.io.Sender;
import org.rouplex.platform.io.Throttle;
import org.rouplex.platform.tcp.RouplexTcpClient;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoResponder {
    private static final Logger logger = Logger.getLogger(EchoResponder.class.getSimpleName());
    final VertxWorkerServiceProvider benchmarkServiceProvider;

    final Sender<ByteBuffer> sender;
    private final Throttle receiveThrottle;

    final EchoReporter echoReporter;

    ByteBuffer sendBuffer;
    Integer clientId;

    EchoResponder(CreateTcpServerRequest createTcpServerRequest, VertxWorkerServiceProvider benchmarkServiceProvider,
                  RouplexTcpClient rouplexTcpClient) throws IOException {

        this.benchmarkServiceProvider = benchmarkServiceProvider;
        rouplexTcpClient.setAttachment(this);

        echoReporter = new EchoReporter(createTcpServerRequest,
                benchmarkServiceProvider.benchmarkerMetrics, EchoResponder.class, rouplexTcpClient);

        echoReporter.connectionEstablished.mark();
        echoReporter.liveConnections.mark();

        sender = rouplexTcpClient.obtainSendChannel(new Throttle() {
            @Override
            public void resume() {
                if (send()) {
                    receiveThrottle.resume();
                }
            }
        }, 0 /*socketSendBufferSize or default*/);

        receiveThrottle = rouplexTcpClient.assignReceiveChannel(new Receiver<byte[]>() {
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
                        rouplexTcpClient.setDebugId("S" + clientId );
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
            sender.send(sendBuffer); // echo as many as possible

            if (sendBuffer.capacity() == 0) {
                echoReporter.sentEos.mark();
            } else {
                int sentSize = sendBuffer.position() - position;
                echoReporter.sentBytes.mark(sentSize);
                echoReporter.sentSizes.update(sentSize);
            }

            return !sendBuffer.hasRemaining();
        } catch (Exception e) {
            echoReporter.sendFailures.mark();
            benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "sendFailures", "EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

            logger.warning(String.format("Failed sending. Cause: %s: %s",
                e.getClass().getSimpleName(), e.getMessage()));

            return false;
        }
    }

    private static int deserializeClientId(byte[] buffer) {
        int clientId = 0;
        if (buffer.length > 3) {
            for (int i = 0; i < 4; i++) {
                clientId <<= 8;
                clientId |= buffer[i] & 0xFF;
            }
        }

        return clientId;
    }
}
