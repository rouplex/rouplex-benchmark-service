package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class StartTcpEchoBenchmarkResponse {
    String benchmarkId;
    String imageId;

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
