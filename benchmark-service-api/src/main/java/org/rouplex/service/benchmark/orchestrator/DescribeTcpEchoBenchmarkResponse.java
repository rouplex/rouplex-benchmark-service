package org.rouplex.service.benchmark.orchestrator;

import java.util.List;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class DescribeTcpEchoBenchmarkResponse {
    String benchmarkId;
    String imageId;

    HostType serverHostType;
    GeoLocation serverGeoLocation;
    String serverIpAddress;

    HostType clientsHostType;
    GeoLocation clientsGeoLocation;
    List<String> clientIpAddresses;

    String jconsoleJmxLink;

    TcpMetricsExpectation tcpServerExpectation;
    TcpMetricsExpectation tcpClientsExpectation;

    public String getBenchmarkId() {
        return benchmarkId;
    }

    public void setBenchmarkId(String benchmarkId) {
        this.benchmarkId = benchmarkId;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public HostType getServerHostType() {
        return serverHostType;
    }

    public void setServerHostType(HostType serverHostType) {
        this.serverHostType = serverHostType;
    }

    public GeoLocation getServerGeoLocation() {
        return serverGeoLocation;
    }

    public void setServerGeoLocation(GeoLocation serverGeoLocation) {
        this.serverGeoLocation = serverGeoLocation;
    }

    public String getServerIpAddress() {
        return serverIpAddress;
    }

    public void setServerIpAddress(String serverIpAddress) {
        this.serverIpAddress = serverIpAddress;
    }

    public HostType getClientsHostType() {
        return clientsHostType;
    }

    public void setClientsHostType(HostType clientsHostType) {
        this.clientsHostType = clientsHostType;
    }

    public GeoLocation getClientsGeoLocation() {
        return clientsGeoLocation;
    }

    public void setClientsGeoLocation(GeoLocation clientsGeoLocation) {
        this.clientsGeoLocation = clientsGeoLocation;
    }

    public List<String> getClientIpAddresses() {
        return clientIpAddresses;
    }

    public void setClientIpAddresses(List<String> clientIpAddresses) {
        this.clientIpAddresses = clientIpAddresses;
    }

    public String getJconsoleJmxLink() {
        return jconsoleJmxLink;
    }

    public void setJconsoleJmxLink(String jconsoleJmxLink) {
        this.jconsoleJmxLink = jconsoleJmxLink;
    }

    public TcpMetricsExpectation getTcpServerExpectation() {
        return tcpServerExpectation;
    }

    public void setTcpServerExpectation(TcpMetricsExpectation tcpServerExpectation) {
        this.tcpServerExpectation = tcpServerExpectation;
    }

    public TcpMetricsExpectation getTcpClientsExpectation() {
        return tcpClientsExpectation;
    }

    public void setTcpClientsExpectation(TcpMetricsExpectation tcpClientsExpectation) {
        this.tcpClientsExpectation = tcpClientsExpectation;
    }
}
