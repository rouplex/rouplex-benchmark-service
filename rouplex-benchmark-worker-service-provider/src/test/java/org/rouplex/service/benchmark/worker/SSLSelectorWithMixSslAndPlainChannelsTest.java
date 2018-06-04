package org.rouplex.service.benchmark.worker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rouplex.commons.configuration.Configuration;
import org.rouplex.service.benchmark.orchestrator.MetricsAggregation;
import org.rouplex.service.benchmark.orchestrator.Provider;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
// Work in progress
@RunWith(Parameterized.class)
public class SSLSelectorWithMixSslAndPlainChannelsTest {
    private static Logger logger = Logger.getLogger(SSLSelectorWithMixSslAndPlainChannelsTest.class.getSimpleName());
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> data = new ArrayList<Object[]>();
        Provider[] providers = {Provider.CLASSIC_NIO, Provider.ROUPLEX_NIOSSL /*, Provider.THIRD_PARTY_SS*/};
        boolean[] secures = {false, true};
        boolean[] aggregates = {true};

        for (Provider provider : providers) {
            for (boolean secure : secures) {
                for (boolean aggregate : aggregates) {
                    if (provider == Provider.CLASSIC_NIO && secure) {
                        continue; // classic nio does not support ssl
                    }

                    data.add(new Object[]{
                            provider,
                            secure,
                            aggregate
                    });
                }
            }
        }

