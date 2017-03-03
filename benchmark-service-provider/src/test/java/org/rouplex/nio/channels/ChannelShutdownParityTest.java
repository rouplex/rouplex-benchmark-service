package org.rouplex.nio.channels;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rouplex.service.benchmarkservice.BenchmarkService;
import org.rouplex.service.benchmarkservice.BenchmarkServiceProvider;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerResponse;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@RunWith(Parameterized.class)
public class ChannelShutdownParityTest {
    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        return Arrays.asList(new Object[][]{{true}});
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
        setSystemProperties();
        BenchmarkService bmService = BenchmarkServiceProvider.get();

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setUseNiossl(secure);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(secure);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);
        port = startTcpServerResponse.getPort();
        hostname = startTcpServerResponse.getHostname();
    }

    private static void setSystemProperties() {
//        System.setProperty("javax.net.ssl.keyStore", "src/test/resources/server-keystore");
//        System.setProperty("javax.net.ssl.keyStorePassword", "kotplot");
//        System.setProperty("java.protocol.handler.pkgs", "com.sun.net.ssl.internal.www.protocol");
//        System.setProperty("javax.net.debug", "ssl");
    }

    @Test
    public void verifyShutdownOutputInvokedDuringConcurrentWriteDoesNotThrow() throws Exception {
        final SocketChannel socketChannel = secure ? SSLSocketChannel.open(buildRelaxedSSLContext()) : SocketChannel.open();
        final StringBuilder sb = new StringBuilder();
        final AtomicBoolean finishedWrites = new AtomicBoolean();

        if (socketChannel.connect(new InetSocketAddress(hostname, port))) {
            socketChannel.configureBlocking(false);

            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 100; i++) {
                            synchronized (finishedWrites) {
                                sb.append("SW: " + System.currentTimeMillis()).append("\n");
                            }
                            socketChannel.write(ByteBuffer.allocate(100000));
                            synchronized (finishedWrites) {
                                sb.append("EW: " + System.currentTimeMillis()).append("\n");
                            }
                        }
                    } catch (IOException e) {
                        synchronized (finishedWrites) {
                            sb.append("EEEE: " + System.currentTimeMillis()).append(e.getClass()).append("\n");
                        }
                    }

                    finishedWrites.set(true);
                    synchronized (finishedWrites) {
                        finishedWrites.notifyAll();
                    }
                }
            });

            Thread.sleep(10);
            synchronized (finishedWrites) {
                sb.append("SHUTDOWN: " + System.currentTimeMillis()).append("\n");
            }

            socketChannel.shutdownOutput(); // if we are lucky we will invoke this during a write

            synchronized (finishedWrites) {
                while (!finishedWrites.get()) {
                    finishedWrites.wait();
                }
            }
        }

        synchronized (finishedWrites) {
            System.out.println(sb);
        }
    }

    @Test
    public void verifySynchronizedWriteInvokedDuringConcurrentShutdownOutputDoesNotThrowAsynchronousCloseException() throws Exception {
        final SocketChannel socketChannel = secure ? SSLSocketChannel.open(buildRelaxedSSLContext()) : SocketChannel.open();
        final StringBuilder sb = new StringBuilder();
        final AtomicReference<IOException> ioe = new AtomicReference<>();
        final AtomicBoolean finishedWrites = new AtomicBoolean();

        if (socketChannel.connect(new InetSocketAddress(hostname, port))) {
            socketChannel.configureBlocking(false);

            es.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        for (int i = 0; i < 100; i++) {
                            synchronized (finishedWrites) {
                                sb.append("WW: " + System.currentTimeMillis()).append("\n");
                                socketChannel.write(ByteBuffer.wrap("sdfsdsdfsddfs".getBytes()));
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        ioe.set(e);
                    }

                    finishedWrites.set(true);
                    synchronized (finishedWrites) {
                        finishedWrites.notifyAll();
                    }
                }
            });

            //Thread.sleep(1);
            synchronized (finishedWrites) {
                sb.append("SD: " + System.currentTimeMillis()).append("\n");
                socketChannel.shutdownOutput();

                while (!finishedWrites.get()) {
                    finishedWrites.wait();
                }
            }

            if (ioe.get() != null) {
                if (!(ioe.get() instanceof ClosedChannelException)) {
                    throw ioe.get();
                }
            }
        }

        synchronized (finishedWrites) {
            System.out.println(sb);
        }
    }

    @Test(expected = AsynchronousCloseException.class)
    public void verifyConcurrentWriteInvokedDuringConcurrentShutdownOutputThrowsAsynchronousCloseException() throws Exception {
        for (int i = 0; i < 100; i++) { // we might get a ClosedChannelException as well, which is fine too ... loop
            final SocketChannel socketChannel = secure ? SSLSocketChannel.open(buildRelaxedSSLContext()) : SocketChannel.open();
            final StringBuilder sb = new StringBuilder();
            final AtomicReference<IOException> ioe = new AtomicReference<>();
            final AtomicBoolean finishedWrites = new AtomicBoolean();

            if (socketChannel.connect(new InetSocketAddress(hostname, port))) {
                socketChannel.configureBlocking(false);

                es.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            for (int i = 0; i < 100; i++) {
                                synchronized (finishedWrites) {
                                    sb.append("WW: " + System.currentTimeMillis()).append("\n");
                                }

                                socketChannel.write(ByteBuffer.wrap("sdfsdsdfsddfs".getBytes()));
                            }
                        } catch (IOException e) {
                            // e.printStackTrace();
                            ioe.set(e);
                        }

                        finishedWrites.set(true);
                        synchronized (finishedWrites) {
                            finishedWrites.notifyAll();
                        }
                    }
                });

                Thread.sleep(1);
                synchronized (finishedWrites) {
                    sb.append("SD: " + System.currentTimeMillis()).append("\n");
                }
                socketChannel.shutdownOutput();

                synchronized (finishedWrites) {
                    while (!finishedWrites.get()) {
                        finishedWrites.wait();
                    }
                }

                if (ioe.get() == null || !(ioe.get() instanceof AsynchronousCloseException)) {
                    continue;
                }

                throw ioe.get();
            }

            synchronized (finishedWrites) {
                System.out.println(sb);
            }
        }
    }

    public static SSLContext buildRelaxedSSLContext() throws Exception {
        TrustManager tm = new X509TrustManager() {
            @Override
            public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {
            }

            @Override
            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        };

        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{tm}, null);

        return sslContext;
    }
}
