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
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.service.benchmark.orchestrator.BenchmarkConfigurationKey;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkAuthServiceProvider implements BenchmarkAuthService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkAuthServiceProvider.class.getSimpleName());
    private static final Gson gson = new Gson();
    private static final HttpTransport httpTransport = new NetHttpTransport();

    private static BenchmarkAuthService benchmarkAuthService;
    public static BenchmarkAuthService get() throws Exception {
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
                        "https://www.googleapis.com/oauth2/v1/userinfo");

                benchmarkAuthService = new BenchmarkAuthServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkAuthService;
        }
    }

    private final Configuration configuration;
    private final GoogleAuthorizationCodeFlow authClient;

    BenchmarkAuthServiceProvider(Configuration configuration) {
        this.configuration = configuration;

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

    @Override
    public GoogleAuthResponse googleAuth(String authCode, String authUser, String sessionState, String prompt) throws Exception {
        GoogleAuthResponse googleAuthResponse = new GoogleAuthResponse();

        if (authCode != null) {
            try {
                GoogleTokenResponse response = authClient.newTokenRequest(authCode)
                        .setRedirectUri(configuration.get(BenchmarkConfigurationKey.BenchmarkMainUrl)).execute();
                Credential credential = authClient.createAndStoreCredential(response, null);
                HttpRequestFactory requestFactory = httpTransport.createRequestFactory(credential);
                // Make an authenticated request

                HttpRequest request = requestFactory.buildGetRequest(new GenericUrl(
                        configuration.get(BenchmarkConfigurationKey.GoogleUserInfoEndPoint)));
                request.getHeaders().setContentType(MediaType.APPLICATION_JSON);

                String jsonIdentity = request.execute().parseAsString();
                GoogleUserInfo googleUserInfo = gson.fromJson(jsonIdentity, GoogleUserInfo.class);
                googleAuthResponse.setUserEmail(googleUserInfo.email);
                googleAuthResponse.setUserGivenName(googleUserInfo.given_name);
                googleAuthResponse.setUserFamilyName(googleUserInfo.family_name);

                return googleAuthResponse;
            } catch (Exception e) {
                e.printStackTrace();
                // for now just fall through ... and provide the login redirect
            }
        }

        String url = authClient.newAuthorizationUrl()
                .setRedirectUri(configuration.get(BenchmarkConfigurationKey.BenchmarkMainUrl)).build();
        googleAuthResponse.setRedirectUrl(url);
        return googleAuthResponse;
    }

    @Override
    public Response googleAuthWithCorsHeaders(String code, String authUser, String sessionState, String prompt) throws Exception {
        return null;
    }

    @Override
    public String rouplexAuth(String email, String password) throws Exception {
        return null;
    }

    @Override
    public void close() throws IOException {
    }
}