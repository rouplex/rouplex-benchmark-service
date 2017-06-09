package org.rouplex.service.benchmark.auth;

import java.util.List;
import java.util.UUID;

class UserAtRouplex extends UserAtProvider {
    private String password; // null if pending confirmations non-empty
    private List<UUID> pendingConfirmations; // null if password is present

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<UUID> getPendingConfirmations() {
        return pendingConfirmations;
    }

    public void setPendingConfirmations(List<UUID> pendingConfirmations) {
        this.pendingConfirmations = pendingConfirmations;
    }
}
