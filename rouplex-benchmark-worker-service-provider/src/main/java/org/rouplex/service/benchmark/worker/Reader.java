package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.tcp.TcpReadChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Reader implements Runnable {
    private static final Logger logger = Logger.getLogger(Reader.class.getSimpleName());
    private static final AtomicInteger zeroReads = new AtomicInteger();
    private static final AtomicInteger nzeroReads = new AtomicInteger();

    final TcpReadChannel readChannel;
    final ByteBuffer byteBuffer;
    final EchoReporter echoReporter;
    final Writer writer;
    Integer clientId;

    public Reader(TcpReadChannel readChannel, int bufferSize, EchoReporter echoReporter, Writer writer) {
        this.readChannel = readChannel;
        this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.echoReporter = echoReporter;
        this.writer = writer;
    }

    public void run() {
        try {
            while (true) {
                int read = readChannel.read(byteBuffer);

                if (read == -1) {
                    echoReporter.receivedEos.mark();
                    if (writer != null) {
                        writer.write(null);
                    } else {
                        // this is a sender, so close tcpClient now
                        closeTcpClient();
                    }
                    return;
                }

                if (read == 0) {
//                    System.out.println("R0:" + readChannel.getTcpClient().getDebugId()
//                        + ": " + zeroReads.incrementAndGet() + " : " + nzeroReads.get());
                    echoReporter.received0Bytes.mark();
                    break;
                }

//                System.out.println("R1:" + readChannel.getTcpClient().getDebugId()
//                    + ": " + zeroReads.get() + " : " + nzeroReads.incrementAndGet());
                echoReporter.receivedBytes.mark(read);
                echoReporter.receivedSizes.update(read);

                if (clientId == null) {
                    clientId = deserializeClientId(byteBuffer.array());
                    //tcpClient.setDebugId("S" + clientId );
                }

                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("Reader[%s] received %s bytes", clientId, read));
                }

                if (writer != null) {
                    byteBuffer.flip();
                    writer.write(byteBuffer);
                } else {
                    byteBuffer.clear();
                }
            }

            readChannel.addChannelReadyCallback(this);
        } catch (IOException ioe) {
            echoReporter.receivedFailures.mark();
            echoReporter.metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "receivedFailures", "EEE", ioe.getClass().getSimpleName(), ioe.getMessage())).mark();

            logger.warning(String.format("TcpClient[%s] failed receiving. Cause: %s: %s",
                clientId, ioe.getClass().getSimpleName(), ioe.getMessage()));

            closeTcpClient();
        }
    }

    private void closeTcpClient() {
        try {
            echoReporter.clientClosed.mark();
            readChannel.getTcpClient().close();

            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("TcpClient[%s] closed", clientId));
            }
        } catch (IOException ioe) {
            logger.warning(String.format("TcpClient[%s] failed to close properly. Cause: %s %s",
                clientId, ioe.getClass().getSimpleName(), ioe.getMessage()));
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
