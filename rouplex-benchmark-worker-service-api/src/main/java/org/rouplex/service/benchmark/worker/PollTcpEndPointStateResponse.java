package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class PollTcpEndPointStateResponse {
    String observedFinishTime; // null if it not observed
    String anticipatedFinishTime; // null if it cannot be anticipated

    public String getObservedFinishTime() {
        return observedFinishTime;
    }

    public void setObservedFinishTime(String observedFinishTime) {
        this.observedFinishTime = observedFinishTime;
    }

    public String getAnticipatedFinishTime() {
        return anticipatedFinishTime;
    }

    public void setAnticipatedFinishTime(String anticipatedFinishTime) {
        this.anticipatedFinishTime = anticipatedFinishTime;
    }
}
