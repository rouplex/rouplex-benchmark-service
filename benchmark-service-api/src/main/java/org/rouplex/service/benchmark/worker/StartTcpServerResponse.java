package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartTcpServerResponse extends StartTcpEndPointResponse {
    String hostaddress;
    String hostname;
    int port;

    public String getHostaddress() {
        return hostaddress;
    }

    public void setHostaddress(String hostaddress) {
        this.hostaddress = hostaddress;
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
}
