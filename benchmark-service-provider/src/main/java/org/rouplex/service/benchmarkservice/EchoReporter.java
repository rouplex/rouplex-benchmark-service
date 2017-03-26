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
    public static final String format = "%s.%s.%s.%s:%s::%s:%s.%s";
    // [N,C].[S,P].[EchoRequester,EchoResponder].[Server]:[Port]::[Client]:[Port].[ClientId]

    final Request request;
    final MetricRegistry benchmarkerMetrics;
    final String actor;

    String serverAddress;
    String serverPort;
    String clientAddress = "A";
    String clientPort = "A";
    String clientId = "A";

    Meter connecting;
    Meter unconnected;
    Meter connected;
    Meter disconnected;

    Meter sentBytes;
    Meter sentEos;
    Meter sendFailures;
    Histogram sentSizes;
    Histogram sendBufferFilled;
    Meter discardedSendBytes;
    Timer sendPauseTime;

    Meter receivedBytes;
    Meter receivedEos;
    Meter receivedDisconnect;
    Histogram receivedSizes;

    String aggregatedId;
    String completeId;

    public EchoReporter(Request request, MetricRegistry benchmarkerMetrics, Class<?> clazz) {
        this.request = request;
        this.benchmarkerMetrics = benchmarkerMetrics;
        actor = clazz.getSimpleName();

        serverAddress = request.getHostname() == null ? "A" : request.getHostname().replace('.', '-');
        serverPort = "" + request.getPort();

        updateId();

        if (benchmarkerMetrics != null) {
            connecting = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connecting"));
            unconnected = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "unconnected"));
            connected = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "connected"));
            disconnected = benchmarkerMetrics.meter(MetricRegistry.name(aggregatedId, "disconnected"));
        }
    }

    public void setTcpClient(RouplexTcpClient tcpClient) throws IOException {
        InetSocketAddress localIsa = (InetSocketAddress) tcpClient.getLocalAddress();
        clientAddress = localIsa.getAddress().getHostAddress().replace('.', '-');
        clientPort = "" + localIsa.getPort();

        InetSocketAddress remoteIsa = (InetSocketAddress) tcpClient.getRemoteAddress();
        serverAddress = remoteIsa.getAddress().getHostAddress().replace('.', '-');
        serverPort = "" + remoteIsa.getPort();
    }

    public void setClientId(int clientId) {
        this.clientId = String.format("%010d", clientId);
        updateId();

        if (benchmarkerMetrics != null) {
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
        }
    }

    void connecting() {
        connecting.mark();
        logger.info(String.format("Connecting %s", completeId));
    }

    void unconnected(Exception e) {
        unconnected.mark();
        logger.info(String.format("Unconnected %s. Cause: %s %s", completeId, e.getClass(), e.getMessage()));
    }

    void connected() {
        connected.mark();
        logger.info(String.format("Connected %s", completeId));
    }

    void disconnected() {
        disconnected.mark();
        logger.info(String.format("Disconnected %s", completeId));
    }

    private void updateId() {
        String secureTag = request.isSsl() ? "S" : "P";
        completeId = String.format(format,
                request.isUseNiossl() ? "N" : "C",
                secureTag,
                actor,
                serverAddress,
                serverPort,
                clientAddress,
                clientPort,
                clientId
        );

        MetricsAggregation ma = request.getMetricsAggregation();
        aggregatedId = String.format(format,
                request.isUseNiossl() ? "N" : "C",
                ma.isAggregateSslWithPlain() ? "A" : secureTag,
                actor,
                ma.isAggregateServerAddresses() ? "A" : serverAddress,
                ma.isAggregateServerPorts() ? "A" : serverPort,
                ma.isAggregateClientAddresses() ? "A" : clientAddress,
                ma.isAggregateClientPorts() ? "A" : clientPort,
                ma.isAggregateClientIds() ? "A" : clientId
        );
    }

    public String getAggregatedId() {
        return aggregatedId;
    }
}