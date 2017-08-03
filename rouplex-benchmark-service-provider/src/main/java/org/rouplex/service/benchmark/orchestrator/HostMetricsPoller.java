package org.rouplex.service.benchmark.orchestrator;

import com.google.gson.Gson;
import org.apache.http.StatusLine;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.utils.TimeUtils;
import org.rouplex.service.deployment.Host;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class HostMetricsPoller implements Closeable {
    private static final Logger logger = Logger.getLogger(HostMetricsPoller.class.getSimpleName());
    private static final Gson gson = new Gson();

    private final String benchmarkId;
    private final Host host;
    private final Map<ObjectName, String> metricNames;
    private final RestClient esRestClient;
    private final String hostJmxUrl;
    private final JMXConnector jmxConnector;

    HostMetricsPoller(String benchmarkId, Host host, Map<ObjectName, String> metricNames,
                      RestClient esRestClient, Configuration configuration) throws Exception {
        this.benchmarkId = benchmarkId;
        this.host = host;
        this.metricNames = metricNames;
        this.esRestClient = esRestClient;

        hostJmxUrl = String.format("service:jmx:rmi://%1$s:%2$s/jndi/rmi://%1$s:%3$s/jmxrmi",
            host.getPublicIpAddress(),
            configuration.get(OrchestratorServiceProvider.ConfigurationKey.RmiServerPortPlatformForJmx),
            configuration.get(OrchestratorServiceProvider.ConfigurationKey.RmiRegistryPortPlatformForJmx));

        jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(hostJmxUrl));
    }

    String getHostJmxUrl() {
        return hostJmxUrl;
    }

    void monitor() {
        logger.info(String.format("Polling metrics from host [%s]", host.getId()));
        MBeanServerConnection jmxConnection;

        try {
            jmxConnection = jmxConnector.getMBeanServerConnection();
        } catch (Exception e) {
            logger.info(String.format("Failed polling metrics from host [%s]. Cause: %s %s",
                host.getId(), e.getClass().getSimpleName(), e.getMessage()));
            return;
        }

        long now = System.currentTimeMillis();
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("benchmarkId", benchmarkId);
        metrics.put("hostId", host.getId());
        metrics.put("collectionTimestamp", now);
        metrics.put("collectionDateTime", TimeUtils.convertMillisToIsoInstant(now, 0));

        metricNames.entrySet().parallelStream().forEach(metricName -> {
            try {
                metrics.put(metricName.getValue(), jmxConnection.getAttribute(metricName.getKey(), "Count"));
            } catch (Exception re) {
                // getAttribute may fail quite a bit initially ... not logging
            }
        });

        try {
            Response indexResponse = esRestClient.performRequest(
                "POST",
                "/rouplex1/echo-benchmark",
                Collections.<String, String>emptyMap(),
                new NStringEntity(gson.toJson(metrics), ContentType.APPLICATION_JSON));

            StatusLine statusLine = indexResponse.getStatusLine();
            if (statusLine.getStatusCode() < 200 || statusLine.getStatusCode() > 299) {
                logger.warning(String.format(
                    "Could not index benchmark [%s] entry in Elastic Search. Cause: http-code=%s, http-reason=%s",
                    benchmarkId, statusLine.getStatusCode(), statusLine.getReasonPhrase()));
            }
        } catch (Exception re) {
            logger.warning(String.format(
                "Could not index benchmark entry [%s] in Elastic Search. Cause: %s %s",
                benchmarkId, re.getClass().getSimpleName(), re.getMessage()));
        }
    }

    @Override
    public void close() throws IOException {
        jmxConnector.close();
    }
}
