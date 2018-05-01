package org.rouplex.service.benchmark.worker;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.rouplex.service.benchmark.orchestrator.MetricsAggregation;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
class EchoReporter {
    public static final String format = "%s.%s.%s.%s:%s::%s:%s";
    // [Provider].[S,P].[EchoRequester,EchoResponder].[Server]:[Port]::[Client]:[Port]

    final Meter clientConnectionEstablished;
    final Meter clientConnectionFailed;
    final Meter clientConnectionLive;
    final Timer clientConnectionTime;
    final Meter clientClosed;
    final Meter clientDisconnectedOk;
    final Meter clientDisconnectedKo;

    final Meter sentBytes;
    final Meter sentEos;
    final Meter sendFailures;
    final Histogram sentSizes;
    final Meter sentBytesDiscarded;

    final Meter receivedBytes;
    final Meter receivedEos;
    final Meter receivedFailures;
    final Histogram receivedSizes;

    private final String aggregatedId;

    final MetricRegistry metricRegistry;

    public EchoReporter(CreateTcpEndPointRequest createTcpEndPointRequest, MetricRegistry metricRegistry,
                        Class<?> clazz, SocketAddress localAddress, SocketAddress remoteAddress) {

        this.metricRegistry = metricRegistry;

        String clientAddress;
        String clientPort;
        try {
            clientAddress = ((InetSocketAddress) localAddress).getAddress().getHostAddress().replace('.', '-');
            clientPort = "" + ((InetSocketAddress) localAddress).getPort();
        } catch (RuntimeException re) { // NPE included
            clientAddress = clientPort = "A";
        }

        String serverAddress;
        String serverPort;
        try {
            serverAddress = ((InetSocketAddress) remoteAddress).getAddress().getHostAddress().replace('.', '-');
            serverPort = "" + ((InetSocketAddress) remoteAddress).getPort();
        } catch (RuntimeException re) { // NPE included
            serverAddress = serverPort = "A";
        }

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
            clientConnectionEstablished = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientConnectionEstablished"));
            clientConnectionFailed = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientConnectionFailed"));
            clientConnectionLive = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientConnectionLive"));

            clientConnectionTime = metricRegistry.timer(MetricRegistry.name(aggregatedId, "clientConnectionTime"));
            clientClosed = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientClosed"));
            clientDisconnectedOk = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientDisconnectedOk"));
            clientDisconnectedKo = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientDisconnectedKo"));

            sentBytes = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sentBytes"));
            sentEos = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sentEos"));
            sendFailures = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sendFailures"));
            sentSizes = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "sentSizes"));
            sentBytesDiscarded = metricRegistry.meter(MetricRegistry.name(aggregatedId, "sentBytesDiscarded"));

            receivedBytes = metricRegistry.meter(MetricRegistry.name(aggregatedId, "receivedBytes"));
            receivedEos = metricRegistry.meter(MetricRegistry.name(aggregatedId, "receivedEos"));
            receivedFailures = metricRegistry.meter(MetricRegistry.name(aggregatedId, "receivedFailures"));
            receivedSizes = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "receivedSizes"));
        } else {
            clientConnectionEstablished = clientConnectionFailed = clientConnectionLive = clientClosed =
                clientDisconnectedOk = clientDisconnectedKo = sentBytes = sentEos = sendFailures = sentBytesDiscarded =
            receivedBytes = receivedEos = receivedFailures = null;
            receivedSizes = sentSizes = null;
            clientConnectionTime = null;
        }
    }

    public String getAggregatedId() {
        return aggregatedId;
    }
}