package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkDescriptor {
    TcpMetricsExpectation tcpMetricsExpectation;
    Map<String, InstanceDescriptor> benchmarkInstances = new HashMap<String, InstanceDescriptor>();

    public TcpMetricsExpectation getTcpMetricsExpectation() {
        return tcpMetricsExpectation;
    }

    public void setTcpMetricsExpectation(TcpMetricsExpectation tcpMetricsExpectation) {
        this.tcpMetricsExpectation = tcpMetricsExpectation;
    }

    public Map<String, InstanceDescriptor> getBenchmarkInstances() {
        return benchmarkInstances;
    }

    public void setBenchmarkInstances(Map<String, InstanceDescriptor> benchmarkInstances) {
        this.benchmarkInstances = benchmarkInstances;
    }
}
