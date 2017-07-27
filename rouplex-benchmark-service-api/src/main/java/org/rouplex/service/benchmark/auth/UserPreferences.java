package org.rouplex.service.benchmark.auth;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class UserPreferences {
    private boolean useUtcTime;

    public boolean isUseUtcTime() {
        return useUtcTime;
    }

    public void setUseUtcTime(boolean useUtcTime) {
        this.useUtcTime = useUtcTime;
    }
}
