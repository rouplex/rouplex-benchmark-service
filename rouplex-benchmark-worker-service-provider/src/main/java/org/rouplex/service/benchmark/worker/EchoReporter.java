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
    final Timer clientConnectionTimes;
    final Meter clientClosed;
    final Meter clientDisconnectedOk;
    final Meter clientDisconnectedKo;

    final Meter wcWrittenByte;
    final Meter wcWritten0Byte;
    final Meter wcWrittenEos;
    final Meter wcWriteFailure;
    final Meter wcDiscardedByteAtNic;
    final Meter wcDiscardedByteAtCpu;
    final Histogram wcWrittenSizes;
    final Timer wcWriteTimes;

    final Meter rcReadByte;
    final Meter rcRead0Byte;
    final Meter rcReadEos;
    final Meter rcReadFailure;
    final Histogram rcReadSizes;

    private final String aggregatedId;

    final MetricRegistry metricRegistry;

    public EchoReporter(CreateTcpEndPointRequest createTcpEndPointRequest, MetricRegistry metricRegistry,
                        String className, SocketAddress localAddress, SocketAddress remoteAddress) {

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
            className,
            ma.isAggregateServerAddresses() ? "A" : serverAddress,
            ma.isAggregateServerPorts() ? "A" : serverPort,
            ma.isAggregateClientAddresses() ? "A" : clientAddress,
            ma.isAggregateClientPorts() ? "A" : clientPort
        );

        if (metricRegistry != null) {
            clientConnectionEstablished = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientConnectionEstablished"));
            clientConnectionFailed = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientConnectionFailed"));
            clientConnectionLive = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientConnectionLive"));

            clientConnectionTimes = metricRegistry.timer(MetricRegistry.name(aggregatedId, "clientConnectionTimes"));
            clientClosed = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientClosed"));
            clientDisconnectedOk = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientDisconnectedOk"));
            clientDisconnectedKo = metricRegistry.meter(MetricRegistry.name(aggregatedId, "clientDisconnectedKo"));

            wcWrittenByte = metricRegistry.meter(MetricRegistry.name(aggregatedId, "wcWrittenByte"));
            wcWritten0Byte = metricRegistry.meter(MetricRegistry.name(aggregatedId, "wcWritten0Byte"));
            wcWrittenEos = metricRegistry.meter(MetricRegistry.name(aggregatedId, "wcWrittenEos"));
            wcWriteFailure = metricRegistry.meter(MetricRegistry.name(aggregatedId, "wcWriteFailure"));
            wcWrittenSizes = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "wcWrittenSizes"));
            wcDiscardedByteAtNic = metricRegistry.meter(MetricRegistry.name(aggregatedId, "wcDiscardedByteAtNic"));
            wcWriteTimes = metricRegistry.timer(MetricRegistry.name(aggregatedId, "wcWriteTimes"));
            wcDiscardedByteAtCpu = metricRegistry.meter(MetricRegistry.name(aggregatedId, "wcDiscardedByteAtCpu"));

            rcReadByte = metricRegistry.meter(MetricRegistry.name(aggregatedId, "rcReadByte"));
            rcRead0Byte = metricRegistry.meter(MetricRegistry.name(aggregatedId, "rcRead0Byte"));
            rcReadEos = metricRegistry.meter(MetricRegistry.name(aggregatedId, "rcReadEos"));
            rcReadFailure = metricRegistry.meter(MetricRegistry.name(aggregatedId, "rcReadFailure"));
            rcReadSizes = metricRegistry.histogram(MetricRegistry.name(aggregatedId, "rcReadSizes"));
        } else {
            throw new IllegalArgumentException("metricRegistry is null");

//            clientConnectionEstablished = clientConnectionFailed = clientConnectionLive = clientClosed =
//                clientDisconnectedOk = clientDisconnectedKo = wcWrittenByte = wcWritten0Byte = wcWrittenEos = wcWriteFailure = wcDiscardedByteAtNic =
//            rcReadByte = rcRead0Byte = rcReadEos = rcReadFailure = null;
//            rcReadSizes = wcWrittenSizes = null;
//            clientConnectionTimes = null;
        }
    }

    public String getAggregatedId() {
        return aggregatedId;
    }
}