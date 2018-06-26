package org.rouplex.service.benchmark.worker;

import org.rouplex.platform.tcp.TcpClient;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoResponder {
    private static final Logger logger = Logger.getLogger(EchoResponder.class.getSimpleName());

    EchoResponder(CreateTcpServerRequest request, RouplexWorkerServiceProvider benchmarkServiceProvider,
                  TcpClient tcpClient) throws IOException {

        EchoReporter echoReporter = new EchoReporter(request, benchmarkServiceProvider.benchmarkerMetrics,
            "EchoResponder", tcpClient.getLocalAddress(), tcpClient.getRemoteAddress());

        echoReporter.clientConnectionEstablished.mark();
        echoReporter.clientConnectionLive.mark();

        boolean echo;
        if (request.getEchoRatio() == null) {
            echo = true;
        } else {
            String[] echoRatios = request.getEchoRatio().split(":");
            echo = echoRatios.length != 2 || !"0".equals(echoRatios[1]);
        }

        Writer writer = echo ? new Writer(tcpClient.getWriteChannel(), echoReporter, true) : null;

        Reader reader = new Reader(tcpClient.getReadChannel(), echoReporter, writer);
        tcpClient.setAttachment(reader);
        reader.run();

        logger.info("Created EchoResponder");
    }
}
