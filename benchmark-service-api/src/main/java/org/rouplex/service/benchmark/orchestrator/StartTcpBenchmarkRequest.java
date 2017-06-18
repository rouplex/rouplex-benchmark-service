package org.rouplex.service.benchmark.orchestrator;

import org.rouplex.service.benchmark.worker.MetricsAggregation;
import org.rouplex.service.benchmark.worker.Provider;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartTcpBenchmarkRequest {
    String echoRatio; // not implemented yet
    String benchmarkRequestId; // optional
    HostType serverHostType; // optional
    GeoLocation serverGeoLocation; // optional
    HostType clientsHostType; // optional
    GeoLocation clientsGeoLocation; // optional
    String imageId; // optional
    String keyName; // optional

    int tcpMemoryAsPercentOfTotal; // optional, experimental for now

    Provider provider;
    int port;
    boolean ssl;
    int socketSendBufferSize; // optional
    int socketReceiveBufferSize; // optional
    int backlog; // optional

    public int clientCount = 100000;
    public int clientsPerHost = 50000;

    public int minPayloadSize;
    public int maxPayloadSize = 10000;
    public int minDelayMillisBetweenSends;
    public int maxDelayMillisBetweenSends = 1000;
    public int minDelayMillisBeforeCreatingClient;
    public int maxDelayMillisBeforeCreatingClient = 10000;
    public int minClientLifeMillis;
    public int maxClientLifeMillis = 10000;

    MetricsAggregation metricsAggregation = new MetricsAggregation();

    public String getEchoRatio() {
        return echoRatio;
    }

    public void setEchoRatio(String echoRatio) {
        this.echoRatio = echoRatio;
    }

    public String getBenchmarkRequestId() {
        return benchmarkRequestId;
    }

    public void setBenchmarkRequestId(String benchmarkRequestId) {
        this.benchmarkRequestId = benchmarkRequestId;
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

    public int getTcpMemoryAsPercentOfTotal() {
        return tcpMemoryAsPercentOfTotal;
    }

    public void setTcpMemoryAsPercentOfTotal(int tcpMemoryAsPercentOfTotal) {
        this.tcpMemoryAsPercentOfTotal = tcpMemoryAsPercentOfTotal;
    }
}
