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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

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
    private final Map<Regions, AmazonEC2> amazonEC2Clients = new HashMap<Regions, AmazonEC2>();

    // for distributed benchmarking, we need to manage remote benchmark instances
    private final Client jaxrsClient; // rouplex platform will provide a specific version of this soon

    // Targeting jdk6 we cannot use the nice LocalDateTime of jdk8
    private final Map<String, Map<Instance, Long>> distributedTcpBenchmarks = new HashMap<String, Map<Instance, Long>>();

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final Configuration configuration;

    BenchmarkOrchestratorServiceProvider(Configuration configuration) throws Exception {
        this.configuration = configuration;
        jaxrsClient = createJaxRsClient();
        startMonitoringBenchmarkInstances();
    }

    @Override
    public StartDistributedTcpBenchmarkResponse
        startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception {

        if (!"ma".equals(request.getMagicWord())) {
            throw new Exception("Ask admin for the magic word");
        }

        if (request.getOptionalBenchmarkRequestId() == null) {
            request.setOptionalBenchmarkRequestId(UUID.randomUUID().toString());
        }

        synchronized (this) {
            // the underlying host just became an orchestrator and is granted unlimited lease
            ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
            configureServiceRequest.setLeaseEndAsIsoInstant(Util.convertIsoInstantToString(Long.MAX_VALUE));
            BenchmarkManagementServiceProvider.get().configureService(configureServiceRequest);

            if (distributedTcpBenchmarks.get(request.getOptionalBenchmarkRequestId()) != null) {
                throw new Exception(String.format("Distributed tcp benchmark [%s] already running",
                        request.getOptionalBenchmarkRequestId()));
            }

            distributedTcpBenchmarks.put(request.getOptionalBenchmarkRequestId(), new HashMap<Instance, Long>());
        }

        int clientInstanceCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;

        logger.info(String.format(
                "Starting Distributed Benchmark [%s]. 1 server ec2 instance and %s client ec2 instances",
                request.getOptionalBenchmarkRequestId(), clientInstanceCount));

        Regions serverEC2Region = getEC2Region(request, true);
        AmazonEC2 amazonEC2ForServer = getAmazonEc2Client(serverEC2Region);

        Regions clientsEC2Region = getEC2Region(request, false);
        AmazonEC2 amazonEC2ForClients = getAmazonEc2Client(clientsEC2Region);

        int ramMB = EC2Metadata.get().getEc2InstanceTypes().get(getHostType(request, true)).getRamMB();
        String userData = buildSystemTuningScript(2000000, ramMB);
        InstanceType serverEC2InstanceType = getEC2InstanceType(request, true);
        Collection<Instance> serverInstances = startRunningEC2Instances1(
                amazonEC2ForServer, request.getOptionalImageId(), serverEC2InstanceType, 1,
                request.getOptionalKeyName(), userData, "server-" + request.getOptionalBenchmarkRequestId());

        ramMB = EC2Metadata.get().getEc2InstanceTypes().get(getHostType(request, false)).getRamMB();
        userData = buildSystemTuningScript(2000000, ramMB);
        InstanceType clientsEC2InstanceType = getEC2InstanceType(request, false);
        Collection<Instance> clientInstances = startRunningEC2Instances1(
                amazonEC2ForClients, request.getOptionalImageId(), clientsEC2InstanceType, clientInstanceCount,
                request.getOptionalKeyName(), userData, "client-" + request.getOptionalBenchmarkRequestId());

        serverInstances = ensureEC2InstancesRunning(amazonEC2ForServer, serverInstances);
        clientInstances = ensureEC2InstancesRunning(amazonEC2ForClients, clientInstances);

        Instance serverInstance = serverInstances.iterator().next();
        Collection<Instance> relatedInstances = new ArrayList<Instance>();
        relatedInstances.addAll(serverInstances);
        relatedInstances.addAll(clientInstances);

        ensureBenchmarkServiceRunning(relatedInstances);
        long leaseExpiration = System.currentTimeMillis() +
                configuration.getAsInteger(BenchmarkConfigurationKey.WorkerLeaseInMinutes) * 60 * 1000;
        for (Instance instance : relatedInstances) {
            distributedTcpBenchmarks.get(request.getOptionalBenchmarkRequestId()).put(instance, leaseExpiration);
        }

        // start server remotely
        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setProvider(request.getProvider());
        startTcpServerRequest.setHostname(serverInstance.getPrivateIpAddress());
        startTcpServerRequest.setPort(request.getPort());
        startTcpServerRequest.setSsl(request.isSsl());
        startTcpServerRequest.setSocketReceiveBufferSize(request.getOptionalSocketReceiveBufferSize());
        startTcpServerRequest.setSocketSendBufferSize(request.getOptionalSocketSendBufferSize());
        startTcpServerRequest.setMetricsAggregation(request.getMetricsAggregation());
        startTcpServerRequest.setBacklog(request.getOptionalBacklog());

        Entity<StartTcpServerRequest> startTcpServerEntity = Entity.entity(startTcpServerRequest, MediaType.APPLICATION_JSON);
        StartTcpServerResponse startTcpServerResponse = jaxrsClient.target(String.format(
                configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), serverInstance.getPublicIpAddress()))
                .path("/worker/tcp/server/start")
                .request(MediaType.APPLICATION_JSON)
                .post(startTcpServerEntity, StartTcpServerResponse.class);

        // start clients remotely
        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setProvider(request.getProvider());
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostaddress());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
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

        List<String> clientIpAddresses = new ArrayList<String>();
        Entity<StartTcpClientsRequest> startTcpClientsEntity = Entity.entity(startTcpClientsRequest, MediaType.APPLICATION_JSON);

        for (Instance clientInstance : clientInstances) {
            clientIpAddresses.add(clientInstance.getPublicIpAddress());

            jaxrsClient.target(String.format(
                    configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), clientInstance.getPublicIpAddress()))
                    .path("/worker/tcp/clients/start")
                    .request(MediaType.APPLICATION_JSON)
                    .post(startTcpClientsEntity);
        }

        // prepare and return response
        StartDistributedTcpBenchmarkResponse response = new StartDistributedTcpBenchmarkResponse();
        response.setBenchmarkRequestId(request.getOptionalBenchmarkRequestId());
        response.setImageId(serverInstance.getImageId());
        response.setServerHostType(HostType.fromString(serverEC2InstanceType.toString()));
        response.setServerGeoLocation(GeoLocation.fromString(serverEC2Region.getName()));
        response.setServerIpAddress(serverInstance.getPublicIpAddress());

        response.setClientsHostType(HostType.fromString(clientsEC2InstanceType.toString()));
        response.setClientsGeoLocation(GeoLocation.fromString(clientsEC2Region.getName()));

        StringBuilder jconsoleJmxLink = new StringBuilder("jconsole");
        for (Instance instance : relatedInstances) {
            jconsoleJmxLink.append(String.format(" service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi",
                    instance.getPublicIpAddress(), instance.getPublicIpAddress()));
        }

        response.setClientIpAddresses(clientIpAddresses);
        response.setJconsoleJmxLink(jconsoleJmxLink.toString());
        return response;
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

    private AmazonEC2 getAmazonEc2Client(Regions region) {
        synchronized (amazonEC2Clients) {
            AmazonEC2 amazonEC2Client = amazonEC2Clients.get(region);

            if (amazonEC2Client == null) {
                amazonEC2Client = AmazonEC2ClientBuilder.defaultClient();
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
                    long timeStart = System.currentTimeMillis();
                    int workerLeaseInMinutes = configuration.getAsInteger(BenchmarkConfigurationKey.WorkerLeaseInMinutes);
                    long newExpiration = timeStart + workerLeaseInMinutes * 60 * 1000;

                    for (Map<Instance, Long> relatedInstances : distributedTcpBenchmarks.values()) {
                        Set<Instance> expiredInstances = new HashSet<Instance>();
                        for (Map.Entry<Instance, Long> instanceEntry : relatedInstances.entrySet()) {
                            try {
                                ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
                                configureServiceRequest.setLeaseEndAsIsoInstant(Util.convertIsoInstantToString(newExpiration));

                                jaxrsClient.target(String.format(
                                        configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor),
                                        instanceEntry.getKey().getPublicIpAddress()))
                                        .path("/management/service/configuration")
                                        .property(JERSEY_CLIENT_CONNECT_TIMEOUT, 2000)
                                        .property(JERSEY_CLIENT_READ_TIMEOUT, 10000)
                                        .request(MediaType.APPLICATION_JSON)
                                        .post(Entity.entity(configureServiceRequest, MediaType.APPLICATION_JSON));

                                instanceEntry.setValue(newExpiration);
                            } catch (Exception e) {
                                if (System.currentTimeMillis() > instanceEntry.getValue()) {
                                    expiredInstances.add(instanceEntry.getKey());
                                }
                            }
                        }

                        for (Instance expiredInstance : expiredInstances) {
                            logger.severe(String.format("Terminating instance at ip [%s]. " +
                                            "Cause: Could not contact it to announce its new lease in the last %s minutes",
                                    expiredInstance.getPublicIpAddress(), workerLeaseInMinutes));

                            String az = expiredInstance.getPlacement().getAvailabilityZone();
                            Regions region = Regions.fromName(az.substring(az.lastIndexOf(' ') + 1));
                            tagAndTerminateEC2Instance(getAmazonEc2Client(region), expiredInstance.getInstanceId());

                            relatedInstances.remove(expiredInstance);
                        }
                    }

                    // upsate leases once a minute (or less often occasionally)
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

    private Regions getEC2Region(StartDistributedTcpBenchmarkRequest request, boolean server) {
        try {
            GeoLocation geoLocation = server ? request.getOptionalServerGeoLocation() : request.getOptionalClientsGeoLocation();
            return Regions.fromName(geoLocation.toString());
        } catch (Exception e) {
            return Regions.US_WEST_2;
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

    private String buildSystemTuningScript(int maxFD, long ramMB) {
        long tcpMemPages = ramMB * 1024 * 1024 / 4096;

        // consider net.core.somaxconn = 1000 as well
        String scriptTemplate = "#!/bin/bash\n\n" +

        "setup_system_socket_limits() {\n" +
            "echo \"=== Rouplex === Allowing more open file descriptors, tcp sockets, tcp memory\"\n" +
            "echo \"* hard nofile %s\" | sudo tee -a /etc/security/limits.conf\n" +
            "echo \"* soft nofile %s\" | sudo tee -a /etc/security/limits.conf\n" +
            "echo \"\" | sudo tee -a /etc/sysctl.conf\n" +
            "echo \"# Allow use of 64000 ports from 1100 to 65100\" | sudo tee -a /etc/sysctl.conf\n" +
            "echo \"net.ipv4.ip_local_port_range = 1100 65100\" | sudo tee -a /etc/sysctl.conf\n" +
            "echo \"\" | sudo tee -a /etc/sysctl.conf\n" +
            "echo \"# Setup bigger tcp memory\" | sudo tee -a /etc/sysctl.conf\n" +
            "echo \"net.ipv4.tcp_mem = 383865 %s %s\" | sudo tee -a /etc/sysctl.conf\n" +
        "}\n\n" +

        "setup_system_socket_limits\n";

        return String.format(scriptTemplate, maxFD, maxFD, tcpMemPages, tcpMemPages);
    }

    private Collection<Instance> startRunningEC2Instances1(
            AmazonEC2 amazonEC2, String optionalImageId, InstanceType instanceType,
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

        List<Instance> instances = amazonEC2.runInstances(runInstancesRequest).getReservation().getInstances();
        if (instances.size() != count) {
            // most likely EC2 already checks this, but won't hurt to recheck
            throw new IOException(String.format(
                    "Could only run %s EC2 instances out of %s requested", instances.size(), count));
        }

        return instances;
    }

    private Collection<Instance> ensureEC2InstancesRunning(
            AmazonEC2 amazonEC2, Collection<Instance> instances) throws Exception {

        Collection<String> instanceIds = new ArrayList<String>();
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }

        DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceIds);
        while (amazonEC2.describeInstanceStatus(describeInstanceRequest).getInstanceStatuses().size() < instanceIds.size()) {
            Thread.sleep(1000);
        }

        instances = new ArrayList<Instance>();
        for (Reservation reservation : amazonEC2.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceIds)).getReservations()) {
            instances.addAll(reservation.getInstances());
        }

        return instances;
    }

    private void ensureBenchmarkServiceRunning(Collection<Instance> instances) throws Exception {
        for (Instance instance : instances) {
            Entity<GetServiceStateRequest> requestEntity = Entity.entity(new GetServiceStateRequest(), MediaType.APPLICATION_JSON);
            WebTarget benchmarkWebTarget = jaxrsClient.target(String.format(
                    configuration.get(BenchmarkConfigurationKey.ServiceHttpDescriptor), instance.getPublicIpAddress()))
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

    @Override
    public void close() throws IOException {
        executorService.shutdown();
        synchronized (executorService) {
            executorService.notifyAll();
        }
    }
}