package org.rouplex.service.benchmark.auth;

import java.util.UUID;

public class User {
    /**
     * Unique, internally generated userId
     */
    private UUID userId;

    /**
     * Name of the user as displayed in the UI (along with user's id from the provider, e.g. John: johns@gmail.com)
     */
    private String userName;
    private CostProfile costProfile;

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public CostProfile getCostProfile() {
        return costProfile;
    }

    public void setCostProfile(CostProfile costProfile) {
        this.costProfile = costProfile;
    }
}