        return data;
    }

    Provider provider;
    boolean secure;
    boolean aggregated;
    RouplexWorkerServiceProvider bmService;

    public SSLSelectorWithMixSslAndPlainChannelsTest(Provider provider, boolean secure, boolean aggregated) {
        this.provider = provider;
        this.secure = secure;
        this.aggregated = aggregated;
    }

    @Before
    public void setup() throws Exception {
        bmService = new RouplexWorkerServiceProvider(new Configuration());
    }

    @After
    public void teardown() throws Exception {
        bmService.close();
    }

    @Test
    public void testConnectSendAndReceive() throws Exception {
        logger.info("Starting test");

        MetricsAggregation metricsAggregation = new MetricsAggregation();
        metricsAggregation.setAggregateServerAddresses(aggregated);
        metricsAggregation.setAggregateServerPorts(aggregated);
        metricsAggregation.setAggregateClientAddresses(aggregated);
        metricsAggregation.setAggregateClientPorts(aggregated);

        CreateTcpServerRequest createTcpServerRequest = new CreateTcpServerRequest();
        createTcpServerRequest.setProvider(provider);
        createTcpServerRequest.setHostname(null); // any local address
        createTcpServerRequest.setPort(0); // any port
        createTcpServerRequest.setSsl(secure);
//        createTcpServerRequest.setSocketSendBufferSize(150000);
//        createTcpServerRequest.setSocketReceiveBufferSize(150000);
        createTcpServerRequest.setMetricsAggregation(metricsAggregation);
        createTcpServerRequest.setBacklog(1000);
        CreateTcpServerResponse createTcpServerResponse = bmService.createTcpServer(createTcpServerRequest);

        CreateTcpClientBatchRequest startTcpClientClusterRequest = new CreateTcpClientBatchRequest();
        startTcpClientClusterRequest.setProvider(provider);
        startTcpClientClusterRequest.setHostname(createTcpServerResponse.getHostname());
        startTcpClientClusterRequest.setPort(createTcpServerResponse.getPort());
//        startTcpClientClusterRequest.setHostname("www.rouplex-demo.com");
//        startTcpClientClusterRequest.setPort(8888);
        startTcpClientClusterRequest.setSsl(secure);
//        startTcpClientClusterRequest.setSocketSendBufferSize(150000);
//        startTcpClientClusterRequest.setSocketReceiveBufferSize(150000);
        startTcpClientClusterRequest.setClientCount(1);
        startTcpClientClusterRequest.setMinDelayMillisBeforeCreatingClient(1);
        startTcpClientClusterRequest.setMaxDelayMillisBeforeCreatingClient(2001);
        startTcpClientClusterRequest.setMinClientLifeMillis(1000);
        startTcpClientClusterRequest.setMaxClientLifeMillis(1001);
        startTcpClientClusterRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientClusterRequest.setMaxDelayMillisBetweenSends(101);
        startTcpClientClusterRequest.setMinPayloadSize(1000);
        startTcpClientClusterRequest.setMaxPayloadSize(1001);
        startTcpClientClusterRequest.setMetricsAggregation(metricsAggregation);
        CreateTcpClientBatchResponse createTcpClientBatchResponse = bmService.createTcpClientBatch(startTcpClientClusterRequest);

        int maxRoundtripMillis = 10000;
        int maxRequestMillis = startTcpClientClusterRequest.getMaxDelayMillisBeforeCreatingClient() + startTcpClientClusterRequest.getMaxClientLifeMillis();
        int sleepDurationMillis = 1000;

        GetMetricsResponse snapshotMetrics = null;
        long lastUsefulMetrics = 0;

        for (int i = 0; i < (maxRequestMillis + maxRoundtripMillis) / sleepDurationMillis; i++) {
            Thread.sleep(sleepDurationMillis);
            GetMetricsResponse currentSnapshotMetrics = bmService.getMetricsResponse();

            if (snapshotMetrics != null && compare(currentSnapshotMetrics, snapshotMetrics)) {
                if (System.currentTimeMillis() - lastUsefulMetrics > 2000) {
                    break; // no update during the delta time? then we are done
                }
            } else {
                lastUsefulMetrics = System.currentTimeMillis();
            }

            try {
                snapshotMetrics = currentSnapshotMetrics;
                if (aggregated) {
                    checkAggregatedMetrics(snapshotMetrics, startTcpClientClusterRequest.getClientCount());
                } else {
                    checkDetailedMetrics(snapshotMetrics, startTcpClientClusterRequest.getClientCount());
                }
                break;
            } catch (AssertionError e) {
                // ok, give it more time
            }
        }

        try {
            if (aggregated) {
                checkAggregatedMetrics(snapshotMetrics, startTcpClientClusterRequest.getClientCount());
            } else {
                checkDetailedMetrics(snapshotMetrics, startTcpClientClusterRequest.getClientCount());
            }
            logger.warning(gson.toJson(snapshotMetrics));
        } catch (AssertionError e) {
            // ok, give it more time
            logger.warning(gson.toJson(snapshotMetrics));
//            Thread.sleep(100000000);
            throw e;
        }
    }

    void checkAggregatedMetrics(GetMetricsResponse snapshotMetrics, int clientCount) {
        AtomicLong connectionStarted = new AtomicLong();
        AtomicLong connectionFailed = new AtomicLong();

        AtomicLong requestersConnected = new AtomicLong();
        AtomicLong respondersConnected = new AtomicLong();
        AtomicLong requestersDisconnectedOk = new AtomicLong();
        AtomicLong requestersDisconnectedKo = new AtomicLong();
        AtomicLong respondersDisconnectedOk = new AtomicLong();
        AtomicLong respondersDisconnectedKo = new AtomicLong();

        AtomicLong requestersSentBytes = new AtomicLong();
        AtomicLong respondersSentBytes = new AtomicLong();
        AtomicLong requestersSentEos = new AtomicLong();
        AtomicLong respondersSentEos = new AtomicLong();
        AtomicLong requestersSentFailures = new AtomicLong();
        AtomicLong respondersSentFailures = new AtomicLong();
        AtomicLong requestersReceivedBytes = new AtomicLong();
        AtomicLong respondersReceivedBytes = new AtomicLong();
        AtomicLong requestersReceivedEos = new AtomicLong();
        AtomicLong respondersReceivedEos = new AtomicLong();
        AtomicLong requestersReceivedDisconnect = new AtomicLong();
        AtomicLong respondersReceivedDisconnect = new AtomicLong();

        for (Map.Entry<String, SnapMeter> metric : snapshotMetrics.getMeters().entrySet()) {
            if (metric.getKey().equals("connection.started")) {
                connectionStarted.addAndGet(metric.getValue().getCount());
            } else if (metric.getKey().equals("connection.failed")) {
                connectionFailed.addAndGet(metric.getValue().getCount());
            } else if (metric.getKey().endsWith(".connection.established")) {
                fetchValue(metric, requestersConnected, respondersConnected);
            } else if (metric.getKey().endsWith(".clientDisconnectedOk")) {
                fetchValue(metric, requestersDisconnectedOk, respondersDisconnectedOk);
            } else if (metric.getKey().endsWith(".clientDisconnectedKo")) {
                fetchValue(metric, requestersDisconnectedKo, respondersDisconnectedKo);
            } else if (metric.getKey().endsWith(".wcWrittenByte")) {
                fetchValue(metric, requestersSentBytes, respondersSentBytes);
            } else if (metric.getKey().endsWith(".wcWriteFailure")) {
                fetchValue(metric, requestersSentFailures, respondersSentFailures);
            } else if (metric.getKey().endsWith(".wcWrittenEos")) {
                fetchValue(metric, requestersSentEos, respondersSentEos);
            } else if (metric.getKey().endsWith(".rcReadByte")) {
                fetchValue(metric, requestersReceivedBytes, respondersReceivedBytes);
            } else if (metric.getKey().endsWith(".rcReadEos")) {
                fetchValue(metric, requestersReceivedEos, respondersReceivedEos);
            } else if (metric.getKey().endsWith(".receivedDisconnect")) {
                fetchValue(metric, requestersReceivedDisconnect, respondersReceivedDisconnect);
            }
        }

        Assert.assertEquals(clientCount, connectionStarted.get());
        Assert.assertEquals(connectionStarted.get(), requestersConnected.get() + connectionFailed.get());

        if (secure || requestersDisconnectedKo.get() == 0) {
            Assert.assertEquals(requestersConnected.get(), respondersConnected.get());

            Assert.assertTrue(requestersSentFailures.get() <= requestersDisconnectedKo.get());
            Assert.assertEquals(respondersReceivedBytes.get(), requestersSentBytes.get());
            Assert.assertTrue(respondersSentBytes.get() <= respondersReceivedBytes.get());
            Assert.assertTrue(requestersReceivedBytes.get() <= respondersSentBytes.get());

            Assert.assertEquals(requestersConnected.get(), requestersSentEos.get() + requestersSentFailures.get());
            Assert.assertEquals(requestersSentEos.get(), respondersReceivedEos.get() + respondersReceivedDisconnect.get());
            Assert.assertEquals(requestersSentEos.get(), requestersReceivedEos.get() + requestersReceivedDisconnect.get());

            Assert.assertEquals(requestersConnected.get(), requestersDisconnectedOk.get() + requestersDisconnectedKo.get());
            Assert.assertEquals(respondersConnected.get(), respondersDisconnectedOk.get() + respondersDisconnectedKo.get());
        }

        if (requestersDisconnectedKo.get() == 0 && respondersDisconnectedKo.get() == 0) {
            Assert.assertEquals(requestersSentBytes.get(), requestersReceivedBytes.get());
        }
    }

    void checkDetailedMetrics(GetMetricsResponse snapshotMetrics, int clientCount) {
        AtomicLong connectionStarted = new AtomicLong();
        AtomicLong connectionFailed = new AtomicLong();

        AtomicLong requestersConnected = new AtomicLong();
        AtomicLong respondersConnected = new AtomicLong();
        AtomicLong requestersDisconnected = new AtomicLong();
        AtomicLong respondersDisconnected = new AtomicLong();

        AtomicLong requestersSentBytes = new AtomicLong();
        AtomicLong respondersSentBytes = new AtomicLong();
        AtomicLong requestersSentEos = new AtomicLong();
        AtomicLong respondersSentEos = new AtomicLong();
        AtomicLong requestersSentFailures = new AtomicLong();
        AtomicLong respondersSentFailures = new AtomicLong();
        AtomicLong requestersReceivedBytes = new AtomicLong();
        AtomicLong respondersReceivedBytes = new AtomicLong();
        AtomicLong requestersReceivedEos = new AtomicLong();
        AtomicLong respondersReceivedEos = new AtomicLong();
        AtomicLong requestersReceivedDisconnect = new AtomicLong();
        AtomicLong respondersReceivedDisconnect = new AtomicLong();

        for (Map.Entry<String, SnapMeter> metric : snapshotMetrics.getMeters().entrySet()) {
            if (metric.getKey().equals("connection.started")) {
                connectionStarted.addAndGet(metric.getValue().getCount());
            }

            if (metric.getKey().equals("connection.failed")) {
                connectionFailed.addAndGet(metric.getValue().getCount());
            } else if (metric.getKey().endsWith(".clientConnectionEstablished")) {
                fetchValue(metric, requestersConnected, respondersConnected);
            } else if (metric.getKey().endsWith(".disconnected")) {
                fetchValue(metric, requestersDisconnected, respondersDisconnected);
            } else if (metric.getKey().endsWith(".wcWrittenByte")) {
                fetchValue(metric, requestersSentBytes, respondersSentBytes);
            } else if (metric.getKey().endsWith(".wcWriteFailure")) {
                fetchValue(metric, requestersSentFailures, respondersSentFailures);
            } else if (metric.getKey().endsWith(".wcWrittenEos")) {
                fetchValue(metric, requestersSentEos, respondersSentEos);
            } else if (metric.getKey().endsWith(".rcReadByte")) {
                fetchValue(metric, requestersReceivedBytes, respondersReceivedBytes);
            } else if (metric.getKey().endsWith(".rcReadEos")) {
                fetchValue(metric, requestersReceivedEos, respondersReceivedEos);
            } else if (metric.getKey().endsWith(".receivedDisconnect")) {
                fetchValue(metric, requestersReceivedDisconnect, respondersReceivedDisconnect);
            }
        }

        Assert.assertEquals(clientCount, connectionStarted.get());

        Assert.assertEquals(connectionStarted.get() - connectionFailed.get(), requestersConnected.get());
        Assert.assertEquals(requestersConnected.get(), respondersConnected.get());

        Assert.assertEquals(0, requestersSentFailures.get());
        Assert.assertEquals(requestersSentBytes.get(), respondersReceivedBytes.get());
        Assert.assertTrue(respondersReceivedBytes.get() >= respondersSentBytes.get());
        Assert.assertTrue(respondersSentBytes.get() >= requestersReceivedBytes.get());

        Assert.assertEquals(requestersConnected.get(), requestersSentEos.get());
        Assert.assertEquals(requestersSentEos.get(), respondersReceivedEos.get() + respondersReceivedDisconnect.get());
        Assert.assertEquals(requestersSentEos.get(), requestersReceivedEos.get() + requestersReceivedDisconnect.get());
    }

    void fetchValue(Map.Entry<String, SnapMeter> entry, AtomicLong val1, AtomicLong val2) {
        if (entry.getKey().contains(EchoRequester.class.getSimpleName())) {
            val1.addAndGet(entry.getValue().getCount());
        } else {
            val2.addAndGet(entry.getValue().getCount());
        }
    }

    private boolean compare(GetMetricsResponse response1, GetMetricsResponse response2) {
        if (response1 == null || response2 == null) {
            return false;
        }

        if (!compare(response1.getMeters(), response2.getMeters())) {
            return false;
        }

        return true;
    }

    private <T extends SnapCounter> boolean compare(SortedMap<String, T> metric1, SortedMap<String, T> metric2) {
        if (metric1 == null) {
            return metric2 == null;
        }

        if (metric2 == null) {
            return false;
        }

        if (metric1.size() != metric2.size()) {
            return false;
        }

        Iterator<Map.Entry<String, T>> entries1 = metric1.entrySet().iterator();
        Iterator<Map.Entry<String, T>> entries2 = metric2.entrySet().iterator();
        while (entries1.hasNext()) {
            Map.Entry<String, T> entry1 = entries1.next();
            Map.Entry<String, T> entry2 = entries2.next();

            if (!entry1.getKey().equals(entry2.getKey())) {
                return false;
            }

            if (entry1.getValue().getCount() != entry2.getValue().getCount()) {
                return false;
            }
        }

        return true;
    }
}
