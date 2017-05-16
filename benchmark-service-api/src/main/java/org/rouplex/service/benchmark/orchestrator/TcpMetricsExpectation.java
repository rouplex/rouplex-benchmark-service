package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class TcpMetricsExpectation {
    int rampUpAsMillis;
    double connectionsPerSecond;
    int maxSimultaneousConnections;
    long maxUploadSpeedAsBitsPerSecond;
    long maxDownloadSpeedAsBitsPerSecond;
    String startAsIsoInstant;
    String finishRampUpAsIsoInstant;
    String finishAsIsoInstant;
    String maxUploadSpeed;
    String maxDownloadSpeed;

    public int getRampUpAsMillis() {
        return rampUpAsMillis;
    }

    public void setRampUpAsMillis(int rampUpAsMillis) {
        this.rampUpAsMillis = rampUpAsMillis;
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

    public long getMaxUploadSpeedAsBitsPerSecond() {
        return maxUploadSpeedAsBitsPerSecond;
    }

    public void setMaxUploadSpeedAsBitsPerSecond(long maxUploadSpeedAsBitsPerSecond) {
        this.maxUploadSpeedAsBitsPerSecond = maxUploadSpeedAsBitsPerSecond;
    }

    public long getMaxDownloadSpeedAsBitsPerSecond() {
        return maxDownloadSpeedAsBitsPerSecond;
    }

    public void setMaxDownloadSpeedAsBitsPerSecond(long maxDownloadSpeedAsBitsPerSecond) {
        this.maxDownloadSpeedAsBitsPerSecond = maxDownloadSpeedAsBitsPerSecond;
    }

    public String getStartAsIsoInstant() {
        return startAsIsoInstant;
    }

    public void setStartAsIsoInstant(String startAsIsoInstant) {
        this.startAsIsoInstant = startAsIsoInstant;
    }

    public String getFinishRampUpAsIsoInstant() {
        return finishRampUpAsIsoInstant;
    }

    public void setFinishRampUpAsIsoInstant(String finishRampUpAsIsoInstant) {
        this.finishRampUpAsIsoInstant = finishRampUpAsIsoInstant;
    }

    public String getFinishAsIsoInstant() {
        return finishAsIsoInstant;
    }

    public void setFinishAsIsoInstant(String finishAsIsoInstant) {
        this.finishAsIsoInstant = finishAsIsoInstant;
    }

    public String getMaxUploadSpeed() {
        return maxUploadSpeed;
    }

    public void setMaxUploadSpeed(String maxUploadSpeed) {
        this.maxUploadSpeed = maxUploadSpeed;
    }

    public String getMaxDownloadSpeed() {
        return maxDownloadSpeed;
    }

    public void setMaxDownloadSpeed(String maxDownloadSpeed) {
        this.maxDownloadSpeed = maxDownloadSpeed;
    }
}
