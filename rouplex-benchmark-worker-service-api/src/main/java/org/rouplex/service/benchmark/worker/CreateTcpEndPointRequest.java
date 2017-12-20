package org.rouplex.service.benchmark.worker;

import org.rouplex.service.benchmark.orchestrator.MetricsAggregation;
import org.rouplex.service.benchmark.orchestrator.Provider;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class CreateTcpEndPointRequest {
    private Provider provider;
    private String hostname;
    private int port;
    private boolean ssl;
    private int socketSendBufferSize;
    private int socketReceiveBufferSize;

    MetricsAggregation metricsAggregation = new MetricsAggregation();

    public Provider getProvider() {
        return provider;
    }

    public void setProvider(Provider provider) {
        this.provider = provider;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
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

    public MetricsAggregation getMetricsAggregation() {
        return metricsAggregation;
    }

    public void setMetricsAggregation(MetricsAggregation metricsAggregation) {
        this.metricsAggregation = metricsAggregation;
    }
}
