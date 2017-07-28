package org.rouplex.service.benchmark.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.commons.utils.SecurityUtils;
import org.rouplex.commons.utils.TimeUtils;
import org.rouplex.commons.utils.ValidationUtils;
import org.rouplex.service.benchmark.BenchmarkConfigurationKey;
import org.rouplex.service.benchmark.auth.UserInfo;
import org.rouplex.service.benchmark.worker.CreateTcpClientBatchRequest;
import org.rouplex.service.benchmark.worker.CreateTcpServerRequest;
import org.rouplex.service.benchmark.worker.CreateTcpServerResponse;
import org.rouplex.service.deployment.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class OrchestratorServiceProvider implements OrchestratorService, Closeable {
    private static final Logger logger = Logger.getLogger(OrchestratorServiceProvider.class.getSimpleName());
    private static final String JERSEY_CLIENT_CONNECT_TIMEOUT = "jersey.config.client.connectTimeout";
    private static final String JERSEY_CLIENT_READ_TIMEOUT = "jersey.config.client.readTimeout";

    private static OrchestratorServiceProvider benchmarkOrchestratorService;

    public static OrchestratorServiceProvider get() throws Exception {
        synchronized (OrchestratorServiceProvider.class) {
            if (benchmarkOrchestratorService == null) {
                // a shortcut for now, this will discovered automatically when rouplex provides a discovery service
                ConfigurationManager configurationManager = new ConfigurationManager();

                configurationManager.putConfigurationEntry( // poor man's authorization
                    BenchmarkConfigurationKey.AuthorizedPrincipals, "andimullaraj@gmail.com,silvanatase@gmail.com");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.WorkerImageId, "ami-00cdd779");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.WorkerInstanceProfileName, "RouplexBenchmarkWorkerRole");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.WorkerSubnetId, "subnet-9f1784c7");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.WorkerSecurityGroupIds, "sg-bef226c5");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.ServiceHttpDescriptor, "https://%s:443/rest/benchmark");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.WorkerLeaseInMinutes, "30");

                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientId,
                    System.getenv(BenchmarkConfigurationKey.GoogleCloudClientId.toString()));

                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientPassword,
                    System.getenv(BenchmarkConfigurationKey.GoogleCloudClientPassword.toString()));

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.RmiServerPortPlatformForJmx, "1705");

                configurationManager.putConfigurationEntry(
                    BenchmarkConfigurationKey.RmiRegistryPortPlatformForJmx, "1706");

                benchmarkOrchestratorService = new OrchestratorServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkOrchestratorService;
        }
    }

    private final Configuration configuration;
    private final Set<String> authorizedPrincipals;

    // for distributed benchmarking, we need to manage remote benchmark instances
    // rouplex platform will provide a typed version of this soon
    private final Client jaxrsClient;

    // next line is a temporary shortcut, we are instantiating the service
    // provider locally rather than accessing a running version of it
    private final DeploymentService deploymentService = DeploymentServiceProvider.get();

    private final Map<String, TcpEchoBenchmark> benchmarks = new HashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    OrchestratorServiceProvider(Configuration configuration) throws Exception {
        this.configuration = configuration;
        this.authorizedPrincipals = new HashSet<>(Arrays.asList(
            configuration.get(BenchmarkConfigurationKey.AuthorizedPrincipals).split(",")));

        jaxrsClient = createJaxRsClient();
        startMonitoringBenchmarkInstances();
    }

    @Override
    public TcpEchoBenchmark createTcpEchoBenchmark(CreateTcpEchoBenchmarkRequest request) throws Exception {
        return createTcpBenchmark(request, null);
    }

    private void checkAndSanitize(CreateTcpEchoBenchmarkRequest request) {
        ValidationUtils.checkNonNullArg(request.getProvider(), "provider");

        ValidationUtils.checkNonNegativeArg(request.getClientCount(), "clientCount");
        ValidationUtils.checkNonNegativeArg(request.getMinClientLifeMillis(), "minClientLifeMillis");
        ValidationUtils.checkNonNegativeArg(request.getMinDelayMillisBeforeCreatingClient(), "minDelayMillisBeforeCreatingClient");
        ValidationUtils.checkNonNegativeArg(request.getMinDelayMillisBetweenSends(), "minDelayMillisBetweenSends");
        ValidationUtils.checkPositiveArg(request.getMinPayloadSize(), "minPayloadSize");

        ValidationUtils.checkPositiveArgDiff(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis(),
            "minClientLifeMillis", "maxClientLifeMillis");
        ValidationUtils.checkPositiveArgDiff(request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient(),
            "minDelayMillisBeforeCreatingClient", "maxDelayMillisBeforeCreatingClient");
        ValidationUtils.checkPositiveArgDiff(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends(),
            "minDelayMillisBetweenSends", "maxDelayMillisBetweenSends");
        ValidationUtils.checkPositiveArgDiff(request.getMaxPayloadSize() - request.getMinPayloadSize(),
            "minPayloadSize", "maxPayloadSize");

        if (request.getBenchmarkId() == null) {
            request.setBenchmarkId(UUID.randomUUID().toString());
        }

        if (request.getImageId() == null) {
            request.setImageId(configuration.get(BenchmarkConfigurationKey.WorkerImageId));
        }

        if (request.getTcpMemoryAsPercentOfTotal() <= 0) {
            request.setTcpMemoryAsPercentOfTotal(50); // 50%
        }
    }

    public TcpEchoBenchmark createTcpBenchmark(final CreateTcpEchoBenchmarkRequest request, UserInfo userInfo) throws Exception {
        checkAndSanitize(request);

        TcpEchoBenchmark benchmark = new TcpEchoBenchmark();
        benchmark.setBenchmarkId(request.getBenchmarkId());
        benchmark.setServerHostType(request.getServerHostType());
        benchmark.setServerGeoLocation(request.getServerGeoLocation());
        benchmark.setClientsHostType(request.getClientsHostType());
        benchmark.setClientsGeoLocation(request.getClientsGeoLocation());

        benchmark.setImageId(request.getImageId());
        benchmark.setKeyName(request.getKeyName());
        benchmark.setEchoRatio(request.getEchoRatio());
        benchmark.setTcpMemoryAsPercentOfTotal(request.getTcpMemoryAsPercentOfTotal());

        benchmark.setProvider(request.getProvider());
        benchmark.setPort(request.getPort());
        benchmark.setSsl(request.isSsl());
        benchmark.setSocketSendBufferSize(request.getSocketSendBufferSize());
        benchmark.setSocketReceiveBufferSize(request.getSocketReceiveBufferSize());
        benchmark.setBacklog(request.getBacklog());

        benchmark.setClientCount(request.getClientCount());
        benchmark.setClientsPerHost(request.getClientsPerHost());

        benchmark.setMinPayloadSize(request.getMinPayloadSize());
        benchmark.setMaxPayloadSize(request.getMaxPayloadSize());
        benchmark.setMinDelayMillisBetweenSends(request.getMinDelayMillisBetweenSends());
        benchmark.setMaxDelayMillisBetweenSends(request.getMaxDelayMillisBetweenSends());
        benchmark.setMinDelayMillisBeforeCreatingClient(request.getMinDelayMillisBeforeCreatingClient());
        benchmark.setMaxDelayMillisBeforeCreatingClient(request.getMaxDelayMillisBeforeCreatingClient());
        benchmark.setMinClientLifeMillis(request.getMinClientLifeMillis());
        benchmark.setMaxClientLifeMillis(request.getMaxClientLifeMillis());

        int clientHostCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;
        TcpMetricsExpectation clientsExpectation = buildClientsTcpMetricsExpectation(benchmark);
        TcpMetricsExpectation serverExpectation = buildServerTcpMetricsExpectation(clientHostCount, clientsExpectation);

        benchmark.setMetricsAggregation(request.getMetricsAggregation());
        benchmark.setTcpServerExpectation(serverExpectation);
        benchmark.setTcpClientsExpectation(clientsExpectation);

        benchmark.setStartingTimestamp(System.currentTimeMillis());
        benchmark.setExpectedDurationMillis(serverExpectation.getDurationMillis());
        benchmark.setExecutionStatus(ExecutionStatus.STARTING);

        synchronized (this) {
            if (benchmarks.putIfAbsent(benchmark.getBenchmarkId(), benchmark) != null) {
                throw new Exception(String.format("Cannot create tcp echo benchmark [%s]. Cause: already running",
                    request.getBenchmarkId()));
            }
        }

        logger.info(String.format("Starting TcpEchoBenchmark [%s]. 1 ec2 server host and %s ec2 client hosts",
            request.getBenchmarkId(), clientHostCount));

        if (authorizedPrincipals.contains(userInfo.getUserIdAtProvider())) {
            executorService.submit((Runnable) () -> startBenchmark(benchmark));
        }

        return benchmark;
    }

    private void startBenchmark(TcpEchoBenchmark benchmark) {
        try {
            CreateDeploymentRequest createDeploymentRequest = new CreateDeploymentRequest();
            DeploymentConfiguration deploymentConfiguration = new DeploymentConfiguration();
            // granting the deployment an extra 10 minutes, plenty of extra time for bootstrapping the benchmark
            long deploymentDuration = benchmark.getExpectedDurationMillis() + 10 * 60_000;
            String expirationDateTime = TimeUtils
                .convertMillisToIsoInstant(System.currentTimeMillis() + deploymentDuration, 0);
            deploymentConfiguration.setLeaseExpirationDateTime(expirationDateTime);
            createDeploymentRequest.setDeploymentConfiguration(deploymentConfiguration);
            deploymentService.createDeployment(benchmark.getBenchmarkId(), createDeploymentRequest);

            // create server
            CreateEc2ClusterRequest createEc2ClusterRequest = new CreateEc2ClusterRequest();
            createEc2ClusterRequest.setRegion(benchmark.getServerGeoLocation());
            createEc2ClusterRequest.setImageId(benchmark.getImageId());
            createEc2ClusterRequest.setHostType(benchmark.getServerHostType());

            createEc2ClusterRequest.setHostCount(1);
            createEc2ClusterRequest.setIamRole(configuration.get(BenchmarkConfigurationKey.WorkerInstanceProfileName));
            createEc2ClusterRequest.setUserData(Base64.getEncoder().encodeToString(buildSystemTuningScript(
                benchmark.getTcpServerExpectation().getMaxSimultaneousConnections() * 2,
                benchmark.getTcpMemoryAsPercentOfTotal()).getBytes(StandardCharsets.UTF_8)));

            createEc2ClusterRequest.setSubnetId(configuration.get(BenchmarkConfigurationKey.WorkerSubnetId));
            createEc2ClusterRequest.setSecurityGroupIds(Arrays.asList(
                configuration.get(BenchmarkConfigurationKey.WorkerSecurityGroupIds).split(",")));
            createEc2ClusterRequest.setTags(new HashMap<String, String>() {{
                put("Name", "bm-server-" + benchmark.getBenchmarkId());
            }});

            createEc2ClusterRequest.setKeyName(benchmark.getKeyName());
            String serverClusterId = deploymentService
                .createEc2Cluster(benchmark.getBenchmarkId(), createEc2ClusterRequest).getClusterId();

            // create clients cluster
            createEc2ClusterRequest.setRegion(benchmark.getClientsGeoLocation());
            createEc2ClusterRequest.setHostType(benchmark.getClientsHostType());
            createEc2ClusterRequest.setHostCount((benchmark.getClientCount() - 1) / benchmark.getClientsPerHost() + 1);
            createEc2ClusterRequest.setUserData(Base64.getEncoder().encodeToString(buildSystemTuningScript(
                benchmark.getTcpClientsExpectation().getMaxSimultaneousConnections(),
                benchmark.getTcpMemoryAsPercentOfTotal()).getBytes(StandardCharsets.UTF_8)));
            createEc2ClusterRequest.setTags(new HashMap<String, String>() {{
                put("Name", "bm-client-" + benchmark.getBenchmarkId());
            }});

            String clientsClusterId = deploymentService
                .createEc2Cluster(benchmark.getBenchmarkId(), createEc2ClusterRequest).getClusterId();

            // ensure both clusters are ready
            long expirationTimestamp = System.currentTimeMillis() + 10 * 60_000;
            Cluster<Host> serverCluster = ensureDeployed(benchmark.getBenchmarkId(), serverClusterId, expirationTimestamp);
            Cluster<Host> clientsCluster = ensureDeployed(benchmark.getBenchmarkId(), clientsClusterId, expirationTimestamp);

            benchmark.setServerHost(serverCluster.getHosts().values().iterator().next());
            benchmark.setClientHosts(clientsCluster.getHosts().values());

            Thread.sleep(10000); // todo remove this asap after completing the deployment service

            // start server remotely
            CreateTcpServerResponse createTcpServerResponse = jaxrsClient.target(String.format(
                configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), benchmark.getServerHost().getPublicIpAddress()))
                .path("/worker/tcp/servers")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(buildCreateTcpServerRequest(benchmark),
                    MediaType.APPLICATION_JSON), CreateTcpServerResponse.class);

            // start clients remotely
            benchmark.getClientHosts().parallelStream().forEach(h -> {
                jaxrsClient.target(String.format(
                    configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), h.getPublicIpAddress()))
                    .path("/worker/tcp/client-batches")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.entity(buildCreateTcpClientBatchRequest(
                        benchmark, createTcpServerResponse.getHostaddress(), createTcpServerResponse.getPort()),
                        MediaType.APPLICATION_JSON));
            });

            // create jconsole link
            StringBuilder jconsoleJmxLink = new StringBuilder();
            addToJmxLink(jconsoleJmxLink, benchmark.getServerHost().getPublicIpAddress());
            for (Host host : benchmark.getClientHosts()) {
                addToJmxLink(jconsoleJmxLink, host.getPublicIpAddress());
            }
            benchmark.setJconsoleJmxLink("jconsole" + jconsoleJmxLink.toString());

            // Sanction running state
            benchmark.setStartedTimestamp(System.currentTimeMillis());
            benchmark.setExecutionStatus(ExecutionStatus.RUNNING);
        } catch (Exception e) {
            benchmark.setExecutionStatus(ExecutionStatus.FAILED);
            benchmark.setException(String.format("%s: %s", e.getClass().getSimpleName(), e.getMessage()));
        }
    }

    private <H extends Host> Cluster<H> ensureDeployed(String deploymentId, String clusterId, long expirationTimestamp) throws Exception {
        while (System.currentTimeMillis() < expirationTimestamp) {
            Cluster<H> cluster = deploymentService.getCluster(deploymentId, clusterId);

            if (cluster.getHosts().values().parallelStream()
                .filter(h -> h.getDeploymentState() == null || h.getPublicIpAddress() == null)
                .count() == 0) {
                return cluster;
            }

            Thread.sleep(1000);
        }

        throw new TimeoutException("Timed out waiting for the cluster to be deployed");
    }

    @Override
    public Set<String> listTcpEchoBenchmarks(String includePublic) throws Exception {
        return benchmarks.keySet();
    }

    @Override
    public TcpEchoBenchmark getTcpEchoBenchmark(String benchmarkId) throws Exception {
        TcpEchoBenchmark benchmark = benchmarks.get(benchmarkId);

        if (benchmark == null) {
            throw new Exception(String.format("TcpEchoBenchmark [%s] not found", benchmarkId));
        }

        return benchmark;
    }

    @Override
    public void close() throws IOException {
        executorService.shutdown();
        synchronized (executorService) {
            executorService.notifyAll();
        }
    }

    private Client createJaxRsClient() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);

        return ClientBuilder.newBuilder()
            .property(JERSEY_CLIENT_CONNECT_TIMEOUT, 12000)
            .property(JERSEY_CLIENT_READ_TIMEOUT, 12000)
            .register(provider)
            .sslContext(SecurityUtils.buildRelaxedSSLContext())
            .hostnameVerifier((s, sslSession) -> true)
            .build();
    }

    private void startMonitoringBenchmarkInstances() {
        executorService.submit((Runnable) () -> {
            while (!executorService.isShutdown()) {
                // we note timeStart since the loop may take time to execute
                long timeStart = System.currentTimeMillis();

                try {
                    for (TcpEchoBenchmark benchmark : benchmarks.values()) {
                        monitorBenchmark(benchmark);
                        // todo add benchmark removal functionality
                    }
                } catch (RuntimeException re) {
                    // ConcurrentModification ... will be retried in one minute anyway
                }

                // update leases once a minute (or less often occasionally)
                long waitMillis = timeStart + 60_000 - System.currentTimeMillis();
                if (waitMillis > 0) {
                    try {
                        synchronized (executorService) {
                            executorService.wait(waitMillis);
                        }
                    } catch (InterruptedException ie) {
                        break;
                    }
                }
            }
        });
    }

    private <T> void monitorBenchmark(TcpEchoBenchmark benchmark) {
        // read metrics from jmx and add them to es
    }

    private String buildSystemTuningScript(int maxFileDescriptors, int tcpAsPercentOfTotalMemory) {
        if (maxFileDescriptors < 1024) {
            maxFileDescriptors = 1024;
        }

        // consider net.core.somaxconn = 1000 as well
        String scriptTemplate = "#!/bin/bash\n\n" +

            "configure_system_limits() {\n" +
            "\techo \"=== Rouplex === Allowing more open file descriptors, tcp sockets, tcp memory\"\n" +
            "\techo %1$s > /proc/sys/fs/nr_open\n" +
            "\techo \"* hard nofile %1$s\" | tee -a /etc/security/limits.conf\n" +
            "\techo \"* soft nofile %1$s\" | tee -a /etc/security/limits.conf\n" +
            "\techo \"\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"# Allow use of 64000 ports from 1100 to 65100\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"net.ipv4.ip_local_port_range = 1100 65100\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"\" | tee -a /etc/sysctl.conf\n" +
            "\ttotal_mem_kb=`free -t | grep Mem | awk '{print $2}'`\n" +
            "\ttcp_mem_in_pages=$(( total_mem_kb * %2$s / 100 / 4))\n" +
            "\techo \"# Setup bigger tcp memory\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"net.ipv4.tcp_mem = 383865 tcp_mem_in_pages tcp_mem_in_pages\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"# Setup greater open files\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"fs.file-max = %1$s\" | tee -a /etc/sysctl.conf\n\n" +
            "\tsysctl -p\n" +
            "}\n\n" +

            "configure_system_limits\n" +
            "service tomcat restart\n";

        return String.format(scriptTemplate, maxFileDescriptors, tcpAsPercentOfTotalMemory);
    }

    private CreateTcpServerRequest buildCreateTcpServerRequest(TcpEchoBenchmark benchmark) {
        CreateTcpServerRequest request = new CreateTcpServerRequest();

        request.setProvider(benchmark.getProvider());
        request.setHostname(benchmark.getServerHost().getPrivateIpAddress());
        request.setPort(benchmark.getPort());
        request.setSsl(benchmark.isSsl());
        request.setSocketReceiveBufferSize(benchmark.getSocketReceiveBufferSize());
        request.setSocketSendBufferSize(benchmark.getSocketSendBufferSize());
        request.setBacklog(benchmark.getBacklog());
        request.setMetricsAggregation(benchmark.getMetricsAggregation());

        return request;
    }

    private CreateTcpClientBatchRequest buildCreateTcpClientBatchRequest(
        TcpEchoBenchmark benchmark, String remoteIpAddress, int remoteIpPort) {

        CreateTcpClientBatchRequest request = new CreateTcpClientBatchRequest();
        request.setProvider(benchmark.getProvider());
        request.setHostname(remoteIpAddress);
        request.setPort(remoteIpPort);
        request.setSsl(benchmark.isSsl());

        request.setClientCount(benchmark.getClientsPerHost());
        request.setMinPayloadSize(benchmark.getMinPayloadSize());
        request.setMaxPayloadSize(benchmark.getMaxPayloadSize());
        request.setMinDelayMillisBetweenSends(benchmark.getMinDelayMillisBetweenSends());
        request.setMaxDelayMillisBetweenSends(benchmark.getMaxDelayMillisBetweenSends());
        request.setMinDelayMillisBeforeCreatingClient(benchmark.getMinDelayMillisBeforeCreatingClient());
        request.setMaxDelayMillisBeforeCreatingClient(benchmark.getMaxDelayMillisBeforeCreatingClient());
        request.setMinClientLifeMillis(benchmark.getMinClientLifeMillis());
        request.setMaxClientLifeMillis(benchmark.getMaxClientLifeMillis());

        request.setSocketReceiveBufferSize(benchmark.getSocketReceiveBufferSize());
        request.setSocketSendBufferSize(benchmark.getSocketSendBufferSize());
        request.setMetricsAggregation(benchmark.getMetricsAggregation());

        return request;
    }

    private TcpMetricsExpectation buildClientsTcpMetricsExpectation(TcpEchoBenchmark benchmark) {
        int durationMillis = benchmark.getMaxDelayMillisBeforeCreatingClient() + benchmark.getMaxClientLifeMillis();
        int connectionRampUpMillis = benchmark.getMaxDelayMillisBeforeCreatingClient() - benchmark.getMinDelayMillisBeforeCreatingClient();
        int clientAvgLifetimeMillis = (benchmark.getMaxClientLifeMillis() + benchmark.getMinClientLifeMillis()) / 2;
        int rampUpAsMillis = Math.min(connectionRampUpMillis, clientAvgLifetimeMillis);

        TcpMetricsExpectation clientsExpectation = new TcpMetricsExpectation();
        clientsExpectation.setDurationMillis(durationMillis);
        clientsExpectation.setRampUpInMillis(rampUpAsMillis);
        clientsExpectation.setConnectionsPerSecond((double) benchmark.getClientsPerHost() * 1000 / connectionRampUpMillis);
        clientsExpectation.setMaxSimultaneousConnections(
            (int) (((long) benchmark.getClientsPerHost() * rampUpAsMillis) / connectionRampUpMillis));

        long avgPayloadSize = (benchmark.getMaxPayloadSize() - 1 + benchmark.getMinPayloadSize()) / 2;
        long avgPayloadPeriodMillis = (benchmark.getMaxDelayMillisBetweenSends() - 1 + benchmark.getMinDelayMillisBetweenSends()) / 2;
        long transferSpeedBps = avgPayloadSize * clientsExpectation.getMaxSimultaneousConnections() / avgPayloadPeriodMillis * 8 * 1000;

        clientsExpectation.setMaxUploadSpeedInBitsPerSecond(transferSpeedBps);
        clientsExpectation.setMaxDownloadSpeedInBitsPerSecond(transferSpeedBps); // todo handle echoRatio of type "x:y"

        return clientsExpectation;
    }

    private TcpMetricsExpectation buildServerTcpMetricsExpectation(
        int clientHostsCount, TcpMetricsExpectation clientsExpectation) {

        TcpMetricsExpectation serverExpectation = new TcpMetricsExpectation();
        serverExpectation.setDurationMillis(clientsExpectation.getDurationMillis());
        serverExpectation.setRampUpInMillis(clientsExpectation.getRampUpInMillis());
        serverExpectation.setConnectionsPerSecond(clientHostsCount * clientsExpectation.getConnectionsPerSecond());
        serverExpectation.setMaxSimultaneousConnections(clientHostsCount * clientsExpectation.getMaxSimultaneousConnections());

        long transferSpeedBps = clientHostsCount * clientsExpectation.getMaxUploadSpeedInBitsPerSecond();
        serverExpectation.setMaxUploadSpeedInBitsPerSecond(transferSpeedBps);
        serverExpectation.setMaxDownloadSpeedInBitsPerSecond(transferSpeedBps);

        return serverExpectation;
    }

    private String convertBpsUp(long bpsValue) {
        String[] units = {" Bps", " Kbps", " Mbps", " Gbps", " Tbps"};

        int index = 0;
        while (bpsValue >= 10000) {
            index++;
            bpsValue /= 1000;
        }

        return bpsValue + units[index];
    }

    private void addToJmxLink(StringBuilder jmxLink, String ipAddress) {
        jmxLink
            .append(" service:jmx:rmi://").append(ipAddress).append(":")
            .append(configuration.get(BenchmarkConfigurationKey.RmiServerPortPlatformForJmx))
            .append("/jndi/rmi://").append(ipAddress).append(":")
            .append(configuration.get(BenchmarkConfigurationKey.RmiRegistryPortPlatformForJmx))
            .append("/jmxrmi");
    }
}
