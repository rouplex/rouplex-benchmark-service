package org.rouplex.service.benchmark.orchestrator;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.commons.configuration.ConfigurationManager;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.service.benchmark.BenchmarkConfigurationKey;
import org.rouplex.service.benchmark.Util;
import org.rouplex.service.benchmark.auth.UserInfo;
import org.rouplex.service.benchmark.management.*;
import org.rouplex.service.benchmark.worker.StartTcpClientsRequest;
import org.rouplex.service.benchmark.worker.StartTcpServerRequest;
import org.rouplex.service.benchmark.worker.StartTcpServerResponse;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class OrchestratorServiceProvider implements OrchestratorService, Closeable {
    private static final Logger logger = Logger.getLogger(OrchestratorServiceProvider.class.getSimpleName());
    private static final String JERSEY_CLIENT_READ_TIMEOUT = "jersey.config.client.readTimeout";
    private static final String JERSEY_CLIENT_CONNECT_TIMEOUT = "jersey.config.client.connectTimeout";

    private static OrchestratorServiceProvider benchmarkOrchestratorService;

    public static OrchestratorServiceProvider get() throws Exception {
        synchronized (OrchestratorServiceProvider.class) {
            if (benchmarkOrchestratorService == null) {
                // a shortcut for now, this will discovered automatically when rouplex provides a discovery service
                ConfigurationManager configurationManager = new ConfigurationManager();
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.WorkerInstanceProfileName,
                    "RouplexBenchmarkWorkerRole");
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.ServiceHttpDescriptor,
                    "https://%s:443/rouplex/benchmark");
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.WorkerLeaseInMinutes, "30");

                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientId,
                    System.getenv(BenchmarkConfigurationKey.GoogleCloudClientId.toString()));

                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.GoogleCloudClientPassword,
                    System.getenv(BenchmarkConfigurationKey.GoogleCloudClientPassword.toString()));

                benchmarkOrchestratorService = new OrchestratorServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkOrchestratorService;
        }
    }

    // for distributed benchmarking, we need to manage ec2 instance lifecycle
    private final Map<Region, AmazonEC2> amazonEC2Clients = new HashMap<Region, AmazonEC2>();

    // for distributed benchmarking, we need to manage remote benchmark instances
    private final Client jaxrsClient; // rouplex platform will provide a specific version of this soon

    private final Map<String, BenchmarkDescriptor<?>> tcpBenchmarks = new HashMap<>();

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Configuration configuration;

    OrchestratorServiceProvider(Configuration configuration) throws Exception {
        this.configuration = configuration;
        jaxrsClient = createJaxRsClient();
        startMonitoringBenchmarkInstances();
    }

    private void checkAndSanitize(StartTcpBenchmarkRequest request) {
        Util.checkNonNullArg(request.getProvider(), "Provider");

        Util.checkNonNegativeArg(request.getClientCount(), "ClientCount");
        Util.checkNonNegativeArg(request.getMinClientLifeMillis(), "MinClientLifeMillis");
        Util.checkNonNegativeArg(request.getMinDelayMillisBeforeCreatingClient(), "MinDelayMillisBeforeCreatingClient");
        Util.checkNonNegativeArg(request.getMinDelayMillisBetweenSends(), "MinDelayMillisBetweenSends");
        Util.checkPositiveArg(request.getMinPayloadSize(), "MinPayloadSize");

        Util.checkPositiveArgDiff(request.getMaxClientLifeMillis() - request.getMinClientLifeMillis(),
            "MinClientLifeMillis", "MaxClientLifeMillis");
        Util.checkPositiveArgDiff(request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient(),
            "MinDelayMillisBeforeCreatingClient", "MaxDelayMillisBeforeCreatingClient");
        Util.checkPositiveArgDiff(request.getMaxDelayMillisBetweenSends() - request.getMinDelayMillisBetweenSends(),
            "MinDelayMillisBetweenSends", "MaxDelayMillisBetweenSends");
        Util.checkPositiveArgDiff(request.getMaxPayloadSize() - request.getMinPayloadSize(),
            "MinPayloadSize", "MaxPayloadSize");

        if (request.getBenchmarkRequestId() == null) {
            request.setBenchmarkRequestId(UUID.randomUUID().toString());
        }

        if (request.getImageId() == null) {
            request.setImageId(EC2MetadataUtils.getAmiId());
        }

        if (request.getTcpMemoryAsPercentOfTotal() <= 0) {
            request.setTcpMemoryAsPercentOfTotal(50); // 50%
        }
    }

    @Override
    public StartTcpBenchmarkResponse startTcpBenchmark(StartTcpBenchmarkRequest request) throws Exception {
        return startTcpBenchmark(request, null);
    }

    public StartTcpBenchmarkResponse startTcpBenchmark(final StartTcpBenchmarkRequest request, UserInfo userInfo) throws Exception {
        checkAndSanitize(request);

        int clientHostCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;
        TcpMetricsExpectation clientsExpectation = buildClientsTcpMetricsExpectation(request);
        long benchmarkExpiration = Util.convertIsoInstantToMillis(clientsExpectation.getFinishAsIsoInstant()) + 5 * 60 * 1000;

        synchronized (this) {
            // the underlying host just became an orchestrator and is granted unlimited lease
            ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
            configureServiceRequest.setLeaseEndAsIsoInstant(Util.convertMillisToIsoInstant(Long.MAX_VALUE));
            ManagementServiceProvider.get().configureService(configureServiceRequest);

            if (tcpBenchmarks.putIfAbsent(request.getBenchmarkRequestId(),
                new BenchmarkDescriptor(request, benchmarkExpiration)) != null) {
                throw new Exception(String.format("Distributed tcp benchmark [%s] already running",
                    request.getBenchmarkRequestId()));
            }
        }

        logger.info(String.format(
            "Starting Distributed Benchmark [%s]. 1 ec2 server host and %s ec2 client hosts",
            request.getBenchmarkRequestId(), clientHostCount));

        // temp hack preventing others from abusing the service
        if ("andimullaraj@gmail.com".equals(userInfo.getUserIdAtProvider())) {
            executorService.submit((Runnable) () -> start(request.getBenchmarkRequestId()));
        }

        StartTcpBenchmarkResponse response = new StartTcpBenchmarkResponse();
        response.setTcpClientsExpectation(clientsExpectation);
        response.setTcpServerExpectation(buildServerTcpMetricsExpectation(clientHostCount, clientsExpectation));

        return response;
    }

    void start(String benchmarkRequestId) {
        BenchmarkDescriptor<Boolean> benchmarkDescriptor = (BenchmarkDescriptor<Boolean>) tcpBenchmarks.get(benchmarkRequestId);

        try {
            StartTcpBenchmarkRequest request = benchmarkDescriptor.getStartTcpBenchmarkRequest();
            Map<String, InstanceDescriptor<Boolean>> serverDescriptors = startRunningInstances(request, true);
            Map<String, InstanceDescriptor<Boolean>> clientDescriptors = startRunningInstances(request, false);

            ensureEC2InstancesRunning(serverDescriptors);
            ensureBenchmarkServiceRunning(serverDescriptors.values());
            InstanceDescriptor serverDescriptor = serverDescriptors.values().iterator().next();
            benchmarkDescriptor.getInstanceDescriptors().putAll(serverDescriptors);

            ensureEC2InstancesRunning(clientDescriptors);
            ensureBenchmarkServiceRunning(clientDescriptors.values());
            benchmarkDescriptor.getInstanceDescriptors().putAll(clientDescriptors);

            // start server remotely
            StartTcpServerResponse startTcpServerResponse = jaxrsClient.target(String.format(
                configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), serverDescriptor.getPublicIpAddress()))
                .path("/worker/tcp/server/start")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(buildStartTcpServerRequest(request, serverDescriptor.getPrivateIpAddress()),
                    MediaType.APPLICATION_JSON), StartTcpServerResponse.class);

            // start clients remotely
            Entity<StartTcpClientsRequest> startTcpClientsEntity = Entity.entity(buildStartTcpClientsRequestRequest(
                request, startTcpServerResponse.getHostaddress(), startTcpServerResponse.getPort()), MediaType.APPLICATION_JSON);

            for (InstanceDescriptor clientDescriptor : clientDescriptors.values()) {
                jaxrsClient.target(String.format(
                    configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), clientDescriptor.getPublicIpAddress()))
                    .path("/worker/tcp/clients/start")
                    .request(MediaType.APPLICATION_JSON)
                    .post(startTcpClientsEntity);
            }
        } catch (Exception e) {
            benchmarkDescriptor.setEventualException(e);
        }
    }

    // a shortcut here targeting just ec2 instances for now
    private Map<String, InstanceDescriptor<Boolean>> startRunningInstances(
        StartTcpBenchmarkRequest request, boolean server) throws Exception {

        int clientHostCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;
        int maxSimultaneousConnectionsPerClient =
            buildClientsTcpMetricsExpectation(request).getMaxSimultaneousConnections();

        int maxFileDescriptors;
        int instanceCount;
        String tagPrefix;

        if (server) {
            maxFileDescriptors = maxSimultaneousConnectionsPerClient * clientHostCount * 2;
            instanceCount = 1;
            tagPrefix = "server-";
        } else {
            maxFileDescriptors = maxSimultaneousConnectionsPerClient;
            instanceCount = clientHostCount;
            tagPrefix = "client-";
        }

        String data = Base64.getEncoder().encodeToString(buildSystemTuningScript(maxFileDescriptors,
            request.getTcpMemoryAsPercentOfTotal()).getBytes(StandardCharsets.UTF_8));

        return startRunningEC2Instances(getEC2Region(request, server), request.getImageId(),
            getEC2InstanceType(request, server), instanceCount, request.getKeyName(), data,
            tagPrefix + request.getBenchmarkRequestId(), server);
    }

    private <T> Map<String, InstanceDescriptor<T>> startRunningEC2Instances(
        Region ec2Region, String imageId, InstanceType instanceType,
        int count, String sshKeyName, String userData, String tag, T metadata) throws IOException {

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
            .withImageId(imageId)
            .withInstanceType(instanceType)
            .withMinCount(count)
            .withMaxCount(count)
            .withIamInstanceProfile(new IamInstanceProfileSpecification()
                .withName(configuration.get(BenchmarkConfigurationKey.WorkerInstanceProfileName)))
            .withSubnetId("subnet-9f1784c7") // auto create this in a later impl
            .withSecurityGroupIds("sg-bef226c5") // auto create this in a later impl
            .withKeyName(sshKeyName)
            .withUserData(userData)
            .withTagSpecifications(new TagSpecification()
                .withResourceType(ResourceType.Instance).withTags(new Tag("Name", tag)));

        Map<String, InstanceDescriptor<T>> instanceDescriptors = new HashMap<>();
        for (Instance instance : getAmazonEc2Client(ec2Region)
            .runInstances(runInstancesRequest).getReservation().getInstances()) {

            InstanceDescriptor<T> instanceDescriptor = new InstanceDescriptor<>();
            instanceDescriptor.setMetadata(metadata);
            instanceDescriptors.put(instance.getInstanceId(), instanceDescriptor);
            instanceDescriptor.setPrivateIpAddress(instance.getPrivateIpAddress());
            instanceDescriptor.setImageId(instance.getImageId());
            instanceDescriptor.setEc2Region(ec2Region);
        }

        return instanceDescriptors;
    }

    @Override
    public DescribeTcpBenchmarkResponse describeTcpBenchmark(DescribeTcpBenchmarkRequest request1) throws Exception {
        BenchmarkDescriptor<Boolean> benchmarkDescriptor = (BenchmarkDescriptor<Boolean>) tcpBenchmarks.get(request1.getBenchmarkRequestId());
        StartTcpBenchmarkRequest request = benchmarkDescriptor.getStartTcpBenchmarkRequest();
        int clientHostCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;

        TcpMetricsExpectation clientsExpectation = buildClientsTcpMetricsExpectation(request);

        TcpMetricsExpectation serverExpectation = buildServerTcpMetricsExpectation(clientHostCount, clientsExpectation);

        // prepare and return response
        DescribeTcpBenchmarkResponse response = new DescribeTcpBenchmarkResponse();
        response.setBenchmarkRequestId(request.getBenchmarkRequestId());
        response.setImageId(request.getImageId());

        InstanceDescriptor<Boolean> serverDescriptor = null;
        List<String> clientIpAddresses = new ArrayList<>();
        for (InstanceDescriptor<Boolean> instanceDescriptor : benchmarkDescriptor.getInstanceDescriptors().values()) {
            if (instanceDescriptor.getMetadata()) {
                serverDescriptor = instanceDescriptor;
                response.setServerHostType(HostType.fromString(instanceDescriptor.getEc2InstanceType().toString()));
                response.setServerGeoLocation(GeoLocation.fromString(instanceDescriptor.getEc2Region().getRegionName()));
                response.setServerIpAddress(instanceDescriptor.getPublicIpAddress());
            } else {
                clientIpAddresses.add(instanceDescriptor.getPublicIpAddress());
                response.setClientsHostType(HostType.fromString(instanceDescriptor.getEc2InstanceType().toString()));
                response.setClientsGeoLocation(GeoLocation.fromString(instanceDescriptor.getEc2Region().getRegionName()));
            }
        }

        StringBuilder jconsoleJmxLink = new StringBuilder("jconsole");
        if (serverDescriptor != null) { // should never be null anyway
            addToJmxLink(jconsoleJmxLink, serverDescriptor.publicIpAddress);
        }
        for (String ipAddress : clientIpAddresses) {
            addToJmxLink(jconsoleJmxLink, ipAddress);
        }

        response.setClientIpAddresses(clientIpAddresses);
        response.setJconsoleJmxLink(jconsoleJmxLink.toString());

        response.setTcpClientsExpectation(clientsExpectation);
        response.setTcpServerExpectation(serverExpectation);

        return response;
    }

    private void addToJmxLink(StringBuilder jmxLink, String ipAddress) {
        jmxLink.append(String.format(" service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi", ipAddress, ipAddress));
    }

    private Client createJaxRsClient() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);

        JacksonJaxbJsonProvider provider = new JacksonJaxbJsonProvider();
        provider.setMapper(mapper);

        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
        clientBuilder.register(provider);
        clientBuilder.sslContext(RouplexTcpClient.buildRelaxedSSLContext());
        clientBuilder.hostnameVerifier(new HostnameVerifier() {
            @Override
            public boolean verify(String s, SSLSession sslSession) {
                return true;
            }
        });

        return clientBuilder.build();
    }

    private AmazonEC2 getAmazonEc2Client(Region region) {
        synchronized (amazonEC2Clients) {
            AmazonEC2 amazonEC2Client = amazonEC2Clients.get(region);

            if (amazonEC2Client == null) {
                amazonEC2Client = AmazonEC2ClientBuilder.standard()
                    .withRegion(Regions.fromName(region.getRegionName())).build();

                amazonEC2Clients.put(region, amazonEC2Client);
            }

            return amazonEC2Client;
        }
    }

    private void startMonitoringBenchmarkInstances() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {

                while (!executorService.isShutdown()) {
                    // we note timeStart since the loop may take time to execute
                    long timeStart = System.currentTimeMillis();

                    try {
                        for (Map.Entry<String, BenchmarkDescriptor<?>> descriptorEntry : tcpBenchmarks.entrySet()) {
                            monitorBenchmark(descriptorEntry.getValue());
                            // remove later
                        }
                    } catch (RuntimeException re) {
                        // ConcurrentModification ... will be retried in one minute anyway
                    }

                    // update leases once a minute (or less often occasionally)
                    long waitMillis = timeStart + 60 * 1000 - System.currentTimeMillis();
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
            }
        });
    }

    private <T> void monitorBenchmark(BenchmarkDescriptor<T> benchmarkDescriptor) {
        if (System.currentTimeMillis() > benchmarkDescriptor.getExpirationTimestamp()) {
            for (Map.Entry<String, InstanceDescriptor<T>> instanceDescriptor : benchmarkDescriptor.getInstanceDescriptors().entrySet()) {
                logger.info(String.format(
                    "Terminating instance [%s] at ip [%s]. Cause: Benchmark has reached maximum time",
                    instanceDescriptor.getKey(), instanceDescriptor.getValue().getPublicIpAddress()));

                tagAndTerminateEC2Instance(getAmazonEc2Client(instanceDescriptor.getValue().getEc2Region()),
                    instanceDescriptor.getKey());
            }

            benchmarkDescriptor.getInstanceDescriptors().clear();
        } else {
            int workerLeaseInMinutes = configuration.getAsInteger(BenchmarkConfigurationKey.WorkerLeaseInMinutes);
            long workerExpiration = System.currentTimeMillis() + workerLeaseInMinutes * 60 * 1000;
            Collection<String> expiredInstanceIds = new ArrayList<String>();

            for (Map.Entry<String, InstanceDescriptor<T>> entry : benchmarkDescriptor.getInstanceDescriptors().entrySet()) {
                InstanceDescriptor instanceDescriptor = entry.getValue();
                try {
                    ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
                    configureServiceRequest.setLeaseEndAsIsoInstant(Util.convertMillisToIsoInstant(workerExpiration));

                    jaxrsClient.target(String.format(
                        configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor),
                        instanceDescriptor.getPublicIpAddress()))
                        .path("/management/service/configuration")
                        .property(JERSEY_CLIENT_CONNECT_TIMEOUT, 2000)
                        .property(JERSEY_CLIENT_READ_TIMEOUT, 10000)
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.entity(configureServiceRequest, MediaType.APPLICATION_JSON));

                    instanceDescriptor.setExpirationTimestamp(workerExpiration);
                } catch (Exception e) {
                    if (System.currentTimeMillis() > instanceDescriptor.getExpirationTimestamp()) {
                        expiredInstanceIds.add(entry.getKey());

                        logger.severe(String.format("Terminating instance [%s] at ip [%s]. " +
                                "Cause: Could not contact it to announce its new lease in the last %s minutes",
                            entry.getKey(), instanceDescriptor.getPublicIpAddress(), workerLeaseInMinutes));

                        tagAndTerminateEC2Instance(getAmazonEc2Client(
                            instanceDescriptor.getEc2Region()), entry.getKey());
                    }
                }
            }

            for (String instanceId : expiredInstanceIds) {
                benchmarkDescriptor.getInstanceDescriptors().remove(instanceId);
            }
        }
    }

    private Region getEC2Region(StartTcpBenchmarkRequest request, boolean server) {
        try {
            GeoLocation geoLocation = server ? request.getServerGeoLocation() : request.getClientsGeoLocation();
            return new Region().withRegionName(geoLocation.toString());
        } catch (Exception e) {
            return new Region().withRegionName(Regions.US_WEST_2.toString());
        }
    }

    private HostType getHostType(StartTcpBenchmarkRequest request, boolean server) {
        HostType hostType = server
            ? request.getServerHostType() : request.getClientsHostType();

        return hostType != null ? hostType : server ? HostType.EC2_M4Large : HostType.EC2_T2Micro;
    }

    private InstanceType getEC2InstanceType(StartTcpBenchmarkRequest request, boolean server) {
        return InstanceType.fromValue(getHostType(request, server).toString());
    }

    private void tagAndTerminateEC2Instance(AmazonEC2 amazonEC2, String instanceId) {
        amazonEC2.createTags(new CreateTagsRequest().withResources(instanceId)
            .withTags(new Tag().withKey("State").withValue("Stopped by Benchmark Orchestrator")));

        amazonEC2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceId));
    }

    private String buildSystemTuningScript(int maxFileDescriptors, int tcpAsPercentOfTotalMemory) {
        // consider net.core.somaxconn = 1000 as well
        String scriptTemplate = "#!/bin/bash\n\n" +

            "configure_system_limits() {\n" +
            "\techo \"=== Rouplex === Allowing more open file descriptors, tcp sockets, tcp memory\"\n" +
            "\techo %s > /proc/sys/fs/nr_open\n" +
            "\techo \"* hard nofile %s\" | tee -a /etc/security/limits.conf\n" +
            "\techo \"* soft nofile %s\" | tee -a /etc/security/limits.conf\n" +
            "\techo \"\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"# Allow use of 64000 ports from 1100 to 65100\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"net.ipv4.ip_local_port_range = 1100 65100\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"\" | tee -a /etc/sysctl.conf\n" +
            "\ttotal_mem_kb=`free -t | grep Mem | awk '{print $2}'`\n" +
            "\ttcp_mem_in_pages=$(( total_mem_kb * %s / 100 / 4))\n" +
            "\techo \"# Setup bigger tcp memory\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"net.ipv4.tcp_mem = 383865 tcp_mem_in_pages tcp_mem_in_pages\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"# Setup greater open files\" | tee -a /etc/sysctl.conf\n" +
            "\techo \"fs.file-max = %s\" | tee -a /etc/sysctl.conf\n\n" +
            "\tsysctl -p\n" +
            "}\n\n" +

            "configure_system_limits\n" +
            "service tomcat restart\n";

        return String.format(scriptTemplate, maxFileDescriptors, maxFileDescriptors,
            maxFileDescriptors, tcpAsPercentOfTotalMemory, maxFileDescriptors);
    }

    /**
     * Wait for instances to start and fill the publicIp for each of them
     *
     * @param instanceDescriptors
     * @throws Exception
     */
    private <T> void ensureEC2InstancesRunning(Map<String, InstanceDescriptor<T>> instanceDescriptors) throws Exception {
        if (instanceDescriptors.isEmpty()) {
            return;
        }

        // all instances are in same region
        AmazonEC2 amazonEC2 = getAmazonEc2Client(instanceDescriptors.values().iterator().next().getEc2Region());
        DescribeInstanceStatusRequest request = new DescribeInstanceStatusRequest().withInstanceIds(instanceDescriptors.keySet());
        while (amazonEC2.describeInstanceStatus(request).getInstanceStatuses().size() < instanceDescriptors.size()) {
            Thread.sleep(1000);
        }

        for (Reservation reservation : amazonEC2.describeInstances(
            new DescribeInstancesRequest().withInstanceIds(instanceDescriptors.keySet())).getReservations()) {

            for (Instance instance : reservation.getInstances()) {
                instanceDescriptors.get(instance.getInstanceId()).setPublicIpAddress(instance.getPublicIpAddress());
            }
        }
    }

    private <T> void ensureBenchmarkServiceRunning(Collection<InstanceDescriptor<T>> instanceDescriptors) throws Exception {
        for (InstanceDescriptor instanceDescriptor : instanceDescriptors) {
            Entity<GetServiceStateRequest> requestEntity = Entity.entity(new GetServiceStateRequest(), MediaType.APPLICATION_JSON);
            WebTarget benchmarkWebTarget = jaxrsClient.target(String.format(
                configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), instanceDescriptor.getPublicIpAddress()))
                .path("/management/service/state")
                .property(JERSEY_CLIENT_CONNECT_TIMEOUT, 2000)
                .property(JERSEY_CLIENT_READ_TIMEOUT, 10000);

            long expirationTimestamp = System.currentTimeMillis() + 10 * 60 * 1000; // 10 minutes
            while (!executorService.isShutdown()) {
                try {
                    if (benchmarkWebTarget.request(MediaType.APPLICATION_JSON)
                        .post(requestEntity, GetServiceStateResponse.class).getServiceState() == ServiceState.RUNNING) {
                        break;
                    }
                } catch (Exception e) {
                    if (System.currentTimeMillis() > expirationTimestamp) {
                        throw e;
                    }
                }

                Thread.sleep(1000);
            }
        }
    }

    private StartTcpServerRequest buildStartTcpServerRequest(StartTcpBenchmarkRequest request, String ipAddress) {
        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();

        startTcpServerRequest.setProvider(request.getProvider());
        startTcpServerRequest.setHostname(ipAddress);
        startTcpServerRequest.setPort(request.getPort());
        startTcpServerRequest.setSsl(request.isSsl());
        startTcpServerRequest.setSocketReceiveBufferSize(request.getSocketReceiveBufferSize());
        startTcpServerRequest.setSocketSendBufferSize(request.getSocketSendBufferSize());
        startTcpServerRequest.setMetricsAggregation(request.getMetricsAggregation());
        startTcpServerRequest.setBacklog(request.getBacklog());

        return startTcpServerRequest;
    }

    private StartTcpClientsRequest buildStartTcpClientsRequestRequest(
        StartTcpBenchmarkRequest request, String remoteIpAddress, int remoteIpPort) {

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setProvider(request.getProvider());
        startTcpClientsRequest.setHostname(remoteIpAddress);
        startTcpClientsRequest.setPort(remoteIpPort);
        startTcpClientsRequest.setSsl(request.isSsl());

        startTcpClientsRequest.setClientCount(request.getClientsPerHost());
        startTcpClientsRequest.setMinPayloadSize(request.getMinPayloadSize());
        startTcpClientsRequest.setMaxPayloadSize(request.getMaxPayloadSize());
        startTcpClientsRequest.setMinDelayMillisBetweenSends(request.getMinDelayMillisBetweenSends());
        startTcpClientsRequest.setMaxDelayMillisBetweenSends(request.getMaxDelayMillisBetweenSends());
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(request.getMinDelayMillisBeforeCreatingClient());
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(request.getMaxDelayMillisBeforeCreatingClient());
        startTcpClientsRequest.setMinClientLifeMillis(request.getMinClientLifeMillis());
        startTcpClientsRequest.setMaxClientLifeMillis(request.getMaxClientLifeMillis());

        startTcpClientsRequest.setSocketReceiveBufferSize(request.getSocketReceiveBufferSize());
        startTcpClientsRequest.setSocketSendBufferSize(request.getSocketSendBufferSize());
        startTcpClientsRequest.setMetricsAggregation(request.getMetricsAggregation());

        return startTcpClientsRequest;
    }

    private TcpMetricsExpectation buildClientsTcpMetricsExpectation(StartTcpBenchmarkRequest request) {
        TcpMetricsExpectation clientsExpectation = new TcpMetricsExpectation();

        clientsExpectation.setStartAsIsoInstant(Util.convertMillisToIsoInstant(System.currentTimeMillis()));

        int connectionRampUpMillis = request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient();
        int clientAvgLifetimeMillis = (request.getMaxClientLifeMillis() + request.getMinClientLifeMillis()) / 2;
        int rampUpAsMillis = Math.min(connectionRampUpMillis, clientAvgLifetimeMillis);

        clientsExpectation.setRampUpInMillis(rampUpAsMillis);
        clientsExpectation.setFinishRampUpAsIsoInstant(Util.convertMillisToIsoInstant(System.currentTimeMillis() + rampUpAsMillis));
        clientsExpectation.setConnectionsPerSecond((double) request.getClientsPerHost() * 1000 / connectionRampUpMillis);
        clientsExpectation.setMaxSimultaneousConnections((int) (((long) request.getClientsPerHost() * rampUpAsMillis) / connectionRampUpMillis));

        long avgPayloadSize = (request.getMaxPayloadSize() - 1 + request.getMinPayloadSize()) / 2;
        long avgPayloadPeriodMillis = (request.getMaxDelayMillisBetweenSends() - 1 + request.getMinDelayMillisBetweenSends()) / 2;
        long transferSpeedBps = avgPayloadSize * clientsExpectation.getMaxSimultaneousConnections() / avgPayloadPeriodMillis * 8 * 1000;

        String transferSpeedHuman = convertBpsUp(transferSpeedBps);
        clientsExpectation.setMaxUploadSpeedInBitsPerSecond(transferSpeedBps);
        clientsExpectation.setMaxDownloadSpeedInBitsPerSecond(transferSpeedBps);
        clientsExpectation.setMaxUploadSpeed(transferSpeedHuman);
        clientsExpectation.setMaxDownloadSpeed(transferSpeedHuman);

        int durationAsMillis = request.getMaxDelayMillisBeforeCreatingClient() + request.getMaxClientLifeMillis();
        clientsExpectation.setFinishAsIsoInstant(Util.convertMillisToIsoInstant(System.currentTimeMillis() + durationAsMillis));
        return clientsExpectation;
    }

    private TcpMetricsExpectation buildServerTcpMetricsExpectation(
        int clientHostsCount, TcpMetricsExpectation clientsExpectation) {

        TcpMetricsExpectation serverExpectation = new TcpMetricsExpectation();
        serverExpectation.setStartAsIsoInstant(clientsExpectation.getStartAsIsoInstant());
        serverExpectation.setRampUpInMillis(clientsExpectation.getRampUpInMillis());
        serverExpectation.setFinishRampUpAsIsoInstant(clientsExpectation.getFinishRampUpAsIsoInstant());
        serverExpectation.setConnectionsPerSecond(clientHostsCount * clientsExpectation.getConnectionsPerSecond());
        serverExpectation.setMaxSimultaneousConnections(clientHostsCount * clientsExpectation.getMaxSimultaneousConnections());

        long transferSpeedBps = clientHostsCount * clientsExpectation.getMaxUploadSpeedInBitsPerSecond();
        String transferSpeedHuman = convertBpsUp(transferSpeedBps);
        serverExpectation.setMaxUploadSpeedInBitsPerSecond(transferSpeedBps);
        serverExpectation.setMaxDownloadSpeedInBitsPerSecond(transferSpeedBps);
        serverExpectation.setMaxUploadSpeed(transferSpeedHuman);
        serverExpectation.setMaxDownloadSpeed(transferSpeedHuman);

        serverExpectation.setFinishAsIsoInstant(clientsExpectation.getFinishAsIsoInstant());

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

    @Override
    public void close() throws IOException {
        executorService.shutdown();
        synchronized (executorService) {
            executorService.notifyAll();
        }
    }
}