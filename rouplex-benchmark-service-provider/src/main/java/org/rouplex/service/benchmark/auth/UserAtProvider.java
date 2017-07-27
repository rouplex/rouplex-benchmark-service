package org.rouplex.service.benchmark.auth;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
class UserAtProvider {
    /**
     * The unique key identifying the user at the provider. Can be user's email address, twitter handle, real name ...
     */
    private String userId;
    private User user;

    public boolean same(UserAtProvider other) {
        return userId.equals(other.getUserId());
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
