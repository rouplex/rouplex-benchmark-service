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
import org.rouplex.service.benchmark.Util;
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

//import com.amazonaws.regions.Regions;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkOrchestratorServiceProvider implements BenchmarkOrchestratorService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkOrchestratorServiceProvider.class.getSimpleName());
    private static final String JERSEY_CLIENT_READ_TIMEOUT = "jersey.config.client.readTimeout";
    private static final String JERSEY_CLIENT_CONNECT_TIMEOUT = "jersey.config.client.connectTimeout";

    public enum BenchmarkConfigurationKey {
        WorkerInstanceProfileName, WorkerLeaseInMinutes, ServiceHttpDescriptor
    }

    private static BenchmarkOrchestratorService benchmarkOrchestratorService;

    public static BenchmarkOrchestratorService get() throws Exception {
        synchronized (BenchmarkOrchestratorServiceProvider.class) {
            if (benchmarkOrchestratorService == null) {
                // a shortcut for now, this will discovered automatically when rouplex provides a discovery service
                ConfigurationManager configurationManager = new ConfigurationManager();
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.WorkerInstanceProfileName,
                        "RouplexBenchmarkWorkerRole");
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.ServiceHttpDescriptor,
                        "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex/benchmark");
                configurationManager.putConfigurationEntry(BenchmarkConfigurationKey.WorkerLeaseInMinutes, "30");

                benchmarkOrchestratorService = new BenchmarkOrchestratorServiceProvider(configurationManager.getConfiguration());
            }

            return benchmarkOrchestratorService;
        }
    }

    // for distributed benchmarking, we need to manage ec2 instance lifecycle
    private final Map<Region, AmazonEC2> amazonEC2Clients = new HashMap<Region, AmazonEC2>();

    // for distributed benchmarking, we need to manage remote benchmark instances
    private final Client jaxrsClient; // rouplex platform will provide a specific version of this soon

    // Targeting jdk6 we cannot use the nice LocalDateTime of jdk8
    private final Map<String, BenchmarkDescriptor> distributedTcpBenchmarks = new HashMap<String, BenchmarkDescriptor>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Configuration configuration;

    BenchmarkOrchestratorServiceProvider(Configuration configuration) throws Exception {
        this.configuration = configuration;
        jaxrsClient = createJaxRsClient();
        startMonitoringBenchmarkInstances();
    }

    private void checkAndSanitize(StartDistributedTcpBenchmarkRequest request) {
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

        if (!"ma".equals(request.getMagicWord())) {
            throw new IllegalArgumentException("Ask admin for the magic word");
        }

        if (request.getOptionalBenchmarkRequestId() == null) {
            request.setOptionalBenchmarkRequestId(UUID.randomUUID().toString());
        }

        if (request.getOptionalTcpMemoryAsPercentOfTotal() < 1) {
            request.setOptionalTcpMemoryAsPercentOfTotal(50); // 50%
        }

    }

    @Override
    public StartDistributedTcpBenchmarkResponse
    startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception {

        checkAndSanitize(request);

        int clientInstanceCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;
        BenchmarkDescriptor benchmarkDescriptor = new BenchmarkDescriptor();

        synchronized (this) {
            // the underlying host just became an orchestrator and is granted unlimited lease
            ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
            configureServiceRequest.setLeaseEndAsIsoInstant(Util.convertMillisToIsoInstant(Long.MAX_VALUE));
            BenchmarkManagementServiceProvider.get().configureService(configureServiceRequest);

            if (distributedTcpBenchmarks.get(request.getOptionalBenchmarkRequestId()) != null) {
                throw new Exception(String.format("Distributed tcp benchmark [%s] already running",
                        request.getOptionalBenchmarkRequestId()));
            }

            distributedTcpBenchmarks.put(request.getOptionalBenchmarkRequestId(), benchmarkDescriptor);
        }

        TcpMetricsExpectation clientsExpectation = buildClientsTcpMetricsExpectation(request);

        logger.info(String.format(
                "Starting Distributed Benchmark [%s]. 1 ec2 server instance and %s ec2 client instances",
                request.getOptionalBenchmarkRequestId(), clientInstanceCount));

        String serverData = Base64.getEncoder().encodeToString(buildSystemTuningScript(
                clientsExpectation.getMaxSimultaneousConnections() * 2,
                request.getOptionalTcpMemoryAsPercentOfTotal()).getBytes(StandardCharsets.UTF_8));
        InstanceType serverEC2InstanceType = getEC2InstanceType(request, true);
        Map<String, InstanceDescriptor> serverDescriptors = startRunningEC2Instances(
                getEC2Region(request, true), request.getOptionalImageId(), serverEC2InstanceType, 1,
                request.getOptionalKeyName(), serverData, "server-" + request.getOptionalBenchmarkRequestId());

        String clientData = Base64.getEncoder().encodeToString(buildSystemTuningScript(
                clientsExpectation.getMaxSimultaneousConnections() * 2 / clientInstanceCount,
                request.getOptionalTcpMemoryAsPercentOfTotal()).getBytes(StandardCharsets.UTF_8));
        InstanceType clientsEC2InstanceType = getEC2InstanceType(request, false);
        Map<String, InstanceDescriptor> clientDescriptors = startRunningEC2Instances(
                getEC2Region(request, false), request.getOptionalImageId(), clientsEC2InstanceType, clientInstanceCount,
                request.getOptionalKeyName(), clientData, "client-" + request.getOptionalBenchmarkRequestId());

        ensureEC2InstancesRunning(serverDescriptors);
        ensureBenchmarkServiceRunning(serverDescriptors.values());
        InstanceDescriptor serverDescriptor = serverDescriptors.values().iterator().next();
        benchmarkDescriptor.getBenchmarkInstances().putAll(serverDescriptors);

        ensureEC2InstancesRunning(clientDescriptors);
        ensureBenchmarkServiceRunning(clientDescriptors.values());
        InstanceDescriptor firstClientDescriptor = clientDescriptors.values().iterator().next();
        benchmarkDescriptor.getBenchmarkInstances().putAll(clientDescriptors);

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

        List<String> clientIpAddresses = new ArrayList<String>();
        for (InstanceDescriptor clientDescriptor : clientDescriptors.values()) {
            clientIpAddresses.add(clientDescriptor.getPublicIpAddress());

            jaxrsClient.target(String.format(
                    configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), clientDescriptor.getPublicIpAddress()))
                    .path("/worker/tcp/clients/start")
                    .request(MediaType.APPLICATION_JSON)
                    .post(startTcpClientsEntity);
        }

        TcpMetricsExpectation serverExpectation = buildServerTcpMetricsExpectation(clientInstanceCount, clientsExpectation);
        benchmarkDescriptor.setTcpMetricsExpectation(serverExpectation);

        // prepare and return response
        StartDistributedTcpBenchmarkResponse response = new StartDistributedTcpBenchmarkResponse();
        response.setBenchmarkRequestId(request.getOptionalBenchmarkRequestId());
        response.setImageId(serverDescriptor.getImageId());
        response.setServerHostType(HostType.fromString(serverEC2InstanceType.toString()));
        response.setServerGeoLocation(GeoLocation.fromString(serverDescriptor.getEc2Region().getRegionName()));
        response.setServerIpAddress(serverDescriptor.getPublicIpAddress());

        response.setClientsHostType(HostType.fromString(clientsEC2InstanceType.toString()));
        response.setClientsGeoLocation(GeoLocation.fromString(firstClientDescriptor.getEc2Region().getRegionName()));

        StringBuilder jconsoleJmxLink = new StringBuilder("jconsole");
        addToJmxLink(jconsoleJmxLink, serverDescriptor);
        for (InstanceDescriptor clientDescriptor : clientDescriptors.values()) {
            addToJmxLink(jconsoleJmxLink, clientDescriptor);
        }

        response.setClientIpAddresses(clientIpAddresses);
        response.setJconsoleJmxLink(jconsoleJmxLink.toString());

        response.setTcpClientsExpectation(clientsExpectation);
        response.setTcpServerExpectation(serverExpectation);

        return response;
    }

    private void addToJmxLink(StringBuilder jmxLink, InstanceDescriptor instanceDescriptor) {
        jmxLink.append(String.format(" service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi",
                instanceDescriptor.getPublicIpAddress(), instanceDescriptor.getPublicIpAddress()));
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
                        for (Map.Entry<String, BenchmarkDescriptor> descriptorEntry : distributedTcpBenchmarks.entrySet()) {
                            monitorBenchmarkInstance(descriptorEntry.getValue());
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

    private void monitorBenchmarkInstance(BenchmarkDescriptor benchmarkDescriptor) {
        long benchmarkExpiration;

        try {
            benchmarkExpiration = Util.convertIsoInstantToMillis(
                    benchmarkDescriptor.getTcpMetricsExpectation().getFinishAsIsoInstant()) + 5 * 60 * 1000;
        } catch (Exception e) {
            benchmarkExpiration = Long.MAX_VALUE;
        }

        if (System.currentTimeMillis() > benchmarkExpiration) {
            for (Map.Entry<String, InstanceDescriptor> instanceDescriptor : benchmarkDescriptor.benchmarkInstances.entrySet()) {
                logger.info(String.format(
                        "Terminating instance [%s] at ip [%s]. Cause: Benchmark has reached maximum time",
                        instanceDescriptor.getKey(), instanceDescriptor.getValue().getPublicIpAddress()));

                tagAndTerminateEC2Instance(getAmazonEc2Client(instanceDescriptor.getValue().getEc2Region()),
                        instanceDescriptor.getKey());
            }

            benchmarkDescriptor.benchmarkInstances.clear();
        } else {
            int workerLeaseInMinutes = configuration.getAsInteger(BenchmarkConfigurationKey.WorkerLeaseInMinutes);
            long workerExpiration = System.currentTimeMillis() + workerLeaseInMinutes * 60 * 1000;
            Collection<String> expiredInstanceIds = new ArrayList<String>();

            for (Map.Entry<String, InstanceDescriptor> entry : benchmarkDescriptor.benchmarkInstances.entrySet()) {
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

                    instanceDescriptor.setExpiration(workerExpiration);
                } catch (Exception e) {
                    if (System.currentTimeMillis() > instanceDescriptor.getExpiration()) {
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
                benchmarkDescriptor.benchmarkInstances.remove(instanceId);
            }
        }
    }

    private Region getEC2Region(StartDistributedTcpBenchmarkRequest request, boolean server) {
        try {
            GeoLocation geoLocation = server ? request.getOptionalServerGeoLocation() : request.getOptionalClientsGeoLocation();
            return new Region().withRegionName(geoLocation.toString());
        } catch (Exception e) {
            return new Region().withRegionName(Regions.US_WEST_2.toString());
        }
    }

    private HostType getHostType(StartDistributedTcpBenchmarkRequest request, boolean server) {
        HostType hostType = server
                ? request.getOptionalServerHostType() : request.getOptionalClientsHostType();

        return hostType != null ? hostType : server ? HostType.EC2_M4Large : HostType.EC2_T2Micro;
    }

    private InstanceType getEC2InstanceType(StartDistributedTcpBenchmarkRequest request, boolean server) {
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

    private Map<String, InstanceDescriptor> startRunningEC2Instances(
            Region ec2Region, String optionalImageId, InstanceType instanceType,
            int count, String sshKeyName, String userData, String tag) throws IOException {

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(optionalImageId != null ? optionalImageId : EC2MetadataUtils.getAmiId())
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

        Map<String, InstanceDescriptor> instanceDescriptors = new HashMap<String, InstanceDescriptor>();
        for (Instance instance : getAmazonEc2Client(ec2Region)
                .runInstances(runInstancesRequest).getReservation().getInstances()) {

            InstanceDescriptor instanceDescriptor = new InstanceDescriptor();
            instanceDescriptors.put(instance.getInstanceId(), instanceDescriptor);
            instanceDescriptor.setPrivateIpAddress(instance.getPrivateIpAddress());
            instanceDescriptor.setImageId(instance.getImageId());
            instanceDescriptor.setEc2Region(ec2Region);
        }

        return instanceDescriptors;
    }

    /**
     * Wait for instances to start and fill the publicIp for each of them
     *
     * @param instanceDescriptors
     * @throws Exception
     */
    private void ensureEC2InstancesRunning(Map<String, InstanceDescriptor> instanceDescriptors) throws Exception {
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

    private void ensureBenchmarkServiceRunning(Collection<InstanceDescriptor> instanceDescriptors) throws Exception {
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

    private StartTcpServerRequest buildStartTcpServerRequest(StartDistributedTcpBenchmarkRequest request, String ipAddress) {
        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();

        startTcpServerRequest.setProvider(request.getProvider());
        startTcpServerRequest.setHostname(ipAddress);
        startTcpServerRequest.setPort(request.getPort());
        startTcpServerRequest.setSsl(request.isSsl());
        startTcpServerRequest.setSocketReceiveBufferSize(request.getOptionalSocketReceiveBufferSize());
        startTcpServerRequest.setSocketSendBufferSize(request.getOptionalSocketSendBufferSize());
        startTcpServerRequest.setMetricsAggregation(request.getMetricsAggregation());
        startTcpServerRequest.setBacklog(request.getOptionalBacklog());

        return startTcpServerRequest;
    }

    private StartTcpClientsRequest buildStartTcpClientsRequestRequest(
            StartDistributedTcpBenchmarkRequest request, String remoteIpAddress, int remoteIpPort) {

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

        startTcpClientsRequest.setSocketReceiveBufferSize(request.getOptionalSocketReceiveBufferSize());
        startTcpClientsRequest.setSocketSendBufferSize(request.getOptionalSocketSendBufferSize());
        startTcpClientsRequest.setMetricsAggregation(request.getMetricsAggregation());

        return startTcpClientsRequest;
    }

    private TcpMetricsExpectation buildClientsTcpMetricsExpectation(StartDistributedTcpBenchmarkRequest request) {
        TcpMetricsExpectation clientsExpectation = new TcpMetricsExpectation();

        clientsExpectation.setStartAsIsoInstant(Util.convertMillisToIsoInstant(System.currentTimeMillis()));

        int connectionRampUpMillis = request.getMaxDelayMillisBeforeCreatingClient() - request.getMinDelayMillisBeforeCreatingClient();
        int clientAvgLifetimeMillis = (request.getMaxClientLifeMillis() + request.getMinClientLifeMillis()) / 2;
        int rampUpAsMillis = Math.min(connectionRampUpMillis, clientAvgLifetimeMillis);

        clientsExpectation.setRampUpAsMillis(rampUpAsMillis);
        clientsExpectation.setFinishRampUpAsIsoInstant(Util.convertMillisToIsoInstant(System.currentTimeMillis() + rampUpAsMillis));
        clientsExpectation.setConnectionsPerSecond((double) request.getClientsPerHost() * 1000 / connectionRampUpMillis);
        clientsExpectation.setMaxSimultaneousConnections((int) (((long) request.getClientsPerHost() * rampUpAsMillis) / connectionRampUpMillis));

        long avgPayloadSize = (request.getMaxPayloadSize() - 1 + request.getMinPayloadSize()) / 2;
        long transferSpeedBps = avgPayloadSize * clientsExpectation.getMaxSimultaneousConnections() * 8;
        String transferSpeedHuman = convertBpsUp(transferSpeedBps);
        clientsExpectation.setMaxUploadSpeedAsBitsPerSecond(transferSpeedBps);
        clientsExpectation.setMaxDownloadSpeedAsBitsPerSecond(transferSpeedBps);
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
        serverExpectation.setRampUpAsMillis(clientsExpectation.getRampUpAsMillis());
        serverExpectation.setFinishRampUpAsIsoInstant(clientsExpectation.getFinishRampUpAsIsoInstant());
        serverExpectation.setConnectionsPerSecond(clientHostsCount * clientsExpectation.getConnectionsPerSecond());
        serverExpectation.setMaxSimultaneousConnections(clientHostsCount * clientsExpectation.getMaxSimultaneousConnections());

        long transferSpeedBps = clientHostsCount * clientsExpectation.getMaxUploadSpeedAsBitsPerSecond();
        String transferSpeedHuman = convertBpsUp(transferSpeedBps);
        serverExpectation.setMaxUploadSpeedAsBitsPerSecond(transferSpeedBps);
        serverExpectation.setMaxDownloadSpeedAsBitsPerSecond(transferSpeedBps);
        serverExpectation.setMaxUploadSpeed(transferSpeedHuman);
        serverExpectation.setMaxDownloadSpeed(transferSpeedHuman);

        serverExpectation.setFinishAsIsoInstant(clientsExpectation.getFinishAsIsoInstant());

        return serverExpectation;
    }

    private String convertBpsUp(long bpsValue) {
        String[] units = {" Bps", " Kbps", " Mbps", " Gbps", " Tbps"};

        int index = 0;
        while (bpsValue >= 10_000) {
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