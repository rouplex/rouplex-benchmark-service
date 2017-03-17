package org.rouplex.service.benchmarkservice.tcp;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class MetricsAggregation {
    boolean aggregateSslWithPlain;
    boolean aggregateServerAddresses;
    boolean aggregateServerPorts;
    boolean aggregateClientAddresses;
    boolean aggregateClientPorts;
    boolean aggregateClientIds;

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

    public boolean isAggregateClientIds() {
        return aggregateClientIds;
    }

    public void setAggregateClientIds(boolean aggregateClientIds) {
        this.aggregateClientIds = aggregateClientIds;
    }

    public boolean isAggregateSslWithPlain() {
        return aggregateSslWithPlain;
    }

    public void setAggregateSslWithPlain(boolean aggregateSslWithPlain) {
        this.aggregateSslWithPlain = aggregateSslWithPlain;
    }
}
