package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class TcpMetricsExpectation {
    int rampUpAsMillis;
    String finishRampUpAsIsoInstant;
    double connectionsPerSecond;
    int maxSimultaneousConnections;
    int maxUploadSpeedAsBitsPerSecond;
    int maxDownloadSpeedAsBitsPerSecond;
    String finishAsIsoInstant;

    public int getRampUpAsMillis() {
        return rampUpAsMillis;
    }

    public void setRampUpAsMillis(int rampUpAsMillis) {
        this.rampUpAsMillis = rampUpAsMillis;
    }

    public String getFinishRampUpAsIsoInstant() {
        return finishRampUpAsIsoInstant;
    }

    public void setFinishRampUpAsIsoInstant(String finishRampUpAsIsoInstant) {
        this.finishRampUpAsIsoInstant = finishRampUpAsIsoInstant;
    }

    public double getConnectionsPerSecond() {
        return connectionsPerSecond;
    }

    public void setConnectionsPerSecond(double connectionsPerSecond) {
        this.connectionsPerSecond = connectionsPerSecond;
    }

    public int getMaxSimultaneousConnections() {
        return maxSimultaneousConnections;
    }

    public void setMaxSimultaneousConnections(int maxSimultaneousConnections) {
        this.maxSimultaneousConnections = maxSimultaneousConnections;
    }

    public int getMaxUploadSpeedAsBitsPerSecond() {
        return maxUploadSpeedAsBitsPerSecond;
    }

    public void setMaxUploadSpeedAsBitsPerSecond(int maxUploadSpeedAsBitsPerSecond) {
        this.maxUploadSpeedAsBitsPerSecond = maxUploadSpeedAsBitsPerSecond;
    }

    public int getMaxDownloadSpeedAsBitsPerSecond() {
        return maxDownloadSpeedAsBitsPerSecond;
    }

    public void setMaxDownloadSpeedAsBitsPerSecond(int maxDownloadSpeedAsBitsPerSecond) {
        this.maxDownloadSpeedAsBitsPerSecond = maxDownloadSpeedAsBitsPerSecond;
    }

    public String getFinishAsIsoInstant() {
        return finishAsIsoInstant;
    }

    public void setFinishAsIsoInstant(String finishAsIsoInstant) {
        this.finishAsIsoInstant = finishAsIsoInstant;
    }
}
