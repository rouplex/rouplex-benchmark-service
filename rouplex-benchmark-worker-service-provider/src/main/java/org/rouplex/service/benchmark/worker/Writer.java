package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.MetricRegistry;
import org.rouplex.commons.annotations.GuardedBy;
import org.rouplex.platform.tcp.TcpWriteChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Writer implements Runnable {
    private static final Logger logger = Logger.getLogger(Writer.class.getSimpleName());
    private static final AtomicInteger zeroWrites = new AtomicInteger();
    private static final AtomicInteger nzeroWrites = new AtomicInteger();

    final TcpWriteChannel writeChannel;
    final ByteBuffer byteBuffer;
    final EchoReporter echoReporter;
    final boolean closeTcpClientWhenShutdown;

    @GuardedBy("lock")
    boolean shutdownRequested;

    // todo, protect this
    Integer clientId;

    public Writer(TcpWriteChannel writeChannel, int bufferSize, EchoReporter echoReporter, boolean closeTcpClientWhenShutdown) {
        this.writeChannel = writeChannel;
        this.closeTcpClientWhenShutdown = closeTcpClientWhenShutdown;
        this.byteBuffer = ByteBuffer.allocate(bufferSize);
        this.echoReporter = echoReporter;
    }

    public void run() {
        try {
            synchronized (writeChannel) {
                byteBuffer.flip();

                if (byteBuffer.hasRemaining()) {
                    if (logger.isLoggable(Level.INFO)) {
                        logger.info(String.format("Writer[%s] writing %s bytes", clientId, byteBuffer.remaining()));
                    }

                    int written = writeChannel.write(byteBuffer);
                    byteBuffer.compact();
                    if (logger.isLoggable(Level.INFO)) {
                        logger.info(String.format("Writer[%s] wrote %s bytes", clientId, written));
                    }

                    if (written == 0) {
//                        System.out.println("W0:" + writeChannel.getTcpClient().getDebugId() + ": "
//                            + zeroWrites.incrementAndGet() + " : " + nzeroWrites.get());
                        echoReporter.sent0Bytes.mark();
                    } else {
//                        System.out.println("W1:" + writeChannel.getTcpClient().getDebugId() + ": " +
//                            zeroWrites.get() + " : " + nzeroWrites.incrementAndGet());
                        echoReporter.sentBytes.mark(written);
                    }

                    echoReporter.sentSizes.update(written);
                }

                if (byteBuffer.position() > 0) {
                    writeChannel.addChannelReadyCallback(this);
                } else if (shutdownRequested) {
                    writeChannel.shutdown();
                    echoReporter.sentEos.mark();

                    if (logger.isLoggable(Level.INFO)) {
                        logger.info(String.format("Writer[%s] shutdown", clientId));
                    }

                    if (closeTcpClientWhenShutdown) {
                        closeTcpClient();
                    }
                }
            }
        } catch (IOException ioe) {
            echoReporter.sendFailures.mark();
            echoReporter.metricRegistry.meter(MetricRegistry.name(echoReporter.getAggregatedId(),
                "sendFailures", "EEE", ioe.getClass().getSimpleName(), ioe.getMessage())).mark();

            logger.warning(String.format("TcpClient[%s] failed sending. Cause: %s: %s",
                clientId, ioe.getClass().getSimpleName(), ioe.getMessage()));

            closeTcpClient();
        }
    }

    void write(ByteBuffer bb) throws IOException {
        synchronized (writeChannel) {
            if (!(shutdownRequested = bb == null)) {
                transfer(bb, byteBuffer);
            }
        }

        run();

        if (bb != null) {
            if (bb.hasRemaining()) {
                echoReporter.sentBytesDiscarded.mark(bb.remaining());
            }

            bb.clear();
        }
    }

    /**
     * Copy bytes from source to destination, adjusting their positions accordingly. This call does not fail if
     * destination cannot accommodate all the bytes available in source, but copies as many as possible.
     *
     * @param source      source buffer
     * @param destination destination buffer
     * @return the number of bytes copied
     */
    private int transfer(ByteBuffer source, ByteBuffer destination) {
        int srcRemaining;
        int destRemaining;

        if ((srcRemaining = source.remaining()) > (destRemaining = destination.remaining())) {
            // todo improve copy speed here as well
            int limit = source.limit();
            source.limit(source.position() + destRemaining);
            destination.put(source);
            source.limit(limit);
            return destRemaining;
        } else {
            if (srcRemaining > 0) {
                if (source.isDirect() || destination.isDirect()) {
                    destination.put(source);
                } else {
                    System.arraycopy(
                        source.array(), source.position(),
                        destination.array(), destination.position(),
                        srcRemaining);

                    source.position(source.position() + srcRemaining);
                    destination.position(destination.position() + srcRemaining);
                }
            }

            return srcRemaining;
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
            logger.warning(String.format("TcpClient[%s] failed to close properly. Cause: %s %s",
                clientId, ioe.getClass().getSimpleName(), ioe.getMessage()));
        }
    }
}
