package org.rouplex.service.benchmark.auth;

import java.util.UUID;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
class BasicAuthProvider extends AuthProvider<UserAtRouplex> {

    BasicAuthProvider() {
        // todo remove this hack, when proper user registration is implemented
        addBetaTesters();
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

    private void addBetaTesters() {
        addBetaTester("tester@rouplex-demo.com", "tester", "Beta Tester");
        addBetaTester("jonschulz@aol.com", "jonschulz@aol.com", "John Schulz");
    }

    private void addBetaTester(String userId, String userPassword, String userName) {
        UserAtRouplex userAtRouplex = new UserAtRouplex();
        userAtRouplex.setUserId(userId);
        userAtRouplex.setPassword(userPassword);

        User user = defaultUserProfile();
        user.setUserName(userName);
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
