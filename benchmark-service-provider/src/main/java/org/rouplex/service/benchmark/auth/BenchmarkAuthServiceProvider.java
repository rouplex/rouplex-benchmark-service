package org.rouplex.service.benchmark.auth;

import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.service.benchmark.BenchmarkConfigurationKey;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkAuthServiceProvider implements BenchmarkAuthService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkAuthServiceProvider.class.getSimpleName());

    private static BenchmarkAuthServiceProvider benchmarkAuthService;
    public static BenchmarkAuthServiceProvider get() throws Exception {
        synchronized (BenchmarkAuthServiceProvider.class) {
            if (benchmarkAuthService == null) {
                ConfigurationManager configurationManager = new ConfigurationManager();
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientId,
                        System.getProperty(BenchmarkConfigurationKey.GoogleCloudClientId.toString()));
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientPassword,
                        System.getProperty(BenchmarkConfigurationKey.GoogleCloudClientPassword.toString()));
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.BenchmarkMainUrl,
                        "http://localhost:8080/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/index.html");
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleUserInfoEndPoint,
                        "https://www.googleapis.com/oauth2/v3/userinfo");

                benchmarkAuthService = new BenchmarkAuthServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkAuthService;
        }
    }

    private final Map<String, SessionInfo> sessionInfos = new HashMap<>();

    private final RouplexAuthProvider rouplexAuthProvider;
    private final GoogleAuthProvider googleAuthProvider;

    BenchmarkAuthServiceProvider(Configuration configuration) throws Exception {
        rouplexAuthProvider = new RouplexAuthProvider();
        googleAuthProvider = new GoogleAuthProvider(configuration);
    }

    @Override
    public SignInResponse signIn(String sessionIdViaCookie, String authProvider,
                                 String authEmail, String authPassword,
                                 String sessionIdViaQueryParam, String code) throws Exception {

        SignInResponse signInResponse = new SignInResponse();
        SessionInfo sessionInfo = sessionInfos.get(sessionIdViaCookie);
        if (sessionInfo != null) { // user in session and hence known, return its profile
            signInResponse.setSessionId(sessionInfo.getSessionId());
            signInResponse.setUserInfo(sessionInfo.getUserInfo());
            return signInResponse;
        }

        if (authProvider != null) {
            String sessionId = null;
            switch (AuthProvider.Provider.valueOf(authProvider)) {
                case rouplex:
                    signInResponse = rouplexAuthProvider.auth(authEmail, authPassword);
                    sessionId = UUID.randomUUID().toString();
                    break;
                case google:
                    signInResponse = googleAuthProvider.auth(code);
                    sessionId = sessionIdViaQueryParam;
                    break;
            }

            sessionInfo = new SessionInfo();
            sessionInfo.setUserInfo(signInResponse.getUserInfo());
            sessionInfo.setSessionId(sessionId);
            sessionInfos.put(sessionId, sessionInfo);

            signInResponse.setSessionId(sessionId);
        }

        return signInResponse; // user unknown, no defined auth providers, no action to be taken
    }

    @Override
    public SignOutResponse signOut(String sessionIdViaHeaderParam) throws Exception {
        if (sessionInfos.remove(sessionIdViaHeaderParam) == null) {
            throw new Exception("Session not found");
        }

        return new SignOutResponse();
    }

    @Override
    public void close() throws IOException {
    }
}