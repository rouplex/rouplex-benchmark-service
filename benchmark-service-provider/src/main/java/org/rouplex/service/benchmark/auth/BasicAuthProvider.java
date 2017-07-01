package org.rouplex.service.benchmark.auth;

import java.util.UUID;

class BasicAuthProvider extends AuthProvider<UserAtRouplex> {

    BasicAuthProvider() {
        // todo remove this hack, when proper user registration is implemented
        addBetaTester();
    }

    SignInUsingBasicAuthResponse signInUsingBasicAuth(String userId, String password) throws Exception {
        UserAtRouplex userAtRouplex = new UserAtRouplex();
        userAtRouplex.setUserId(userId);
        userAtRouplex.setPassword(password);
        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(UUID.randomUUID().toString());
        sessionInfo.setUserInfo(fromUserAtProvider(getAuthenticatedUser(userAtRouplex)));
        return new SignInUsingBasicAuthResponse(sessionInfo);
    }

    private void addBetaTester() {
        UserAtRouplex userAtRouplex = new UserAtRouplex();
        userAtRouplex.setUserId("tester@rouplex-demo.com");
        userAtRouplex.setPassword("tester");

        User user = defaultUserProfile();
        user.setUserName("Beta Tester");
        userAtRouplex.setUser(user);

        try {
            addUser(userAtRouplex);
        } catch (AuthException ae) {
            // never thrown in this context
        }
    }

    private User defaultUserProfile() {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName("");
        CostProfile costProfile = new CostProfile();
        costProfile.setTotalDollarsPerHour(10);
        user.setCostProfile(costProfile);
        return user;
    }
}
