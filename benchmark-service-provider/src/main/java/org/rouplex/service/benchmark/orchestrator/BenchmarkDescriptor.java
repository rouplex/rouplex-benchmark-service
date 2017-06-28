package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkDescriptor<T> {
    private final StartTcpEchoBenchmarkRequest startTcpEchoBenchmarkRequest;
    private final long expirationTimestamp;
    private final Map<String, InstanceDescriptor<T>> instanceDescriptors = new HashMap<>();
    private Exception eventualException;

    public BenchmarkDescriptor(StartTcpEchoBenchmarkRequest startTcpEchoBenchmarkRequest, long expirationTimestamp) {
        this.startTcpEchoBenchmarkRequest = startTcpEchoBenchmarkRequest;
        this.expirationTimestamp = expirationTimestamp;
    }

    public StartTcpEchoBenchmarkRequest getStartTcpEchoBenchmarkRequest() {
        return startTcpEchoBenchmarkRequest;
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
