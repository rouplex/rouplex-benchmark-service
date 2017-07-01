package org.rouplex.service.benchmark.auth;

import java.util.HashMap;
import java.util.Map;

class AuthProvider<T extends UserAtProvider> {
    Map<String, T> usersAtProvider = new HashMap<>();

    /**
     * Add or replace a user for this provider. Inheriting classes can opt to throw an exception if this operation is
     * not acceptable to be added.
     *
     * @param userAtProvider
     *          The user to be added or replaced
     */
    void addUser(T userAtProvider) throws AuthException {
        usersAtProvider.put(userAtProvider.getUserId(), userAtProvider);
    }

    T getAuthenticatedUser(T userAtProvider) throws AuthException {
        T found = usersAtProvider.get(userAtProvider.getUserId());
        if (found == null) {
            throw new AuthException(String.format(
                    "User %s not found", userAtProvider.getUserId()), AuthException.Reason.NotFound);
        }

        if (!found.same(userAtProvider)) {
            throw new AuthException(String.format(
                    "Bad user credentials for %s", userAtProvider.getUserId()), AuthException.Reason.BadCredentials);
        }

        return found;
    }

    protected static UserInfo fromUserAtProvider(UserAtProvider userAtProvider) {
        UserInfo userInfo = new UserInfo();
        userInfo.setUserName(userAtProvider.getUser().getUserName());
        userInfo.setUserIdAtProvider(userAtProvider.getUserId());
        return userInfo;
    }
}
