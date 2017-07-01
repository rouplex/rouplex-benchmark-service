package org.rouplex.service.benchmark.auth;

public class UserInfo {
    private String userIdAtProvider;
    private String userName; // if available
    private UserPreferences userPreferences = new UserPreferences();

    public String getUserIdAtProvider() {
        return userIdAtProvider;
    }

    public void setUserIdAtProvider(String userIdAtProvider) {
        this.userIdAtProvider = userIdAtProvider;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public UserPreferences getUserPreferences() {
        return userPreferences;
    }

    public void setUserPreferences(UserPreferences userPreferences) {
        this.userPreferences = userPreferences;
    }
}
