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
    boolean mergeClientMetrics;

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

    public boolean isUseNiossl() {
        return useNiossl;
    }

    public void setUseNiossl(boolean useNiossl) {
        this.useNiossl = useNiossl;
    }

    public boolean isUseSharedBinder() {
        return useSharedBinder;
    }

    public void setUseSharedBinder(boolean useSharedBinder) {
        this.useSharedBinder = useSharedBinder;
    }

    public boolean isMergeClientMetrics() {
        return mergeClientMetrics;
    }

    public void setMergeClientMetrics(boolean mergeClientMetrics) {
        this.mergeClientMetrics = mergeClientMetrics;
    }
}
