package org.rouplex.service.benchmarkservice;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.util.EC2MetadataUtils;
import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.rouplex.platform.tcp.RouplexTcpBinder;
import org.rouplex.platform.tcp.RouplexTcpClient;
import org.rouplex.platform.tcp.RouplexTcpClientListener;
import org.rouplex.platform.tcp.RouplexTcpServer;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapCounter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapHistogram;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapTimer;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

public class BenchmarkServiceProvider implements BenchmarkService, Closeable {
    private static Logger logger = Logger.getLogger(BenchmarkServiceProvider.class.getSimpleName());

    static BenchmarkServiceProvider benchmarkServiceProvider;

    public static BenchmarkServiceProvider get() throws Exception {
        synchronized (BenchmarkServiceProvider.class) {
            if (benchmarkServiceProvider == null) {
                benchmarkServiceProvider = new BenchmarkServiceProvider();
            }

            return benchmarkServiceProvider;
        }
    }

    final Map<Provider, RouplexTcpBinder> sharedTcpBinders = new HashMap<Provider, RouplexTcpBinder>();
    final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    final MetricRegistry benchmarkerMetrics = new MetricRegistry();
    final MetricsUtil metricsUtil = new MetricsUtil(TimeUnit.SECONDS);
    final AtomicInteger incrementalId = new AtomicInteger();
    final Random random = new Random();
    Map<String, Closeable> closeables = new HashMap<>();

    final Client jaxrsClient;
    final AmazonEC2 amazonEC2;
    final String masterIpAddress;

    final Map<String, Collection<Instance>> distributedTcpBenchmarks = new HashMap<String, Collection<Instance>>();

