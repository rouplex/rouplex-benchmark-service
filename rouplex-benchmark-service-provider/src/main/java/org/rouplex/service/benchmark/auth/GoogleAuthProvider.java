package org.rouplex.service.benchmark.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.google.gson.Gson;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.service.benchmark.BenchmarkConfigurationKey;

import javax.ws.rs.core.MediaType;
import java.util.Collections;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
class GoogleAuthProvider extends AuthProvider<UserAtGoogle> {
    private static final Logger logger = Logger.getLogger(GoogleAuthProvider.class.getSimpleName());

    private static final Gson gson = new Gson();
    private static final HttpTransport httpTransport = new NetHttpTransport();

    private final Configuration configuration;
    private final String googleRedirectUri;
    private final GoogleAuthorizationCodeFlow authClient;

    GoogleAuthProvider(Configuration configuration) {
        this.configuration = configuration;
        googleRedirectUri = configuration.get(BenchmarkConfigurationKey.BenchmarkMainUrl);

        authClient = new GoogleAuthorizationCodeFlow.Builder(
                httpTransport, new JacksonFactory(),
                configuration.get(BenchmarkConfigurationKey.GoogleCloudClientId),
                configuration.get(BenchmarkConfigurationKey.GoogleCloudClientPassword),
                Oauth2Scopes.all()).setAccessType("online").setApprovalPrompt("force")
                .build();
    }

    private static class GoogleUserInfo {
        String email;
        String given_name;
        String family_name;
    }

    StartSignInUsingGoogleOauth2Response startSignInUsingGoogleOauth2(String state) {
        String sessionId = UUID.randomUUID().toString();

        String url = authClient.newAuthorizationUrl()
            .setRedirectUri(googleRedirectUri)
            .setScopes(Collections.singletonList(Oauth2Scopes.USERINFO_EMAIL))
            .setState(sessionId + ";" + state + ";")
            .build();

        SessionInfo sessionInfo = new SessionInfo();
        sessionInfo.setSessionId(sessionId);
        StartSignInUsingGoogleOauth2Response response = new StartSignInUsingGoogleOauth2Response(sessionInfo);
        response.setRedirectUrl(url);
        return response;
    }

    FinishSignInUsingGoogleOauth2Response finishSignInUsingGoogleOauth2(SessionInfo sessionInfo, String authCode) throws Exception {
        // resolve user based on the authCode handle from google
        GoogleTokenResponse googleTokenResponse = authClient.newTokenRequest(authCode)
                .setRedirectUri(googleRedirectUri)
                .execute();

        Credential credential = authClient.createAndStoreCredential(googleTokenResponse, null);

        HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
        HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(
                configuration.get(BenchmarkConfigurationKey.GoogleUserInfoEndPoint)));
        request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String jsonIdentity = request.execute().parseAsString();
        GoogleUserInfo googleUserInfo = gson.fromJson(jsonIdentity, GoogleUserInfo.class);

        UserAtGoogle userAtGoogle = new UserAtGoogle();
        userAtGoogle.setUserId(googleUserInfo.email);
        userAtGoogle.setGivenName(googleUserInfo.given_name);
        userAtGoogle.setFamilyName(googleUserInfo.family_name);

        try {
            userAtGoogle = getAuthenticatedUser(userAtGoogle);
        } catch (AuthException authException) {
            userAtGoogle.setUser(inferUserProfile(userAtGoogle));
            addUser(userAtGoogle);
        }

        sessionInfo.setUserInfo(fromUserAtProvider(userAtGoogle));
        return new FinishSignInUsingGoogleOauth2Response(sessionInfo);
    }

    private User inferUserProfile(UserAtGoogle userAtGoogle) {
        User user = new User();
        user.setUserId(UUID.randomUUID());
        user.setUserName(userAtGoogle.getGivenName() + " " + userAtGoogle.getFamilyName());
        CostProfile costProfile = new CostProfile();
        costProfile.setTotalDollarsPerHour(10);
        user.setCostProfile(costProfile);
        return user;
    }
}
