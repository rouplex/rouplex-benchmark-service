package org.rouplex.service.benchmarkservice.tcp;

import java.util.List;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartDistributedTcpBenchmarkResponse {
    String benchmarkRequestId;
    String serverInstanceType;
    String clientInstanceType;
    String serverIpAddress;
    List<String> clientIpAddresses;
    String serverJmxLink;
    List<String> clientJmxLinks;

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

    public String getServerIpAddress() {
        return serverIpAddress;
    }

    public void setServerIpAddress(String serverIpAddress) {
        this.serverIpAddress = serverIpAddress;
    }

    public List<String> getClientIpAddresses() {
        return clientIpAddresses;
    }

    public void setClientIpAddresses(List<String> clientIpAddresses) {
        this.clientIpAddresses = clientIpAddresses;
    }

    public String getServerJmxLink() {
        return serverJmxLink;
    }

    public void setServerJmxLink(String serverJmxLink) {
        this.serverJmxLink = serverJmxLink;
    }

    public List<String> getClientJmxLinks() {
        return clientJmxLinks;
    }

    public void setClientJmxLinks(List<String> clientJmxLinks) {
        this.clientJmxLinks = clientJmxLinks;
    }
}