    final RouplexTcpClientListener rouplexTcpClientListener = new RouplexTcpClientListener() {
        @Override
        public void onConnected(RouplexTcpClient rouplexTcpClient) {
            try {
                if (rouplexTcpClient.getRouplexTcpServer() == null) {
                    ((EchoRequester) rouplexTcpClient.getAttachment()).startSendingThenClose(rouplexTcpClient);
                } else {
                    new EchoResponder((StartTcpServerRequest) rouplexTcpClient.getRouplexTcpServer().getAttachment(),
                            BenchmarkServiceProvider.this, rouplexTcpClient);
                }
            } catch (Exception e) {
                benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnected")).mark();
                benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnected.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onConnected(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }

        @Override
        public void onConnectionFailed(RouplexTcpClient rouplexTcpClient, Exception reason) {
            try {
                benchmarkerMetrics.meter(MetricRegistry.name("connection.failed")).mark();
                benchmarkerMetrics.meter(MetricRegistry.name("connection.failed.EEE",
                        reason.getClass().getSimpleName(), reason.getMessage())).mark();

                logger.warning(String.format("Failed connecting EchoRequester. Cause: %s: %s",
                        reason.getClass().getSimpleName(), reason.getMessage()));
            } catch (Exception e) {
                benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnectionFailed")).mark();
                benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onConnectionFailed.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onConnectionFailed(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }

        @Override
        public void onDisconnected(RouplexTcpClient rouplexTcpClient,
                                   Exception optionalReason, boolean drainedChannels) {
            try {
                EchoReporter echoReporter = rouplexTcpClient.getRouplexTcpServer() == null
                        ? ((EchoRequester) rouplexTcpClient.getAttachment()).echoReporter
                        : ((EchoResponder) rouplexTcpClient.getAttachment()).echoReporter;

                echoReporter.liveConnections.mark(-1);
                (drainedChannels ? echoReporter.disconnectedOk : echoReporter.disconnectedKo).mark();

                logger.info(String.format("Disconnected %s. Drained: %s",
                        rouplexTcpClient.getAttachment().getClass().getSimpleName(), drainedChannels));
            } catch (Exception e) {
                benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onDisconnected")).mark();
                benchmarkServiceProvider.benchmarkerMetrics.meter(MetricRegistry.name(
                        "BenchmarkInternalError.onDisconnected.EEE", e.getClass().getSimpleName(), e.getMessage())).mark();

                logger.warning(String.format("Failed handling onDisconnected(). Cause: %s: %s",
                        e.getClass().getSimpleName(), e.getMessage()));
            }
        }
    };

    BenchmarkServiceProvider() throws Exception {
        sharedTcpBinders.put(Provider.CLASSIC_NIO, createRouplexTcpBinder(Provider.CLASSIC_NIO));

        try {
            sharedTcpBinders.put(Provider.ROUPLEX_NIOSSL, createRouplexTcpBinder(Provider.ROUPLEX_NIOSSL));
        } catch (Exception e) {
            // we tried
        }

        try {
            sharedTcpBinders.put(Provider.SCALABLE_SSL, createRouplexTcpBinder(Provider.SCALABLE_SSL));
        } catch (Exception e) {
            // we tried
        }

        addCloseable(JmxReporter.forRegistry(benchmarkerMetrics)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build()
        ).start();

        amazonEC2 = AmazonEC2Client.builder().withCredentials(new AWSCredentialsProvider() {
            @Override
            public AWSCredentials getCredentials() {
                return new BasicAWSCredentials("AKIAI7HYQJMBH36ZZGEQ", "k7rdZDPUfWwD+3bnJB8fPhyHh29LVtB2Wb9ZJ1qL");
            }

            @Override
            public void refresh() {

            }
        }).withRegion(Regions.US_WEST_2).build();
        jaxrsClient = createJaxRsClient();

        String masterIpAddress;
        try {
            masterIpAddress = EC2MetadataUtils.getUserData();
        } catch (Exception e) {
            masterIpAddress = null;
        }
        this.masterIpAddress = masterIpAddress;

        startMonitoringBenchmarkLease();
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

    private void startMonitoringBenchmarkLease() {
        if (masterIpAddress == null) {
            // no master
            return;
        }

        Executors.newSingleThreadExecutor().submit(new Runnable() {
            @Override
            public void run() {
                Entity<GetServiceStateRequest> request =
                        Entity.entity(new GetServiceStateRequest(), MediaType.APPLICATION_JSON);

                long expirationTimestamp = System.currentTimeMillis() + 30 * 60000; // 30 minutes
                while (!isClosed()) {
                    try {
                        Thread.sleep(60000); // poll every minute
                        jaxrsClient.target(String.format(
                                "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex/benchmark", masterIpAddress))
                                .path("/service-status")
                                .request(MediaType.APPLICATION_JSON)
                                .post(request);

                        expirationTimestamp = System.currentTimeMillis() + 30 * 60000; // 30 minutes
                    } catch (Exception e) {
                        if (System.currentTimeMillis() > expirationTimestamp) {
                            logger.severe(String.format(
                                    "Shutting down instance. Cause: Could not poll master's [%s] state in last 30 secs",
                                    masterIpAddress));

                            // kill self
                            amazonEC2.terminateInstances(new TerminateInstancesRequest()
                                    .withInstanceIds(EC2MetadataUtils.getInstanceId()));
                        }
                    }
                }
            }
        });
    }

    @Override
    public GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception {
        GetServiceStateResponse response = new GetServiceStateResponse();
        response.setServiceState(ServiceState.OK);
        return response;
    }

    @Override
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        if (request.getProvider() == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        try {
            logger.info(String.format("Creating EchoServer at %s:%s", request.getHostname(), request.getPort()));
            RouplexTcpBinder tcpBinder = request.isUseSharedBinder()
                    ? sharedTcpBinders.get(request.getProvider()) : createRouplexTcpBinder(request.getProvider());

            ServerSocketChannel serverSocketChannel;

            if (request.isSsl()) {
                switch (request.getProvider()) {
                    case ROUPLEX_NIOSSL:
                        serverSocketChannel = org.rouplex.nio.channels
                                .SSLServerSocketChannel.open(SSLContext.getDefault());
                        break;
                    case SCALABLE_SSL:
                        serverSocketChannel = scalablessl
                                .SSLServerSocketChannel.open(SSLContext.getDefault());
                        break;
                    case CLASSIC_NIO:
                    default:
                        throw new Exception("This provider cannot provide ssl communication");
                }
            } else {
                serverSocketChannel = ServerSocketChannel.open();
            }

            RouplexTcpServer rouplexTcpServer = RouplexTcpServer.newBuilder()
                    .withRouplexTcpBinder(tcpBinder)
                    .withServerSocketChannel(serverSocketChannel)
                    .withLocalAddress(request.getHostname(), request.getPort())
                    .withSecure(request.isSsl(), null) // value ignored in current context since channel is provided
                    .withBacklog(request.getBacklog())
                    .withSendBufferSize(request.getSocketSendBufferSize())
                    .withReceiveBufferSize(request.getSocketReceiveBufferSize())
                    .withAttachment(request)
                    .build();

            InetSocketAddress isa = (InetSocketAddress) rouplexTcpServer.getLocalAddress(true);
            logger.info(String.format("Created EchoServer at %s:%s",
                    isa.getAddress().getHostAddress().replace('.', '-'), isa.getPort()));

            String hostPort = String.format("%s:%s", isa.getHostName().replace('.', '-'), isa.getPort());
            addCloseable(hostPort, rouplexTcpServer);

            StartTcpServerResponse response = new StartTcpServerResponse();
            response.setHostaddress(isa.getAddress().getHostAddress());
            response.setHostname(isa.getHostName());
            response.setPort(isa.getPort());
            return response;
        } catch (Exception e) {
            logger.warning(String.format("Failed creating EchoServer at %s:%s. Cause: %s: %s",
                    request.getHostname(), request.getPort(), e.getClass().getSimpleName(), e.getMessage()));
            throw e;
        }
    }

    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        String hostPort = String.format("%s:%s", request.getHostname().replace('.', '-'), request.getPort());
        if (!closeAndRemove(hostPort)) {
            throw new IOException("There is no EchoServer listening at " + hostPort);
        }

        return new StopTcpServerResponse();
    }

    @Override
    public StartTcpClientsResponse startTcpClients(final StartTcpClientsRequest request) throws Exception {
        if (request.getProvider() == null) {
            throw new IllegalArgumentException("Provider cannot be null");
        }

        InetSocketAddress inetSocketAddress = new InetSocketAddress(request.getHostname(), request.getPort());
        if (inetSocketAddress.isUnresolved()) {
            throw new IllegalArgumentException(String.format("Unresolved address %s", inetSocketAddress));
        }

        logger.info(String.format("Creating %s EchoClients for server at %s:%s",
                request.clientCount, request.getHostname(), request.getPort()));

        final RouplexTcpBinder tcpBinder = request.isUseSharedBinder()
                ? sharedTcpBinders.get(request.getProvider()) : createRouplexTcpBinder(request.getProvider());

        for (int cc = 0; cc < request.clientCount; cc++) {
            long startClientMillis = request.minDelayMillisBeforeCreatingClient + random.nextInt(
                    request.maxDelayMillisBeforeCreatingClient - request.minDelayMillisBeforeCreatingClient);
            logger.info(String.format("Scheduled creation of EchoRequester in %s milliseconds.", startClientMillis));

            scheduledExecutor.schedule(new Runnable() {
                @Override
                public void run() {
                    new EchoRequester(BenchmarkServiceProvider.this, request, tcpBinder);
                }
            }, startClientMillis, TimeUnit.MILLISECONDS);
        }

        return new StartTcpClientsResponse();
    }

    @Override
    public GetSnapshotMetricsResponse getSnapshotMetricsResponse(GetSnapshotMetricsRequest request) throws Exception {
        SortedMap<String, SnapCounter> counters = new TreeMap<>();
        for (Map.Entry<String, Counter> entry : benchmarkerMetrics.getCounters().entrySet()) {
            counters.put(entry.getKey(), metricsUtil.snapCounter(entry.getValue()));
        }

        SortedMap<String, SnapMeter> meters = new TreeMap<>();
        for (Map.Entry<String, Meter> entry : benchmarkerMetrics.getMeters().entrySet()) {
            meters.put(entry.getKey(), metricsUtil.snapMeter(entry.getValue()));
        }

        SortedMap<String, SnapHistogram> histograms = new TreeMap<>();
        for (Map.Entry<String, Histogram> entry : benchmarkerMetrics.getHistograms().entrySet()) {
            histograms.put(entry.getKey(), metricsUtil.snapHistogram(entry.getValue()));
        }

        SortedMap<String, SnapTimer> timers = new TreeMap<>();
        for (Map.Entry<String, Timer> entry : benchmarkerMetrics.getTimers().entrySet()) {
            timers.put(entry.getKey(), metricsUtil.snapTimer(entry.getValue()));
        }

        GetSnapshotMetricsResponse response = new GetSnapshotMetricsResponse();
        response.setCounters(counters);
        response.setMeters(meters);
        response.setHistograms(histograms);
        response.setTimers(timers);
        return response;
    }

    private Collection<Instance> runEc2Instances(InstanceType instanceType, int count,
                                                  String sshKeyName, String userData, String tag) throws IOException {
        RunInstancesRequest runInstancesRequest = new RunInstancesRequest()
                .withImageId(EC2MetadataUtils.getAmiId())
                .withInstanceType(instanceType)
                .withMinCount(count)
                .withMaxCount(count)
                .withIamInstanceProfile(new IamInstanceProfileSpecification().withName("RouplexBenchmarkRole"))
                .withSubnetId("subnet-9f1784c7")
                .withSecurityGroupIds("sg-bef226c5")
                .withKeyName(sshKeyName)
                .withUserData(userData)
                .withTagSpecifications(new TagSpecification().withTags(new Tag("Name", tag)));

        Collection<String> instanceIds = new ArrayList<String>();
        for (Instance instance : amazonEC2.runInstances(runInstancesRequest).getReservation().getInstances()) {
            instanceIds.add(instance.getInstanceId());
        }

        DescribeInstanceStatusRequest describeInstanceRequest = new DescribeInstanceStatusRequest().withInstanceIds(instanceIds);
        while (amazonEC2.describeInstanceStatus(describeInstanceRequest).getInstanceStatuses().size() < count) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new IOException("interrupted");
            }
        }

        // fill in the public ip address for each started instance
        Collection<Instance> instances = new ArrayList<Instance>();
        for (Reservation reservation : amazonEC2.describeInstances(
                new DescribeInstancesRequest().withInstanceIds(instanceIds)).getReservations()) {
            instances.addAll(reservation.getInstances());
        }

        return instances;
    }

    private void ensureBenchmarkServiceStarted(String publicIpAddress) throws Exception {
        Entity<GetServiceStateRequest> requestEntity = Entity.entity(new GetServiceStateRequest(), MediaType.APPLICATION_JSON);
        WebTarget benchmarkWebTarget = jaxrsClient.target(String.format(
                "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex", publicIpAddress))
                .path("/benchmark/service-state");

        long expirationTimestamp = System.currentTimeMillis() + 5 * 60000; // 5 minutes
        while (!isClosed()) {
            try {
                if (benchmarkWebTarget.request(MediaType.APPLICATION_JSON)
                        .post(requestEntity, GetServiceStateResponse.class).getServiceState() == ServiceState.OK) {
                    break;
                }

                Thread.sleep(1000);
                expirationTimestamp = System.currentTimeMillis() + 5 * 60000; // 5 minutes
            } catch (Exception e) {
                if (System.currentTimeMillis() > expirationTimestamp) {
                    throw e;
                }
            }
        }
    }

    @Override
    public StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception {
        if (request.getBenchmarkRequestId() == null) {
            request.setBenchmarkRequestId(UUID.randomUUID().toString());
        }

        synchronized (this) {
            if (distributedTcpBenchmarks.putIfAbsent(request.getBenchmarkRequestId(), new ArrayList<>()) != null) {
                throw new Exception(String.format("Distributed tcp benchmark [%s] already running", request.getBenchmarkRequestId()));
            }
        }

        InstanceType instanceType;
        try {
            instanceType = InstanceType.fromValue(request.getServerInstanceType());
        } catch (Exception e) {
            instanceType = InstanceType.M4Large;
        }

        Instance serverInstance = runEc2Instances(
                instanceType, 1, request.getKeyName(), masterIpAddress,
                "server-" + request.getBenchmarkRequestId()).iterator().next();

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setProvider(request.getProvider());
        startTcpServerRequest.setHostname(serverInstance.getPrivateIpAddress());
        startTcpServerRequest.setPort(request.getPort());
        startTcpServerRequest.setSsl(request.isSsl());
        startTcpServerRequest.setSocketReceiveBufferSize(request.getSocketReceiveBufferSize());
        startTcpServerRequest.setSocketSendBufferSize(request.getSocketSendBufferSize());
        startTcpServerRequest.setMetricsAggregation(request.getMetricsAggregation());
        startTcpServerRequest.setBacklog(request.getBacklog());

        ensureBenchmarkServiceStarted(serverInstance.getPublicIpAddress());
        Entity<StartTcpServerRequest> startTcpServerEntity = Entity.entity(startTcpServerRequest, MediaType.APPLICATION_JSON);
        StartTcpServerResponse startTcpServerResponse = jaxrsClient.target(String.format(
                "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex", serverInstance.getPublicIpAddress()))
                .path("/benchmark/tcp/server/start")
                .request(MediaType.APPLICATION_JSON)
                .post(startTcpServerEntity, StartTcpServerResponse.class);

        try {
            instanceType = InstanceType.fromValue(request.getClientInstanceType());
        } catch (Exception e) {
            instanceType = InstanceType.T2Micro;
        }
        int clientInstanceCount = (request.getClientCount() - 1) / request.getClientsPerHost() + 1;
        Collection<Instance> clientInstances = runEc2Instances(
                instanceType, clientInstanceCount, request.getKeyName(), masterIpAddress,
                "client-" + request.getBenchmarkRequestId());

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

        startTcpClientsRequest.setSocketReceiveBufferSize(request.getSocketReceiveBufferSize());
        startTcpClientsRequest.setSocketSendBufferSize(request.getSocketSendBufferSize());
        startTcpClientsRequest.setMetricsAggregation(request.getMetricsAggregation());

        for (Instance clientInstance : clientInstances) {
            ensureBenchmarkServiceStarted(clientInstance.getPublicIpAddress());
        }

        Entity<StartTcpClientsRequest> startTcpClientsEntity = Entity.entity(startTcpClientsRequest, MediaType.APPLICATION_JSON);
        for (Instance clientInstance : clientInstances) {
            jaxrsClient.target(String.format(
                    "https://%s:8088/benchmark-service-provider-jersey-1.0-SNAPSHOT/rouplex", clientInstance.getPublicIpAddress()))
                    .path("/benchmark/tcp/clients/start")
                    .request(MediaType.APPLICATION_JSON)
                    .post(startTcpClientsEntity);
        }

        distributedTcpBenchmarks.get(request.getBenchmarkRequestId()).add(serverInstance);
        distributedTcpBenchmarks.get(request.getBenchmarkRequestId()).addAll(clientInstances);

        StartDistributedTcpBenchmarkResponse response = new StartDistributedTcpBenchmarkResponse();
        response.setBenchmarkRequestId(request.getBenchmarkRequestId());
        response.setServerInstanceType(serverInstance.getInstanceType());
        response.setClientInstanceType(instanceType.toString());
        response.setServerIpAddress(serverInstance.getPublicIpAddress());
        response.setServerJmxLink(String.format("jconsole service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi",
                serverInstance.getPublicIpAddress(), serverInstance.getPublicIpAddress()));

        response.setClientIpAddresses(new ArrayList<String>());
        response.setClientJmxLinks(new ArrayList<String>());
        for (Instance clientInstance : clientInstances) {
            response.getClientIpAddresses().add(clientInstance.getPublicIpAddress());
            response.getClientJmxLinks().add(String.format("jconsole service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi",
                    clientInstance.getPublicIpAddress(), clientInstance.getPublicIpAddress()));
        }

        return response;
    }

    private RouplexTcpBinder createRouplexTcpBinder(Provider provider) throws Exception {
        Selector selector = null;

        switch (provider) {
            case CLASSIC_NIO:
                selector = java.nio.channels.Selector.open();
                break;
            case ROUPLEX_NIOSSL:
                selector = org.rouplex.nio.channels.SSLSelector.open();
                break;
            case SCALABLE_SSL:
                selector = scalablessl.SSLSelector.open(SSLContext.getDefault());
                break;
        }

        RouplexTcpBinder rouplexTcpBinder = new RouplexTcpBinder(selector);
        rouplexTcpBinder.setRouplexTcpClientListener(rouplexTcpClientListener);
        return addCloseable(rouplexTcpBinder);
    }

    protected <T extends Closeable> T addCloseable(T t) {
        return addCloseable(UUID.randomUUID().toString(), t);
    }

    protected boolean closeAndRemove(String closeableId) throws IOException {
        Closeable closeable = closeables.remove(closeableId);
        if (closeable == null) {
            return false;
        }

        closeable.close();
        return true;
    }

    protected <T extends Closeable> T addCloseable(String id, T t) {
        synchronized (scheduledExecutor) {
            if (isClosed()) {
                throw new IllegalStateException("Already closed.");
            }

            closeables.put(id, t);
            return t;
        }
    }

    @Override
    public void close() throws IOException {
        synchronized (scheduledExecutor) {
            for (Closeable closeable : closeables.values()) {
                try {
                    closeable.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            closeables = null;
            scheduledExecutor.shutdownNow();
        }
    }

    public boolean isClosed() {
        synchronized (scheduledExecutor) {
            return closeables == null;
        }
    }
}