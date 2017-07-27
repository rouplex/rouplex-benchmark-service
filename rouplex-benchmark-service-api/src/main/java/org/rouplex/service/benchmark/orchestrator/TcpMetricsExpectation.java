package org.rouplex.service.benchmark.orchestrator;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class TcpMetricsExpectation {
    private int rampUpInMillis;
    private int durationMillis;
    private double connectionsPerSecond;
    private int maxSimultaneousConnections;
    private long maxUploadSpeedInBitsPerSecond;
    private long maxDownloadSpeedInBitsPerSecond;

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
}
