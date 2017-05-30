package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.*;
import org.rouplex.platform.tcp.RouplexTcpClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoReporter {
    private static final Logger logger = Logger.getLogger(BenchmarkWorkerServiceProvider.class.getSimpleName());
    public static final String format = "%s.%s.%s.%s:%s::%s:%s";
    // [Provider].[S,P].[EchoRequester,EchoResponder].[Server]:[Port]::[Client]:[Port]

    final MetricRegistry benchmarkerMetrics;
    final String actor;

    final String serverAddress;
    final String serverPort;
    final String clientAddress;
    final String clientPort;

    final Meter connectionEstablished;
    final Meter liveConnections;
    final Timer connectionTime;
    final Meter disconnectedOk;
    final Meter disconnectedKo;

    final Meter sentBytes;
    final Meter sentEos;
    final Meter sendFailures;
    final Histogram sentSizes;
    final Histogram sendBufferFilled;
    final Meter discardedSendBytes;
    final Timer sendPauseTime;

    final Meter receivedBytes;
    final Meter receivedEos;
    final Meter receivedDisconnect;
    final Histogram receivedSizes;

    final String aggregatedId;
    final String completeId;

    public EchoReporter(StartTcpEndPointRequest startTcpEndPointRequest, MetricRegistry benchmarkerMetrics,
                        Class<?> clazz, RouplexTcpClient tcpClient) throws IOException {

        this.benchmarkerMetrics = benchmarkerMetrics;
        this.actor = clazz.getSimpleName();

        InetSocketAddress localIsa = (InetSocketAddress) tcpClient.getLocalAddress();
        clientAddress = localIsa.getAddress().getHostAddress().replace('.', '-');
        clientPort = "" + localIsa.getPort();

        InetSocketAddress remoteIsa = (InetSocketAddress) tcpClient.getRemoteAddress();
        serverAddress = remoteIsa.getAddress().getHostAddress().replace('.', '-');
        serverPort = "" + remoteIsa.getPort();

        String secureTag = startTcpEndPointRequest.isSsl() ? "S" : "P";
        completeId = String.format(format,
                startTcpEndPointRequest.getProvider(),
                secureTag,
                actor,
                serverAddress,
                serverPort,
                clientAddress,
                clientPort
        );

        MetricsAggregation ma = startTcpEndPointRequest.getMetricsAggregation();
        aggregatedId = String.format(format,
                startTcpEndPointRequest.getProvider(),
                ma.isAggregateSslWithPlain() ? "A" : secureTag,
                actor,
                ma.isAggregateServerAddresses() ? "A" : serverAddress,
                ma.isAggregateServerPorts() ? "A" : serverPort,
                ma.isAggregateClientAddresses() ? "A" : clientAddress,
                ma.isAggregateClientPorts() ? "A" : clientPort
        );

        if (benchmarkerMetrics != null) {
            connectionEstablished = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connection.established"));
            liveConnections = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connection.live"));

            connectionTime = benchmarkerMetrics.timer(MetricRegistry.name(aggregatedId, "connectionTime"));
            disconnectedOk = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "disconnectedOk"));
            disconnectedKo = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "disconnectedKo"));

            sentBytes = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "sentBytes"));
            sentEos = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "sentEos"));
            sendFailures = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "sendFailures"));
            sentSizes = benchmarkerMetrics.histogram(MetricRegistry.name(aggregatedId, "sentSizes"));
            sendBufferFilled = benchmarkerMetrics.histogram(MetricRegistry.name(aggregatedId, "sendBufferFilled"));
            discardedSendBytes = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "discardedSendBytes"));
            sendPauseTime = benchmarkerMetrics.timer(MetricRegistry.name(aggregatedId, "sendPauseTime"));

            receivedBytes = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "receivedBytes"));
            receivedEos = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "receivedEos"));
            receivedDisconnect = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "receivedDisconnect"));
            receivedSizes = benchmarkerMetrics.histogram(MetricRegistry.name(aggregatedId, "receivedSizes"));
        } else {
            connectionEstablished = liveConnections = disconnectedOk = disconnectedKo = sentBytes = sentEos =
                    sendFailures = discardedSendBytes = receivedBytes = receivedEos = receivedDisconnect = null;
            receivedSizes = sentSizes = sendBufferFilled = null;
            connectionTime = sendPauseTime = null;
        }
    }

    public String getAggregatedId() {
        return aggregatedId;
    }
}