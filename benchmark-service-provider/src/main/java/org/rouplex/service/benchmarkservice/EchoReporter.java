package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmarkservice.tcp.MetricsAggregation;
import org.rouplex.service.benchmarkservice.tcp.Request;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EchoReporter {
    private static final Logger logger = Logger.getLogger(BenchmarkServiceProvider.class.getSimpleName());
    public static final String format = "%s.%s.%s.%s:%s::%s:%s";
    // [Provider].[S,P].[EchoRequester,EchoResponder].[Server]:[Port]::[Client]:[Port]

    final MetricRegistry benchmarkerMetrics;
    final String actor;

    final String serverAddress;
    final String serverPort;
    final String clientAddress;
    final String clientPort;

    final Meter connected;
    final Timer connectionTime;
    final Meter connectionFailed;
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

    public EchoReporter(Request request, MetricRegistry benchmarkerMetrics, Class<?> clazz, RouplexTcpClient tcpClient) throws IOException {
        this.benchmarkerMetrics = benchmarkerMetrics;
        this.actor = clazz.getSimpleName();

        InetSocketAddress localIsa = (InetSocketAddress) tcpClient.getLocalAddress(true);
        clientAddress = localIsa.getAddress().getHostAddress().replace('.', '-');
        clientPort = "" + localIsa.getPort();

        InetSocketAddress remoteIsa = (InetSocketAddress) tcpClient.getRemoteAddress(true);
        serverAddress = remoteIsa.getAddress().getHostAddress().replace('.', '-');
        serverPort = "" + remoteIsa.getPort();

        String secureTag = request.isSsl() ? "S" : "P";
        completeId = String.format(format,
                request.getProvider(),
                secureTag,
                actor,
                serverAddress,
                serverPort,
                clientAddress,
                clientPort
        );

        MetricsAggregation ma = request.getMetricsAggregation();
        aggregatedId = String.format(format,
                request.getProvider(),
                ma.isAggregateSslWithPlain() ? "A" : secureTag,
                actor,
                ma.isAggregateServerAddresses() ? "A" : serverAddress,
                ma.isAggregateServerPorts() ? "A" : serverPort,
                ma.isAggregateClientAddresses() ? "A" : clientAddress,
                ma.isAggregateClientPorts() ? "A" : clientPort
        );

        if (benchmarkerMetrics != null) {
            connected = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connected"));
            connectionTime = benchmarkerMetrics.timer(MetricRegistry.name(aggregatedId, "connectTime"));
            connectionFailed = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connectionFailed"));
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
            connected = connectionFailed = disconnectedOk = disconnectedKo = sentBytes = sentEos =
                    sendFailures = discardedSendBytes = receivedBytes = receivedEos = receivedDisconnect = null;
            receivedSizes = sentSizes = sendBufferFilled = null;
            connectionTime = sendPauseTime = null;
        }
    }

    public String getAggregatedId() {
        return aggregatedId;
    }
}