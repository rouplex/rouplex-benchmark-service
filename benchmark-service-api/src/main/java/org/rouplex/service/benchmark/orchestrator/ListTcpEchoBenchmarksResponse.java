package org.rouplex.service.benchmark.orchestrator;

import java.util.Collection;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class ListTcpEchoBenchmarksResponse {
    Collection<String> benchmarkIds;

    public Collection<String> getBenchmarkIds() {
        return benchmarkIds;
    }

    public void setBenchmarkIds(Collection<String> benchmarkIds) {
        this.benchmarkIds = benchmarkIds;
    }
}
