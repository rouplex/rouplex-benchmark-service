package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.tcp.TcpReadChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Reader implements Runnable {
    private static final Logger logger = Logger.getLogger(Reader.class.getSimpleName());

    // We can use a shared pool (since the BB used are only temporary containers between read and writes)
    // Not creating a ThreadLocal b/c down't want to promote this pattern at all
    private final static ConcurrentMap<Thread, ByteBuffer> byteBufferPool = new ConcurrentHashMap<>();

    final TcpReadChannel readChannel;
    final EchoReporter echoReporter;
    final Writer writer;
    Integer clientId;

    public Reader(TcpReadChannel readChannel, EchoReporter echoReporter, Writer writer) {
        this.readChannel = readChannel;
        this.echoReporter = echoReporter;
        this.writer = writer;
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

    public void run() {
        try {
            ByteBuffer byteBuffer = borrowByteBuffer();
            int totalRead = 0;

            while (true) {
                int read = readChannel.read(byteBuffer);

                if (read == -1) {
                    echoReporter.rcReadByte.mark(totalRead);
                    echoReporter.rcReadEos.mark();
                    if (writer != null) {
                        writer.shutdown();
                    } else {
                        // this is a sender, so close tcpClient now
                        closeTcpClient();
                    }
                    return;
                }

                if (read == 0) {
                    break;
                }

                totalRead += read;

                if (clientId == null) {
                    clientId = deserializeClientId(byteBuffer);
                    //tcpClient.setDebugId("S" + clientId );
                }

                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("Reader[%s] received %s bytes", clientId, read));
                }

                if (writer != null) {
                    byteBuffer.flip();
                    writer.write(byteBuffer);
                }

                // normally we would do compact, but we discard all (and report)
                byteBuffer.clear();
            }

            echoReporter.rcReadByte.mark(totalRead);
            echoReporter.rcReadSizes.update(totalRead);
            if (totalRead == 0) {
                echoReporter.rcRead0Byte.mark();
            }

            readChannel.addChannelReadyCallback(this);
        } catch (IOException ioe) {
            echoReporter.rcReadFailure.mark();
            echoReporter.metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "rcReadFailure", "EEE", ioe.getClass().getSimpleName(), ioe.getMessage())).mark();

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

    private static int deserializeClientId(ByteBuffer byteBuffer) {
        byte[] buffer = new byte[4];
        byteBuffer.get(buffer);

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
