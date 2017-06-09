package org.rouplex.service.benchmark.auth;

import javax.annotation.Nonnull;

class UserAtProvider<T extends UserAtProvider> {
    /**
     * The unique key identifying the user at the provider. Can be user's email address, twitter handle, real name ...
     */
    private @Nonnull String userId;
    private UserProfile userProfile;

    public boolean same(T other) {
        return userId.equals(other.getUserId());
    }

    @Nonnull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@Nonnull String userId) {
        this.userId = userId;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    public void setUserProfile(UserProfile userProfile) {
        this.userProfile = userProfile;
    }
}
