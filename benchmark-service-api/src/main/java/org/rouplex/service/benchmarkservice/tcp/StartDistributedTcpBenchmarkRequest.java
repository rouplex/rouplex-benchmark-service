package org.rouplex.service.benchmarkservice.tcp;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartDistributedTcpBenchmarkRequest {
    String benchmarkRequestId;
    String serverInstanceType = "t2.micro";
    String clientInstanceType = "t2.micro";
    String keyName;

    Provider provider;
    int port;
    boolean ssl;
    int socketSendBufferSize;
    int socketReceiveBufferSize;
    int backlog;

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

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getBenchmarkRequestId() {
        return benchmarkRequestId;
    }

    public void setBenchmarkRequestId(String benchmarkRequestId) {
        this.benchmarkRequestId = benchmarkRequestId;
    }

    public String getServerInstanceType() {
        return serverInstanceType;
    }

    public void setServerInstanceType(String serverInstanceType) {
        this.serverInstanceType = serverInstanceType;
    }

    public String getClientInstanceType() {
        return clientInstanceType;
    }

    public void setClientInstanceType(String clientInstanceType) {
        this.clientInstanceType = clientInstanceType;
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
}
