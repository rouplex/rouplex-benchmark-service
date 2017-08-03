package org.rouplex.service.benchmark.orchestrator;

import org.rouplex.service.deployment.GeoLocation;
import org.rouplex.service.deployment.Host;
import org.rouplex.service.deployment.HostType;

import java.util.Collection;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class TcpEchoBenchmark {
    private String id;
    private HostType serverHostType;
    private GeoLocation serverGeoLocation;
    private HostType clientsHostType;
    private GeoLocation clientsGeoLocation;
    private String imageId;
    private String keyName; // optional
    private String echoRatio; // not implemented yet
    private int tcpMemoryAsPercentOfTotal; // optional, experimental for now

    private Provider provider;
    private int port;
    private boolean ssl;
    private int socketSendBufferSize; // optional
    private int socketReceiveBufferSize; // optional
    private int backlog; // optional

    private int clientCount;
    private int clientsPerHost;

    private int minPayloadSize;
    private int maxPayloadSize;
    private int minDelayMillisBetweenSends;
    private int maxDelayMillisBetweenSends;
    private int minDelayMillisBeforeCreatingClient;
    private int maxDelayMillisBeforeCreatingClient;
    private int minClientLifeMillis;
    private int maxClientLifeMillis;

    private MetricsAggregation metricsAggregation;
    private TcpMetricsExpectation tcpServerExpectation;
    private TcpMetricsExpectation tcpClientsExpectation;

    private ExecutionStatus executionStatus;
    private String jconsoleJmxLink;
    private String exception;

    private long startingTimestamp;
    private long expectedDurationMillis;
    private long startedTimestamp;

    private Host serverHost;
    private Collection<? extends Host> clientHosts;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public HostType getServerHostType() {
        return serverHostType;
    }

    public void setServerHostType(HostType serverHostType) {
        this.serverHostType = serverHostType;
    }

    public GeoLocation getServerGeoLocation() {
        return serverGeoLocation;
    }

    public void setServerGeoLocation(GeoLocation serverGeoLocation) {
        this.serverGeoLocation = serverGeoLocation;
    }

    public HostType getClientsHostType() {
        return clientsHostType;
    }

    public void setClientsHostType(HostType clientsHostType) {
        this.clientsHostType = clientsHostType;
    }

    public GeoLocation getClientsGeoLocation() {
        return clientsGeoLocation;
    }

    public void setClientsGeoLocation(GeoLocation clientsGeoLocation) {
        this.clientsGeoLocation = clientsGeoLocation;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getEchoRatio() {
        return echoRatio;
    }

    public void setEchoRatio(String echoRatio) {
        this.echoRatio = echoRatio;
    }

    public int getTcpMemoryAsPercentOfTotal() {
        return tcpMemoryAsPercentOfTotal;
    }

    public void setTcpMemoryAsPercentOfTotal(int tcpMemoryAsPercentOfTotal) {
        this.tcpMemoryAsPercentOfTotal = tcpMemoryAsPercentOfTotal;
    }

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public void setSsl(boolean ssl) {
        this.ssl = ssl;
    }

    public int getSocketSendBufferSize() {
        return socketSendBufferSize;
    }

    public void setSocketSendBufferSize(int socketSendBufferSize) {
        this.socketSendBufferSize = socketSendBufferSize;
    }

    public int getSocketReceiveBufferSize() {
        return socketReceiveBufferSize;
    }

    public void setSocketReceiveBufferSize(int socketReceiveBufferSize) {
        this.socketReceiveBufferSize = socketReceiveBufferSize;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public int getClientCount() {
        return clientCount;
    }

    public void setClientCount(int clientCount) {
        this.clientCount = clientCount;
    }

    public int getClientsPerHost() {
        return clientsPerHost;
    }

    public void setClientsPerHost(int clientsPerHost) {
        this.clientsPerHost = clientsPerHost;
    }

    public int getMinPayloadSize() {
        return minPayloadSize;
    }

    public void setMinPayloadSize(int minPayloadSize) {
        this.minPayloadSize = minPayloadSize;
    }

    public int getMaxPayloadSize() {
        return maxPayloadSize;
    }

    public void setMaxPayloadSize(int maxPayloadSize) {
        this.maxPayloadSize = maxPayloadSize;
    }

    public int getMinDelayMillisBetweenSends() {
        return minDelayMillisBetweenSends;
    }

    public void setMinDelayMillisBetweenSends(int minDelayMillisBetweenSends) {
        this.minDelayMillisBetweenSends = minDelayMillisBetweenSends;
    }

    public int getMaxDelayMillisBetweenSends() {
        return maxDelayMillisBetweenSends;
    }

    public void setMaxDelayMillisBetweenSends(int maxDelayMillisBetweenSends) {
        this.maxDelayMillisBetweenSends = maxDelayMillisBetweenSends;
    }

    public int getMinDelayMillisBeforeCreatingClient() {
        return minDelayMillisBeforeCreatingClient;
    }

    public void setMinDelayMillisBeforeCreatingClient(int minDelayMillisBeforeCreatingClient) {
        this.minDelayMillisBeforeCreatingClient = minDelayMillisBeforeCreatingClient;
    }

    public int getMaxDelayMillisBeforeCreatingClient() {
        return maxDelayMillisBeforeCreatingClient;
    }

    public void setMaxDelayMillisBeforeCreatingClient(int maxDelayMillisBeforeCreatingClient) {
        this.maxDelayMillisBeforeCreatingClient = maxDelayMillisBeforeCreatingClient;
    }

    public int getMinClientLifeMillis() {
        return minClientLifeMillis;
    }

    public void setMinClientLifeMillis(int minClientLifeMillis) {
        this.minClientLifeMillis = minClientLifeMillis;
    }

    public int getMaxClientLifeMillis() {
        return maxClientLifeMillis;
    }

    public void setMaxClientLifeMillis(int maxClientLifeMillis) {
        this.maxClientLifeMillis = maxClientLifeMillis;
    }

    public MetricsAggregation getMetricsAggregation() {
        return metricsAggregation;
    }

    public void setMetricsAggregation(MetricsAggregation metricsAggregation) {
        this.metricsAggregation = metricsAggregation;
    }

    public TcpMetricsExpectation getTcpServerExpectation() {
        return tcpServerExpectation;
    }

    public void setTcpServerExpectation(TcpMetricsExpectation tcpServerExpectation) {
        this.tcpServerExpectation = tcpServerExpectation;
    }

    public TcpMetricsExpectation getTcpClientsExpectation() {
        return tcpClientsExpectation;
    }

    public void setTcpClientsExpectation(TcpMetricsExpectation tcpClientsExpectation) {
        this.tcpClientsExpectation = tcpClientsExpectation;
    }

    public ExecutionStatus getExecutionStatus() {
        return executionStatus;
    }

    public void setExecutionStatus(ExecutionStatus executionStatus) {
        this.executionStatus = executionStatus;
    }

    public String getJconsoleJmxLink() {
        return jconsoleJmxLink;
    }

    public void setJconsoleJmxLink(String jconsoleJmxLink) {
        this.jconsoleJmxLink = jconsoleJmxLink;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public long getStartingTimestamp() {
        return startingTimestamp;
    }

    public void setStartingTimestamp(long startingTimestamp) {
        this.startingTimestamp = startingTimestamp;
    }

    public long getExpectedDurationMillis() {
        return expectedDurationMillis;
    }

    public void setExpectedDurationMillis(long expectedDurationMillis) {
        this.expectedDurationMillis = expectedDurationMillis;
    }

    public long getStartedTimestamp() {
        return startedTimestamp;
    }

    public void setStartedTimestamp(long startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }

    public Host getServerHost() {
        return serverHost;
    }

    public void setServerHost(Host serverHost) {
        this.serverHost = serverHost;
    }

    public Collection<? extends Host> getClientHosts() {
        return clientHosts;
    }

    public void setClientHosts(Collection<? extends Host> clientHosts) {
        this.clientHosts = clientHosts;
    }
}
