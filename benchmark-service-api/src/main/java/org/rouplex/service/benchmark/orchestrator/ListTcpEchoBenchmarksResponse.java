package org.rouplex.service.benchmark.orchestrator;

import java.util.List;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class ListTcpEchoBenchmarksResponse {
    List<String> benchmarkIds;

    public List<String> getBenchmarkIds() {
        return benchmarkIds;
    }

    public void setBenchmarkIds(List<String> benchmarkIds) {
        this.benchmarkIds = benchmarkIds;
    }
}
