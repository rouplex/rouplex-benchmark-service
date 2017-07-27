package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class MetricsAggregation {
    private boolean aggregateSslWithPlain;
    private boolean aggregateServerAddresses = true;
    private boolean aggregateServerPorts = true;
    private boolean aggregateClientAddresses = true;
    private boolean aggregateClientPorts = true;

    public boolean isAggregateServerAddresses() {
        return aggregateServerAddresses;
    }

    public void setAggregateServerAddresses(boolean aggregateServerAddresses) {
        this.aggregateServerAddresses = aggregateServerAddresses;
    }

    public boolean isAggregateServerPorts() {
        return aggregateServerPorts;
    }

    public void setAggregateServerPorts(boolean aggregateServerPorts) {
        this.aggregateServerPorts = aggregateServerPorts;
    }

    public boolean isAggregateClientAddresses() {
        return aggregateClientAddresses;
    }

    public void setAggregateClientAddresses(boolean aggregateClientAddresses) {
        this.aggregateClientAddresses = aggregateClientAddresses;
    }

    public boolean isAggregateClientPorts() {
        return aggregateClientPorts;
    }

    public void setAggregateClientPorts(boolean aggregateClientPorts) {
        this.aggregateClientPorts = aggregateClientPorts;
    }

    public boolean isAggregateSslWithPlain() {
        return aggregateSslWithPlain;
    }

    public void setAggregateSslWithPlain(boolean aggregateSslWithPlain) {
        this.aggregateSslWithPlain = aggregateSslWithPlain;
    }
}
