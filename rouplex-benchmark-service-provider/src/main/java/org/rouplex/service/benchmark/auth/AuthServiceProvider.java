package org.rouplex.service.benchmark.auth;

import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class AuthServiceProvider implements AuthService, Closeable {
    public enum ConfigurationKey {
        BenchmarkMainUrl,
        GoogleCloudClientId,
        GoogleCloudClientPassword,
        GoogleUserInfoEndPoint,
    }

    private static final Logger logger = Logger.getLogger(AuthServiceProvider.class.getSimpleName());
    private static AuthServiceProvider benchmarkAuthService;
    public static AuthServiceProvider get() throws Exception {
        synchronized (AuthServiceProvider.class) {
            if (benchmarkAuthService == null) {
                ConfigurationManager configurationManager = new ConfigurationManager();

                configurationManager.putConfigurationEntry(ConfigurationKey.BenchmarkMainUrl,
                    System.getProperty(ConfigurationKey.BenchmarkMainUrl.toString()));
                configurationManager.putConfigurationEntry(ConfigurationKey.GoogleCloudClientId,
                    System.getProperty(ConfigurationKey.GoogleCloudClientId.toString()));
                configurationManager.putConfigurationEntry(ConfigurationKey.GoogleCloudClientPassword,
                    System.getProperty(ConfigurationKey.GoogleCloudClientPassword.toString()));
                configurationManager.putConfigurationEntry(ConfigurationKey.GoogleUserInfoEndPoint,
                    "https://www.googleapis.com/oauth2/v3/userinfo");

                benchmarkAuthService = new AuthServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkAuthService;
        }
    }

    private final Map<String, SessionInfo> sessionInfos = new HashMap<>();

    private final BasicAuthProvider basicAuthProvider;
    private final GoogleAuthProvider googleAuthProvider;

    AuthServiceProvider(Configuration configuration) throws Exception {
        basicAuthProvider = new BasicAuthProvider();
        googleAuthProvider = new GoogleAuthProvider(configuration);
    }

    public UserInfo getUserInfo(String sessionId) {
        SessionInfo sessionInfo = sessionInfos.get(sessionId);
        return sessionInfo != null ? sessionInfo.getUserInfo() : null;
    }

    private <T extends GetSessionInfoResponse> T addSessionInfo(T getSessionInfoResponse) {
        SessionInfo sessionInfo = getSessionInfoResponse.getSessionInfo();
        sessionInfos.put(sessionInfo.getSessionId(), sessionInfo);
        return getSessionInfoResponse;
    }

    /**
     * Taking this out for the moment. Using an existing sessionId while a new session is about to start needs a more
     * thorough understanding of the risks involved.
     */
    private <T extends SessionInfo> T signInUsingExistingSession(String sessionId, Supplier<T> responseSupplier) {
        UserInfo userInfo = getUserInfo(sessionId);

        if (userInfo != null) {
            T response = responseSupplier.get();
            response.setSessionId(sessionId);
            response.setUserInfo(userInfo);
            // no redirect url for authentication
            return response;
        }

        return null;
    }

    @Override
    public StartSignInUsingGoogleOauth2Response startSignInUsingGoogleOauth2(String state) throws Exception {
        return addSessionInfo(googleAuthProvider.startSignInUsingGoogleOauth2(state));
    }

    @Override
    public FinishSignInUsingGoogleOauth2Response finishSignInUsingGoogleOauth2(String state, String code) throws Exception {
        int semicolonPos = state == null ? -1 : state.indexOf(';');
        String sessionId = semicolonPos != -1 ? state.substring(0, semicolonPos) : null;

        if (sessionId == null) {
            throw new Exception("Authentication cannot proceed. Cause: Missing session id");
        }

        SessionInfo sessionInfo = sessionInfos.get(sessionId);
        if (sessionInfo == null) {
            throw new Exception(String.format("Authentication cannot proceed. Cause: Session id %s not found", sessionId));
        }

        return googleAuthProvider.finishSignInUsingGoogleOauth2(sessionInfo, code);
    }

    @Override
    public SignInUsingBasicAuthResponse signInUsingBasicAuth(String userName, String password) throws Exception {
        return addSessionInfo(basicAuthProvider.signInUsingBasicAuth(userName, password));
    }

    @Override
    public SignOutResponse signOut(String sessionIdViaHeaderParam) throws Exception {
        if (sessionInfos.remove(sessionIdViaHeaderParam) == null) {
            throw new Exception("Session not found");
        }

        return new SignOutResponse();
    }

    @Override
    public GetSessionInfoResponse getSessionInfo(String sessionIdViaCookie, String sessionIdViaHeader) throws Exception {
        SessionInfo sessionInfo = new SessionInfo();

        UserInfo userInfo = getUserInfo(sessionIdViaHeader);
        if (userInfo != null) {
            sessionInfo.setUserInfo(userInfo);
            sessionInfo.setSessionId(sessionIdViaHeader);
        } else {
            sessionInfo.setUserInfo(getUserInfo(sessionIdViaCookie));
            sessionInfo.setSessionId(sessionIdViaCookie);
        }

        return new GetSessionInfoResponse(sessionInfo);
    }

    @Override
    public void setUserPreferences(String sessionIdViaHeader, UserPreferences userPreferences) throws Exception {
        // todo
    }

    @Override
    public void close() throws IOException {
    }
}