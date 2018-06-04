package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.platform.tcp.TcpWriteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Writer {
    private static final Logger logger = Logger.getLogger(Writer.class.getSimpleName());

    final TcpWriteChannel writeChannel;
    final EchoReporter echoReporter;
    final boolean closeTcpClientWhenShutdown;

    // todo, protect this
    Integer clientId;

    public Writer(TcpWriteChannel writeChannel, EchoReporter echoReporter, boolean closeTcpClientWhenShutdown) {
        this.writeChannel = writeChannel; //new TcpBufferedWriteChannel(writeChannel, 1_000_000, false, true);
        this.closeTcpClientWhenShutdown = closeTcpClientWhenShutdown;
        this.echoReporter = echoReporter;
    }

    void write(ByteBuffer bb) throws IOException {
        try {
            long startTimeNano = System.nanoTime();

            while (bb.hasRemaining()) {
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("Writer[%s] writing %s bytes", clientId, bb.remaining()));
                }

                int written = writeChannel.write(bb);
                if (logger.isLoggable(Level.INFO)) {
                    logger.info(String.format("Writer[%s] wrote %s bytes", clientId, written));
                }

                if (written == 0) {
                    break;
                }
            }

            echoReporter.wcWrittenByte.mark(bb.position());
            echoReporter.wcWrittenSizes.update(bb.position());
            echoReporter.wcDiscardedByteAtNic.mark(bb.remaining());
            echoReporter.wcWriteTimes.update(System.nanoTime() - startTimeNano, TimeUnit.NANOSECONDS);

            if (bb.position() == 0) {
                echoReporter.wcWritten0Byte.mark();
            }
        } catch (IOException ioe) {
            echoReporter.wcWriteFailure.mark();
            echoReporter.metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "wcWriteFailure", "EEE", ioe.getClass().getSimpleName(), ioe.getMessage())).mark();

            logger.warning(String.format("TcpClient[%s] failed sending. Cause: %s: %s",
                clientId, ioe.getClass().getSimpleName(), ioe.getMessage()));

            closeTcpClient();
            throw ioe;
        }
    }

    void shutdown() throws IOException {
        try {
            writeChannel.shutdown();
            echoReporter.wcWrittenEos.mark();

            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("Writer[%s] shutdown", clientId));
            }

            if (closeTcpClientWhenShutdown) {
                closeTcpClient();
            }
        } catch (Exception e) {
            echoReporter.metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "wcShutdownFailure", "EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

            logger.warning(String.format("TcpClient[%s] writeChannel failed shutting down. Cause: %s: %s",
                clientId, e.getClass().getSimpleName(), e.getMessage()));

            closeTcpClient();
            throw e;
        }
    }

    private void closeTcpClient() {
        try {
            echoReporter.clientClosed.mark();
            writeChannel.getTcpClient().close();

            if (logger.isLoggable(Level.INFO)) {
                logger.info(String.format("TcpClient[%s] closed", clientId));
            }
        } catch (IOException ioe) {
            echoReporter.metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "tcpClientCloseFailure", "EEE", ioe.getClass().getSimpleName(), ioe.getMessage())).mark();

            logger.warning(String.format("TcpClient[%s] failed to close properly. Cause: %s %s",
                clientId, ioe.getClass().getSimpleName(), ioe.getMessage()));
        }
    }
}
