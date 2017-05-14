package org.rouplex.service.benchmark.orchestrator;

import org.rouplex.service.benchmark.worker.MetricsAggregation;
import org.rouplex.service.benchmark.worker.Provider;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartDistributedTcpBenchmarkRequest {
    String magicWord;
    String optionalBenchmarkRequestId;
    HostType optionalServerHostType;
    GeoLocation optionalServerGeoLocation;
    HostType optionalClientsHostType;
    GeoLocation optionalClientsGeoLocation;
    String optionalImageId;
    String optionalKeyName;

    Provider provider;
    int port;
    boolean ssl;
    int optionalSocketSendBufferSize;
    int optionalSocketReceiveBufferSize;
    int optionalBacklog;

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

    public String getMagicWord() {
        return magicWord;
    }

    public void setMagicWord(String magicWord) {
        this.magicWord = magicWord;
    }

    public String getOptionalBenchmarkRequestId() {
        return optionalBenchmarkRequestId;
    }

    public void setOptionalBenchmarkRequestId(String optionalBenchmarkRequestId) {
        this.optionalBenchmarkRequestId = optionalBenchmarkRequestId;
    }

    public HostType getOptionalServerHostType() {
        return optionalServerHostType;
    }

    public void setOptionalServerHostType(HostType optionalServerHostType) {
        this.optionalServerHostType = optionalServerHostType;
    }

    public GeoLocation getOptionalServerGeoLocation() {
        return optionalServerGeoLocation;
    }

    public void setOptionalServerGeoLocation(GeoLocation optionalServerGeoLocation) {
        this.optionalServerGeoLocation = optionalServerGeoLocation;
    }

    public HostType getOptionalClientsHostType() {
        return optionalClientsHostType;
    }

    public void setOptionalClientsHostType(HostType optionalClientsHostType) {
        this.optionalClientsHostType = optionalClientsHostType;
    }

    public GeoLocation getOptionalClientsGeoLocation() {
        return optionalClientsGeoLocation;
    }

    public void setOptionalClientsGeoLocation(GeoLocation optionalClientsGeoLocation) {
        this.optionalClientsGeoLocation = optionalClientsGeoLocation;
    }

    public String getOptionalImageId() {
        return optionalImageId;
    }

    public void setOptionalImageId(String optionalImageId) {
        this.optionalImageId = optionalImageId;
    }

    public String getOptionalKeyName() {
        return optionalKeyName;
    }

    public void setOptionalKeyName(String optionalKeyName) {
        this.optionalKeyName = optionalKeyName;
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

    public int getOptionalSocketSendBufferSize() {
        return optionalSocketSendBufferSize;
    }

    public void setOptionalSocketSendBufferSize(int optionalSocketSendBufferSize) {
        this.optionalSocketSendBufferSize = optionalSocketSendBufferSize;
    }

    public int getOptionalSocketReceiveBufferSize() {
        return optionalSocketReceiveBufferSize;
    }

    public void setOptionalSocketReceiveBufferSize(int optionalSocketReceiveBufferSize) {
        this.optionalSocketReceiveBufferSize = optionalSocketReceiveBufferSize;
    }

    public int getOptionalBacklog() {
        return optionalBacklog;
    }

    public void setOptionalBacklog(int optionalBacklog) {
        this.optionalBacklog = optionalBacklog;
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
