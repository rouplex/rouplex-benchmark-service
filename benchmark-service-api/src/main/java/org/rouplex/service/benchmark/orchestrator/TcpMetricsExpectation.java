package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class TcpMetricsExpectation {
    int rampUpInMillis;
    int durationMillis;
    double connectionsPerSecond;
    int maxSimultaneousConnections;
    long maxUploadSpeedInBitsPerSecond;
    long maxDownloadSpeedInBitsPerSecond;
    String startAsIsoInstant;
    String finishRampUpAsIsoInstant;
    String finishAsIsoInstant;
    String maxUploadSpeed;
    String maxDownloadSpeed;

    public int getRampUpInMillis() {
        return rampUpInMillis;
    }

    public void setRampUpInMillis(int rampUpInMillis) {
        this.rampUpInMillis = rampUpInMillis;
    }

    public int getDurationMillis() {
        return durationMillis;
    }

    public void setDurationMillis(int durationMillis) {
        this.durationMillis = durationMillis;
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

    public long getMaxUploadSpeedInBitsPerSecond() {
        return maxUploadSpeedInBitsPerSecond;
    }

    public void setMaxUploadSpeedInBitsPerSecond(long maxUploadSpeedInBitsPerSecond) {
        this.maxUploadSpeedInBitsPerSecond = maxUploadSpeedInBitsPerSecond;
    }

    public long getMaxDownloadSpeedInBitsPerSecond() {
        return maxDownloadSpeedInBitsPerSecond;
    }

    public void setMaxDownloadSpeedInBitsPerSecond(long maxDownloadSpeedInBitsPerSecond) {
        this.maxDownloadSpeedInBitsPerSecond = maxDownloadSpeedInBitsPerSecond;
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
