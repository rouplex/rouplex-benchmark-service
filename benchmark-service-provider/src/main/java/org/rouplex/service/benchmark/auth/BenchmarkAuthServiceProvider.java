package org.rouplex.service.benchmark.auth;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2Scopes;
import com.google.gson.Gson;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.service.benchmark.orchestrator.BenchmarkConfigurationKey;

import javax.ws.rs.core.Response;
import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkAuthServiceProvider implements BenchmarkAuthService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkAuthServiceProvider.class.getSimpleName());
    private static final Gson gson = new Gson();

    private static BenchmarkAuthService benchmarkAuthService;

    public static BenchmarkAuthService get() throws Exception {
        synchronized (BenchmarkAuthServiceProvider.class) {
            if (benchmarkAuthService == null) {
                ConfigurationManager configurationManager = new ConfigurationManager();
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientId,
                        System.getProperty(BenchmarkConfigurationKey.GoogleCloudClientId.toString()));
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientPassword,
                        System.getProperty(BenchmarkConfigurationKey.GoogleCloudClientPassword.toString()));

                benchmarkAuthService = new BenchmarkAuthServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkAuthService;
        }
    }

    private static final Iterable<String> SCOPE = Arrays.asList("https://www.googleapis.com/auth/userinfo.profile;https://www.googleapis.com/auth/userinfo.email".split(";"));
    private static final String USER_INFO_URL = "https://www.googleapis.com/oauth2/v1/userinfo";
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private final Configuration configuration;
    GoogleAuthorizationCodeFlow authClient;

    BenchmarkAuthServiceProvider(Configuration configuration) {
        this.configuration = configuration;

        authClient = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), new JacksonFactory(),
                configuration.get(BenchmarkConfigurationKey.GoogleCloudClientId),
                configuration.get(BenchmarkConfigurationKey.GoogleCloudClientPassword),
                Oauth2Scopes.all()).setAccessType("online").setApprovalPrompt("force")
                .build();
    }

    @Override
    public GoogleAuthResponse googleAuth(String authCode, String authUser, String sessionState, String prompt) throws Exception {
        class UserInfo {
            String email;
            String given_name;
            String family_name;
        }

        GoogleAuthResponse googleAuthResponse = new GoogleAuthResponse();

        if (authCode != null) {
            try {
                final GoogleTokenResponse response = authClient.newTokenRequest(authCode).setRedirectUri("https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/webjars/swagger-ui/2.2.5/index.html?url=https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/swagger.json").execute();
                final Credential credential = authClient.createAndStoreCredential(response, null);
                final HttpRequestFactory requestFactory = HTTP_TRANSPORT.createRequestFactory(credential);
                // Make an authenticated request
                final GenericUrl url = new GenericUrl(USER_INFO_URL);
                final HttpRequest request = requestFactory.buildGetRequest(url);
                request.getHeaders().setContentType("application/json");
                final String jsonIdentity = request.execute().parseAsString();
                UserInfo userInfo = gson.fromJson(jsonIdentity, UserInfo.class);
                googleAuthResponse.setUserEmail(userInfo.email);
                googleAuthResponse.setUserGivenName(userInfo.given_name);
                googleAuthResponse.setUserFamilyName(userInfo.family_name);

                return googleAuthResponse;
            } catch (Exception e) {
                e.printStackTrace();
                // for now just fall through ... and provide the login redirect
            }
        }

        String url = authClient.newAuthorizationUrl().setRedirectUri("https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/webjars/swagger-ui/2.2.5/index.html?url=https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0.0-SNAPSHOT/rouplex/swagger.json").build();
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