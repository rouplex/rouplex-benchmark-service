package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmark.orchestrator.MetricsAggregation;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoReporter {
    private static final Logger logger = Logger.getLogger(WorkerServiceProvider.class.getSimpleName());
    public static final String format = "%s.%s.%s.%s:%s::%s:%s";
    // [Provider].[S,P].[EchoRequester,EchoResponder].[Server]:[Port]::[Client]:[Port]

    final Meter connectionEstablished;
    final Meter liveConnections;
    final Timer connectionTime;
    final ConcurrentMap<Integer, Meter> connectionTimes = new ConcurrentHashMap<>();
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

    private final String aggregatedId;

    public EchoReporter(CreateTcpEndPointRequest createTcpEndPointRequest, MetricRegistry benchmarkerMetrics,
                        Class<?> clazz, RouplexTcpClient tcpClient) throws IOException {

        InetSocketAddress localIsa = (InetSocketAddress) tcpClient.getLocalAddress();
        String clientAddress = localIsa.getAddress().getHostAddress().replace('.', '-');
        String clientPort = "" + localIsa.getPort();

        InetSocketAddress remoteIsa = (InetSocketAddress) tcpClient.getRemoteAddress();
        String serverAddress = remoteIsa.getAddress().getHostAddress().replace('.', '-');
        String serverPort = "" + remoteIsa.getPort();

        String secureTag = createTcpEndPointRequest.isSsl() ? "S" : "P";

        MetricsAggregation ma = createTcpEndPointRequest.getMetricsAggregation();
        aggregatedId = String.format(format,
            createTcpEndPointRequest.getProvider(),
            ma.isAggregateSslWithPlain() ? "A" : secureTag,
            clazz.getSimpleName(),
            ma.isAggregateServerAddresses() ? "A" : serverAddress,
            ma.isAggregateServerPorts() ? "A" : serverPort,
            ma.isAggregateClientAddresses() ? "A" : clientAddress,
            ma.isAggregateClientPorts() ? "A" : clientPort
        );

        if (benchmarkerMetrics != null) {
            for (int bucket = 1; bucket < 7; bucket++) {
                connectionTimes.put(bucket, benchmarkerMetrics.meter(MetricRegistry.name(
                    aggregatedId, "connectionTime.millisBucket", "100000".substring(0, bucket))));
            }

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