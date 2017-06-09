package org.rouplex.service.benchmark.auth;

import java.util.UUID;

class RouplexAuthProvider extends AuthProvider<UserAtRouplex> {

    RouplexAuthProvider() {
        // todo remove this hack, when proper user registration is implemented
        addBetaTester(); // todo remove this
    }

    SignInResponse auth(String email, String password) throws Exception {
        SignInResponse signInResponse = new SignInResponse();
        UserAtRouplex userAtRouplex = new UserAtRouplex();
        userAtRouplex.setUserId(email);
        userAtRouplex.setPassword(password);
        signInResponse.setUserInfo(fromUserAtProvider(getAuthenticatedUser(userAtRouplex)));
        return signInResponse;
    }

    private UserProfile defaultUserProfile() {
        UserProfile userProfile = new UserProfile();
        userProfile.setUserId(UUID.randomUUID());
        userProfile.setUserName("");
        CostProfile costProfile = new CostProfile();
        costProfile.setTotalDollarsPerHour(10);
        userProfile.setCostProfile(costProfile);
        return userProfile;
    }

    private void addBetaTester() {
        UserAtRouplex userAtRouplex = new UserAtRouplex();
        userAtRouplex.setUserId("tester@rouplex-demo.com");
        userAtRouplex.setPassword("tester");

        UserProfile userProfile = defaultUserProfile();
        userProfile.setUserName("Beta Tester");
        userAtRouplex.setUserProfile(userProfile);

        try {
            addUser(userAtRouplex);
        } catch (AuthException ae) {
            // never thrown in this context
        }
    }
}
