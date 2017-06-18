package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkDescriptor<T> {
    private final StartTcpBenchmarkRequest startTcpBenchmarkRequest;
    private final long expirationTimestamp;
    private final Map<String, InstanceDescriptor<T>> instanceDescriptors = new HashMap<>();
    private Exception eventualException;

    public BenchmarkDescriptor(StartTcpBenchmarkRequest startTcpBenchmarkRequest,
                               long expirationTimestamp) {
        this.startTcpBenchmarkRequest = startTcpBenchmarkRequest;
        this.expirationTimestamp = expirationTimestamp;
    }

    public StartTcpBenchmarkRequest getStartTcpBenchmarkRequest() {
        return startTcpBenchmarkRequest;
    }

    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public Map<String, InstanceDescriptor<T>> getInstanceDescriptors() {
        return instanceDescriptors;
    }

    public Exception getEventualException() {
        return eventualException;
    }

    public void setEventualException(Exception eventualException) {
        this.eventualException = eventualException;
    }
}
