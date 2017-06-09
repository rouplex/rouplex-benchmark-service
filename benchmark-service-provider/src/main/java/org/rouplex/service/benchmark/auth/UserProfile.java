package org.rouplex.service.benchmark.auth;

import javax.annotation.Nonnull;
import java.util.UUID;

public class UserProfile {
    /**
     * Unique, internally generated userId
     */
    private @Nonnull UUID userId;

    /**
     * Name of the user as displayed in the UI (along with user's id from the provider, e.g. John: johns@gmail.com)
     */
    private @Nonnull String userName;
    private CostProfile costProfile;

    @Nonnull
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(@Nonnull UUID userId) {
        this.userId = userId;
    }

    @Nonnull
    public String getUserName() {
        return userName;
    }

    public void setUserName(@Nonnull String userName) {
        this.userName = userName;
    }

    public CostProfile getCostProfile() {
        return costProfile;
    }

    public void setCostProfile(CostProfile costProfile) {
        this.costProfile = costProfile;
    }
}
