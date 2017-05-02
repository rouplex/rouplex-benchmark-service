package org.rouplex.service.benchmark.orchestrator;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.rouplex.platform.tcp.RouplexTcpClient;
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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BenchmarkOrchestratorServiceProvider implements BenchmarkOrchestratorService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkOrchestratorServiceProvider.class.getSimpleName());
    private static final String JERSEY_CLIENT_READ_TIMEOUT = "jersey.config.client.readTimeout";
    private static final String JERSEY_CLIENT_CONNECT_TIMEOUT = "jersey.config.client.connectTimeout";
    private static final SimpleDateFormat UTC_DATE_FORMAT = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ssZ");

    private static BenchmarkOrchestratorService benchmarkOrchestratorService;
    public static BenchmarkOrchestratorService get() throws Exception {
        synchronized (BenchmarkOrchestratorServiceProvider.class) {
            if (benchmarkOrchestratorService == null) {
                benchmarkOrchestratorService = new BenchmarkOrchestratorServiceProvider();
            }

            return benchmarkOrchestratorService;
        }
    }

    // for distributed benchmarking, we need to manage ec2 instance lifecycle
    private final Map<Regions, AmazonEC2> amazonEC2Clients = new HashMap<>();

    // for distributed benchmarking, we need to manage remote benchmark instances
    private final Client jaxrsClient;

    // Targeting jdk6 we cannot use the nice LocalDateTime of jdk8
    private final Map<String, Map<Instance, Long>> distributedTcpBenchmarks = new HashMap<String, Map<Instance, Long>>();

    BenchmarkOrchestratorServiceProvider() throws Exception {
        jaxrsClient = createJaxRsClient();
        startMonitoringBenchmarkInstances();
    }

    @Override
    public StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception {
        if (request.getOptionalBenchmarkRequestId() == null) {
            request.setOptionalBenchmarkRequestId(UUID.randomUUID().toString());
        }

        synchronized (this) {
            // the underlying host just became an orchestrator and is granted unlimited lease
            ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
            configureServiceRequest.setLeaseEndAsIsoInstant(UTC_DATE_FORMAT.format(Long.MAX_VALUE));
            BenchmarkManagementServiceProvider.get().configureService(configureServiceRequest);

            if (distributedTcpBenchmarks.get(request.getOptionalBenchmarkRequestId()) != null) {
                throw new Exception(String.format("Distributed tcp benchmark [%s] already running", request.getOptionalBenchmarkRequestId()));
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

        InstanceType serverEC2InstanceType = getEC2InstanceType(request, true);
        Collection<Instance> serverInstances = startRunningEc2Instances(
                amazonEC2ForServer, request.getOptionalImageId(), serverEC2InstanceType, 1, request.getOptionalKeyName(), null,
                "server-" + request.getOptionalBenchmarkRequestId());

        InstanceType clientsEC2InstanceType = getEC2InstanceType(request, false);
        Collection<Instance> clientInstances = startRunningEc2Instances(
                amazonEC2ForClients, request.getOptionalImageId(), clientsEC2InstanceType, clientInstanceCount,
                request.getOptionalKeyName(), null, "client-" + request.getOptionalBenchmarkRequestId());

        serverInstances = ensureEc2InstancesRunning(amazonEC2ForServer, serverInstances);
        clientInstances = ensureEc2InstancesRunning(amazonEC2ForClients, clientInstances);

        Instance serverInstance = serverInstances.iterator().next();
        Collection<Instance> relatedInstances = new ArrayList<Instance>();
        relatedInstances.addAll(serverInstances);
        relatedInstances.addAll(clientInstances);

        ensureBenchmarkServiceRunning(relatedInstances);
        for (Instance instance : relatedInstances) {
            distributedTcpBenchmarks.get(request.getOptionalBenchmarkRequestId())
                    .put(instance, System.currentTimeMillis() + 30 * 60 * 1000);
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
            "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex", serverInstance.getPublicIpAddress()))
            .path("/benchmark/tcp/server/start")
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

        Entity<StartTcpClientsRequest> startTcpClientsEntity = Entity.entity(startTcpClientsRequest, MediaType.APPLICATION_JSON);
        for (Instance clientInstance : clientInstances) {
            jaxrsClient.target(String.format(
                "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex", clientInstance.getPublicIpAddress()))
                .path("/benchmark/tcp/clients/start")
                .request(MediaType.APPLICATION_JSON)
                .post(startTcpClientsEntity);
        }

        // prepare and return response
        StartDistributedTcpBenchmarkResponse response = new StartDistributedTcpBenchmarkResponse();
        response.setBenchmarkRequestId(request.getOptionalBenchmarkRequestId());
        response.setImageId(serverInstance.getImageId());
        response.setServerHostType(HostType.fromValue(serverEC2InstanceType.toString()));
        response.setServerGeoLocation(GeoLocation.fromName(serverEC2Region.toString()));
        response.setServerIpAddress(serverInstance.getPublicIpAddress());

        response.setClientsHostType(HostType.fromValue(clientsEC2InstanceType.toString()));
        response.setClientsGeoLocation(GeoLocation.fromName(clientsEC2Region.toString()));

        StringBuilder jconsoleJmxLink = new StringBuilder("jconsole");
        for (Instance instance : relatedInstances) {
            jconsoleJmxLink.append(' ').append(String.format(
                    "jconsole service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi",
                        instance.getPublicIpAddress(), instance.getPublicIpAddress()));
        }

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
                amazonEC2Client = AmazonEC2Client.builder().withCredentials(new AWSCredentialsProvider() {
                    @Override
                    public AWSCredentials getCredentials() {
                        return new BasicAWSCredentials("AKIAI7HYQJMBH36ZZGEQ", "k7rdZDPUfWwD+3bnJB8fPhyHh29LVtB2Wb9ZJ1qL");
                    }

                    @Override
                    public void refresh() {

                    }
                }).withRegion(region).build();

                // aaa temp amazonEC2Client = AmazonEC2Client.builder().withRegion(region).build();
                amazonEC2Clients.put(region, amazonEC2Client);
            }

            return amazonEC2Client;
        }
    }

    private void startMonitoringBenchmarkInstances() {
        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {

                while (!isClosed()) {
                    long timeStart = System.currentTimeMillis();

                    for (Map<Instance, Long> relatedInstances : distributedTcpBenchmarks.values()) {
                        Set<Instance> expiredInstances = new HashSet<Instance>();
                        for (Map.Entry<Instance, Long> instanceEntry : relatedInstances.entrySet()) {
                            try {
                                long newExpiration = System.currentTimeMillis() + 30 * 60 * 1000;
                                ConfigureServiceRequest configureServiceRequest = new ConfigureServiceRequest();
                                configureServiceRequest.setLeaseEndAsIsoInstant(UTC_DATE_FORMAT.format(newExpiration));

                                jaxrsClient.target(String.format(
                                        "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex",
                                        instanceEntry.getKey().getPublicIpAddress()))
                                        .path("/benchmark/service/configure")
                                        .property(JERSEY_CLIENT_CONNECT_TIMEOUT, 2)
                                        .property(JERSEY_CLIENT_READ_TIMEOUT, 10)
                                        .request(MediaType.APPLICATION_JSON)
                                        .post(Entity.entity(configureServiceRequest, MediaType.APPLICATION_JSON));

                                // give instance 30 minutes lease
                                instanceEntry.setValue(newExpiration);
                            } catch (Exception e) {
                                if (System.currentTimeMillis() > instanceEntry.getValue()) {
                                    logger.severe(String.format(
                                            "Terminating instance at ip [%s]. Cause: Could not poll its state",
                                            instanceEntry.getKey().getPublicIpAddress()));

                                    String az = instanceEntry.getKey().getPlacement().getAvailabilityZone();
                                    Regions region = Regions.fromName(az.substring(az.lastIndexOf(' ') + 1));
                                    tagAndTerminateEc2Instance(getAmazonEc2Client(region), instanceEntry.getKey().getInstanceId());

                                    expiredInstances.add(instanceEntry.getKey());
                                }
                            }
                        }

                        for (Instance expiredInstance : expiredInstances) {
                            relatedInstances.remove(expiredInstance);
                        }
                    }

                    // once a minute lease updates are more than enough
                    long waitMillis = timeStart + 60 * 1000 - System.currentTimeMillis();
                    if (waitMillis > 0) {
                        try {
                            Thread.sleep(waitMillis);
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

    private InstanceType getEC2InstanceType(StartDistributedTcpBenchmarkRequest request, boolean server) {
        try {
            HostType hostType = server
                    ? request.getOptionalServerHostType() : request.getOptionalClientsHostType();
            return InstanceType.fromValue(hostType.toString());
        } catch (Exception e) {
            return server ? InstanceType.M4Large : InstanceType.T2Micro;
        }
    }

    private void tagAndTerminateEc2Instance(AmazonEC2 amazonEC2, String instanceId) {
        amazonEC2.createTags(new CreateTagsRequest().withResources(instanceId)
                .withTags(new Tag().withKey("State").withValue("Terminated by Benchmark Orchestrator")));

        amazonEC2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceId));
    }

    private Collection<Instance> startRunningEc2Instances(
            AmazonEC2 amazonEC2, String optionalImageId, InstanceType instanceType,
            int count, String sshKeyName, String userData, String tag) throws IOException {

        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(optionalImageId != null ? optionalImageId : EC2MetadataUtils.getAmiId())
                .withInstanceType(instanceType)
                .withMinCount(count)
                .withMaxCount(count)
                .withIamInstanceProfile(new IamInstanceProfileSpecification().withName("RouplexBenchmarkRole"))
                .withSubnetId("subnet-9f1784c7")
                .withSecurityGroupIds("sg-bef226c5")
                .withKeyName(sshKeyName)
                .withUserData(userData)
                .withTagSpecifications(new TagSpecification().withResourceType(ResourceType.Instance).withTags(new Tag("Name", tag)));

        List<Instance> instances = amazonEC2.runInstances(runInstancesRequest).getReservation().getInstances();
        if (instances.size() != count) {
            // most likely EC2 already checks this, but won't hurt to recheck
            throw new IOException(String.format(
                    "Could only run %s EC2 instances out of %s requested", instances.size(), count));
        }

        return instances;
    }

    private Collection<Instance> ensureEc2InstancesRunning(
            AmazonEC2 amazonEC2, Collection<Instance> instances) throws IOException {

        Collection<String> instanceIds = new ArrayList<String>();
        for (Instance instance : instances) {
            instanceIds.add(instance.getInstanceId());
        }

        DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceIds);
        while (amazonEC2.describeInstanceStatus(describeInstanceRequest).getInstanceStatuses().size() < instanceIds.size()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
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
                    "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex", instance.getPublicIpAddress()))
                    .path("/benchmark/service/state")
                    .property(JERSEY_CLIENT_CONNECT_TIMEOUT, 2000)
                    .property(JERSEY_CLIENT_READ_TIMEOUT, 10000);

            long expirationTimestamp = System.currentTimeMillis() + 10 * 60 * 1000; // 10 minutes
            while (!isClosed()) {
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
    }

    public boolean isClosed() {
        return false;
    }
}