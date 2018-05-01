package org.rouplex.service.benchmark.worker;

import org.rouplex.platform.tcp.TcpClient;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoResponder {
    private static final Logger logger = Logger.getLogger(EchoResponder.class.getSimpleName());

    final EchoReporter echoReporter;
    Integer clientId;

    EchoResponder(CreateTcpServerRequest createTcpServerRequest, RouplexWorkerServiceProvider benchmarkServiceProvider,
                  TcpClient connectedTcpClient) throws IOException {

        connectedTcpClient.setAttachment(this);

        echoReporter = new EchoReporter(createTcpServerRequest, benchmarkServiceProvider.benchmarkerMetrics,
            EchoResponder.class, connectedTcpClient.getLocalAddress(), connectedTcpClient.getRemoteAddress());

        echoReporter.clientConnectionEstablished.mark();
        echoReporter.clientConnectionLive.mark();

        new Reader(connectedTcpClient.getReadChannel(), 10000, echoReporter,
            new Writer(connectedTcpClient.getWriteChannel(), 10000, echoReporter, true)).run();

        logger.info("Created EchoResponder");
    }

}
