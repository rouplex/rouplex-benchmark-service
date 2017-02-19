package org.rouplex.service.benchmarkservice.tcp;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartTcpServerRequest {
    String hostname;
    int port;
    boolean ssl;

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
}
