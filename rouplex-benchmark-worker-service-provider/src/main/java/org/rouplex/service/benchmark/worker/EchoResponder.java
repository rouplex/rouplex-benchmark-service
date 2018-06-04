package org.rouplex.service.benchmark.worker;

import org.rouplex.platform.tcp.TcpClient;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoResponder {
    private static final Logger logger = Logger.getLogger(EchoResponder.class.getSimpleName());

    EchoResponder(CreateTcpServerRequest createTcpServerRequest, RouplexWorkerServiceProvider benchmarkServiceProvider,
                  TcpClient tcpClient) throws IOException {

        EchoReporter echoReporter = new EchoReporter(createTcpServerRequest, benchmarkServiceProvider.benchmarkerMetrics,
            "EchoResponder", tcpClient.getLocalAddress(), tcpClient.getRemoteAddress());

        echoReporter.clientConnectionEstablished.mark();
        echoReporter.clientConnectionLive.mark();

        Reader reader = new Reader(tcpClient.getReadChannel(), echoReporter,
            new Writer(tcpClient.getWriteChannel(), echoReporter, true));

        tcpClient.setAttachment(reader);
        reader.run();

        logger.info("Created EchoResponder");
    }
}
