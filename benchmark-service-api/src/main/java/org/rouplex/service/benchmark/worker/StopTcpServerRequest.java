package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StopTcpServerRequest {
    String hostname;
    int port;

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
}
