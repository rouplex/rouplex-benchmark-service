package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class DescribeTcpBenchmarkRequest {
    private String benchmarkRequestId;

    public String getBenchmarkRequestId() {
        return benchmarkRequestId;
    }

    public void setBenchmarkRequestId(String benchmarkRequestId) {
        this.benchmarkRequestId = benchmarkRequestId;
    }
}
