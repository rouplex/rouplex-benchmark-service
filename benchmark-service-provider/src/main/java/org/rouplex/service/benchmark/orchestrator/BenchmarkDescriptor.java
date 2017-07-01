package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkDescriptor<T> {
    private final StartTcpEchoBenchmarkRequest startTcpEchoBenchmarkRequest;
    private final long startingTimestamp;
    private final long expectedDurationMillis;
    private final Map<String, InstanceDescriptor<T>> instanceDescriptors = new HashMap<>();

    private long startedTimestamp;
    private Exception eventualException;

    public BenchmarkDescriptor(StartTcpEchoBenchmarkRequest startTcpEchoBenchmarkRequest,
                               long startingTimestamp, long expectedDurationMillis) {
        this.startTcpEchoBenchmarkRequest = startTcpEchoBenchmarkRequest;
        this.startingTimestamp = startingTimestamp;
        this.expectedDurationMillis = expectedDurationMillis;
    }

    public StartTcpEchoBenchmarkRequest getStartTcpEchoBenchmarkRequest() {
        return startTcpEchoBenchmarkRequest;
    }

    public long getStartingTimestamp() {
        return startingTimestamp;
    }

    public long getExpectedDurationMillis() {
        return expectedDurationMillis;
    }

    public Map<String, InstanceDescriptor<T>> getInstanceDescriptors() {
        return instanceDescriptors;
    }

    public long getStartedTimestamp() {
        return startedTimestamp;
    }

    public void setStartedTimestamp(long startedTimestamp) {
        this.startedTimestamp = startedTimestamp;
    }

    public Exception getEventualException() {
        return eventualException;
    }

    public void setEventualException(Exception eventualException) {
        this.eventualException = eventualException;
    }
}
