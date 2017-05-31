package org.rouplex.service.benchmark.auth;

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.oauth2.Oauth2Scopes;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.service.benchmark.orchestrator.BenchmarkConfigurationKey;

import java.io.Closeable;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkAuthServiceProvider implements BenchmarkAuthService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkAuthServiceProvider.class.getSimpleName());

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

    private final Configuration configuration;

    BenchmarkAuthServiceProvider(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public String login() throws Exception {
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                new NetHttpTransport(), new JacksonFactory(),
                configuration.get(BenchmarkConfigurationKey.GoogleCloudClientId),
                configuration.get(BenchmarkConfigurationKey.GoogleCloudClientPassword),
                Oauth2Scopes.all()).setAccessType("online").setApprovalPrompt("force")
                .build();
        String url = flow.newAuthorizationUrl().setRedirectUri("https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/webjars/swagger-ui/2.2.5/index.html?url=https://www.rouplex-demo.com:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex/swagger.json").build();
        return url;
    }

    @Override
    public void close() throws IOException {
    }
}