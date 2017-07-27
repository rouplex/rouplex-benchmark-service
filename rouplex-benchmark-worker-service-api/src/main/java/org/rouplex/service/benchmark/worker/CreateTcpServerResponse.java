package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class CreateTcpServerResponse {
    private String serverId;
    private String hostaddress;
    private String hostname;
    private int port;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

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
