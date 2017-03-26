package org.rouplex.service.benchmarkservice.tcp;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Request {
    boolean useNiossl = true; // if ssl is true this must be set to true, otherwise will throw not implemented exception
    String hostname;
    int port;
    boolean ssl;
    boolean useSharedBinder = true;
    int socketSendBufferSize;
    int socketReceiveBufferSize;

    MetricsAggregation metricsAggregation = new MetricsAggregation();

    public boolean isUseNiossl() {
        return useNiossl;
    }

    public void setUseNiossl(boolean useNiossl) {
        this.useNiossl = useNiossl;
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

    public boolean isUseSharedBinder() {
        return useSharedBinder;
    }

    public void setUseSharedBinder(boolean useSharedBinder) {
        this.useSharedBinder = useSharedBinder;
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
