package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmark.orchestrator.MetricsAggregation;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
class EchoReporter {
    public static final String format = "%s.%s.%s.%s:%s::%s:%s";
    // [Provider].[S,P].[EchoRequester,EchoResponder].[Server]:[Port]::[Client]:[Port]

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

    final Meter receivedBytes;
    final Meter receivedEos;
    final Meter receivedDisconnect;
    final Histogram receivedSizes;

    private final String aggregatedId;

    final MetricRegistry metricRegistry;

    public EchoReporter(CreateTcpEndPointRequest createTcpEndPointRequest, MetricRegistry metricRegistry,
                        Class<?> clazz, RouplexTcpClient tcpClient) throws IOException {

        this.metricRegistry = metricRegistry;

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

        if (metricRegistry != null) {
            connectionEstablished = metricRegistry.meter(MetricRegistry.name(aggregatedId, "connection.established"));
            liveConnections = metricRegistry.meter(MetricRegistry.name(aggregatedId, "connection.live"));

            connectionTime = metricRegistry.timer(MetricRegistry.name(aggregatedId, "connectionTime"));
            disconnectedOk = metricRegistry.meter(MetricRegistry.name(aggregatedId, "disconnectedOk"));
            disconnectedKo = metricRegistry.meter(MetricRegistry.name(aggregatedId, "disconnectedKo"));

            sentBytes = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sentBytes"));
            sentEos = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sentEos"));
            sendFailures = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sendFailures"));
            sentSizes = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "sentSizes"));
            sendBufferFilled = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "sendBufferFilled"));
            discardedSendBytes = metricRegistry.meter(MetricRegistry.name(aggregatedId, "discardedSendBytes"));

            receivedBytes = metricRegistry.meter(MetricRegistry.name(aggregatedId, "receivedBytes"));
            receivedEos = metricRegistry.meter(MetricRegistry.name(aggregatedId, "receivedEos"));
            receivedDisconnect = metricRegistry.meter(MetricRegistry.name(aggregatedId, "receivedDisconnect"));
            receivedSizes = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "receivedSizes"));
        } else {
            connectionEstablished = liveConnections = disconnectedOk = disconnectedKo = sentBytes = sentEos =
                sendFailures = discardedSendBytes = receivedBytes = receivedEos = receivedDisconnect = null;
            receivedSizes = sentSizes = sendBufferFilled = null;
            connectionTime = null;
        }
    }

    public String getAggregatedId() {
        return aggregatedId;
    }
}