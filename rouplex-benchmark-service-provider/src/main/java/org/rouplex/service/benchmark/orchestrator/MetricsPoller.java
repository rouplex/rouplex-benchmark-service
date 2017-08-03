package org.rouplex.service.benchmark.orchestrator;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.rouplex.commons.annotations.GuardedBy;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.service.deployment.Host;

import javax.management.ObjectName;
import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class MetricsPoller implements Closeable {
    private static final Logger logger = Logger.getLogger(MetricsPoller.class.getSimpleName());

    private static final String[] providers = new String[]{
        Provider.CLASSIC_NIO.toString() + ".P",
        Provider.ROUPLEX_NIOSSL.toString() + ".S"
    };

    private static final String[] actors = new String[]{
        "EchoRequester",
        "EchoResponder"
    };

    private static final String[] metrics = new String[]{
        "connection.established",
        "connection.live",
        "connectionTime",
        "disconnectedOk",
        "disconnectedKo",
        "sentBytes",
        "sentEos",
        "sendFailures",
        "sentSizes",
        "sendBufferFilled",
        "discardedSendBytes",
        "sendPauseTime",
        "receivedBytes",
        "receivedEos",
        "receivedDisconnect",
        "receivedSizes"
    };

    private final Object lock = new Object();
    private final Configuration configuration;
    private final ExecutorService executorService;
    private final Map<ObjectName, String> metricNames = new HashMap<>();
    private final RestClient esRestClient;
    private final ConcurrentMap<String, HostMetricsPoller> hostMetricsPollers = new ConcurrentHashMap<>();

    @GuardedBy("lock")
    private boolean closed;

    public MetricsPoller(Configuration configuration, ExecutorService executorService) {
        this.configuration = configuration;
        this.executorService = executorService;

        for (String provider : providers) {
            for (String actor : actors) {
                for (String metric : metrics) {
                    addMetricName(String.format("%s.%s.A:A::A:A.%s", provider, actor, metric));
                }
            }

            for (int bucket = 1; bucket < 7; bucket++) {
                addMetricName(String.format(
                    "%s.EchoRequester.A:A::A:A.connectionTime.millisBucket.%s",
                    provider, "100000".substring(0, bucket)));
            }
        }

        esRestClient = RestClient.builder(new HttpHost(
            configuration.get(OrchestratorServiceProvider.ConfigurationKey.ElasticSearchHost),
            configuration.getAsInteger(OrchestratorServiceProvider.ConfigurationKey.ElasticSearchPort),
            configuration.get(OrchestratorServiceProvider.ConfigurationKey.ElasticSearchProtocol))
        ).build();

        startPollingMetrics();
    }

    private void addMetricName(String metricName) {
        String fqName = String.format("rouplex-benchmark:name=\"%s\"", metricName);
        try {
            metricNames.put(new ObjectName(fqName), metricName.replaceAll("\\.", "-"));
        } catch (Exception e) {
            logger.severe(String.format("Could not create ObjectName for metric [%s]. Cause: %s %s",
                metricName, e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private void startPollingMetrics() {
        executorService.submit((Runnable) () -> {
            while (true) {
                // we note timeStart since the loop may take time to execute
                long timeStart = System.currentTimeMillis();
                logger.info("Starting metrics poll for all benchmarks");

                try {
                    for (Map.Entry<String, HostMetricsPoller> entry : hostMetricsPollers.entrySet()) {
                        entry.getValue().monitor();
                    }
                } catch (RuntimeException re) {
                    // ConcurrentModification ... will be retried in one minute anyway
                    logger.info(String.format("Failed monitoring metrics (will be retried in one minute). Cause: %s %s",
                        re.getClass().getSimpleName(), re.getMessage()));
                }

                // update leases once a minute (or less often occasionally)
                long waitMillis = timeStart + 60_000 - System.currentTimeMillis();
                synchronized (lock) {
                    closed |= executorService.isShutdown();
                    if (closed) {
                        break;
                    }

                    if (waitMillis > 0) {
                        try {
                            lock.wait(waitMillis);
                        } catch (InterruptedException ie) {
                            closed = true;
                            break;
                        }
                    }
                }
            }

            // No risk of ConcurrentModified since no changes are allowed in closed state (in which we are)
            for (Map.Entry<String, HostMetricsPoller> entry : hostMetricsPollers.entrySet()) {
                try {
                    entry.getValue().close();
                } catch (Exception e) {
                    logger.warning(String.format("Failed polling metrics from host [%s]. Cause: %s %s",
                        entry.getKey(), e.getClass().getSimpleName(), e.getMessage()));
                }
            }
        });
    }

    void addBenchmarkToMonitor(TcpEchoBenchmark tcpEchoBenchmark) throws Exception {
        synchronized (lock) {
            if (closed) {
                throw new Exception("Closed");
            }

            // create jconsole link
            StringBuilder jconsoleJmxLink = new StringBuilder();

            String hostJmxUrl = addHostToMonitor(tcpEchoBenchmark.getId(), tcpEchoBenchmark.getServerHost());
            jconsoleJmxLink.append(" ").append(hostJmxUrl);

            for (Host host : tcpEchoBenchmark.getClientHosts()) {
                hostJmxUrl = addHostToMonitor(tcpEchoBenchmark.getId(), host);
                jconsoleJmxLink.append(" ").append(hostJmxUrl);
            }

            tcpEchoBenchmark.setJconsoleJmxLink("jconsole" + jconsoleJmxLink.toString());
        }
    }

    private String addHostToMonitor(String benchmarkId, Host host) throws Exception {
        HostMetricsPoller hostMetricsPoller = new HostMetricsPoller(
            benchmarkId, host, metricNames, esRestClient, configuration);

        hostMetricsPollers.put(host.getId(), hostMetricsPoller);
        return hostMetricsPoller.getHostJmxUrl();
    }

    @Override
    public void close() throws IOException {
        synchronized (lock) {
            closed = true;
            lock.notifyAll();
        }
    }
}
