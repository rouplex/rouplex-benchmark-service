package org.rouplex.nio.channels;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmarkservice.BenchmarkService;
import org.rouplex.service.benchmarkservice.BenchmarkServiceProvider;
import org.rouplex.service.benchmarkservice.tcp.Provider;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerResponse;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@RunWith(Parameterized.class)
public class ChannelShutdownParityTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{{true}, {false}});
    }

    boolean secure;
    String hostname;
    int port;
    ExecutorService es = Executors.newSingleThreadExecutor();

    public ChannelShutdownParityTest(boolean secure) throws IOException {
        this.secure = secure;
    }

    @Before
    public void startServer() throws Exception {
        BenchmarkService bmService = BenchmarkServiceProvider.get();

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setProvider(Provider.ROUPLEX_NIOSSL);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(secure);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);
        port = startTcpServerResponse.getPort();
        hostname = startTcpServerResponse.getHostname();
    }

    @Test
    public void verifyChannelWriteThrowsIfShutdownOutputIsInvokedConcurrently() throws Exception {
        final SocketChannel socketChannel = secure ? SSLSocketChannel.open(RouplexTcpClient.buildRelaxedSSLContext()) : SocketChannel.open();
        final StringBuilder sb = new StringBuilder();

        final AtomicReference<Object> result = new AtomicReference<>();
        final AtomicInteger loopCount = new AtomicInteger();

        final Object lock = new Object();

        socketChannel.connect(new InetSocketAddress(hostname, port));
        socketChannel.configureBlocking(false);

        es.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 1000; i++) {
                        synchronized (lock) {
                            sb.append("StartWrite: ").append(System.currentTimeMillis()).append("\n");
                            loopCount.set(i);
                            lock.notifyAll();
                        }
                        socketChannel.write(ByteBuffer.allocate(100000));
                        synchronized (lock) {
                            sb.append("FinishWrite: ").append(System.currentTimeMillis()).append("\n");
                        }
                    }

                    synchronized (lock) {
                        sb.append("FinishLoop: ").append(System.currentTimeMillis()).append("\n");
                        result.set("No Exception");
                        lock.notifyAll();
                    }
                } catch (IOException e) {
                    synchronized (lock) {
                        sb.append("ErrorWrite: ").append(System.currentTimeMillis()).append(e.getClass()).append("\n");
                        result.set(e);
                        lock.notifyAll();
                    }
                }
            }
        });

        synchronized (lock) {
            while (loopCount.get() < 10) {
                lock.wait();

                if (result.get() != null) {
                    throw (Exception) result.get();
                }
            }
            sb.append("Shutdown Start: Loop: ").append(loopCount).append(" Time: ").append(System.currentTimeMillis()).append("\n");
        }

        socketChannel.shutdownOutput(); // if we are lucky we will invoke this during a write

        synchronized (lock) {
            sb.append("Shutdown Finish: Loop: ").append(loopCount).append(" Time: ").append(System.currentTimeMillis()).append("\n");
        }

        synchronized (lock) {
            while (result.get() == null) {
                lock.wait();
            }
        }

        Object value = result.get();
        Assert.assertTrue(value instanceof ClosedChannelException || value instanceof AsynchronousCloseException);
        System.out.println(sb);
    }
}
