package org.rouplex.service.benchmark.worker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rouplex.service.benchmark.management.SnapCounter;
import org.rouplex.service.benchmark.management.SnapMeter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@RunWith(Parameterized.class)
public class SSLSelectorWithMixSslAndPlainChannelsTest {
    private static Logger logger = Logger.getLogger(SSLSelectorWithMixSslAndPlainChannelsTest.class.getSimpleName());
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> data = new ArrayList<Object[]>();
//        Provider[] providers = {Provider.CLASSIC_NIO, Provider.ROUPLEX_NIOSSL, Provider.SCALABLE_SSL};
        Provider[] providers = {Provider.CLASSIC_NIO, Provider.ROUPLEX_NIOSSL};
//        Provider[] providers = {Provider.SCALABLE_SSL};
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
    BenchmarkWorkerServiceProvider bmService;

    public SSLSelectorWithMixSslAndPlainChannelsTest(Provider provider, boolean secure, boolean aggregated) {
        this.provider = provider;
        this.secure = secure;
        this.aggregated = aggregated;
    }

    @Before
    public void setup() throws Exception {
        bmService = new BenchmarkWorkerServiceProvider();
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

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setProvider(provider);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(secure);
//        startTcpServerRequest.setSocketSendBufferSize(150000);
//        startTcpServerRequest.setSocketReceiveBufferSize(150000);
        startTcpServerRequest.setMetricsAggregation(metricsAggregation);
        startTcpServerRequest.setBacklog(1000);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setProvider(provider);
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostname());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
//        startTcpClientsRequest.setHostname("www.rouplex-demo.com");
//        startTcpClientsRequest.setPort(8888);
        startTcpClientsRequest.setSsl(secure);
//        startTcpClientsRequest.setSocketSendBufferSize(150000);
//        startTcpClientsRequest.setSocketReceiveBufferSize(150000);
        startTcpClientsRequest.setClientCount(1);
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(1);
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(2001);
        startTcpClientsRequest.setMinClientLifeMillis(1000);
        startTcpClientsRequest.setMaxClientLifeMillis(1001);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientsRequest.setMaxDelayMillisBetweenSends(101);
        startTcpClientsRequest.setMinPayloadSize(1000);
        startTcpClientsRequest.setMaxPayloadSize(1001);
        startTcpClientsRequest.setMetricsAggregation(metricsAggregation);
        StartTcpClientsResponse startTcpClientsResponse = bmService.startTcpClients(startTcpClientsRequest);

        int maxRoundtripMillis = 10000;
        int maxRequestMillis = startTcpClientsRequest.getMaxDelayMillisBeforeCreatingClient() + startTcpClientsRequest.getMaxClientLifeMillis();
        int sleepDurationMillis = 1000;

        GetMetricsResponse snapshotMetrics = null;
        long lastUsefulMetrics = 0;

        for (int i = 0; i < (maxRequestMillis + maxRoundtripMillis) / sleepDurationMillis; i++) {
            Thread.sleep(sleepDurationMillis);
            GetMetricsResponse currentSnapshotMetrics = bmService.getMetricsResponse(new GetMetricsRequest());

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
                    checkAggregatedMetrics(snapshotMetrics, startTcpClientsRequest.getClientCount());
                } else {
                    checkDetailedMetrics(snapshotMetrics, startTcpClientsRequest.getClientCount());
                }
                break;
            } catch (AssertionError e) {
                // ok, give it more time
            }
        }

        try {
            if (aggregated) {
                checkAggregatedMetrics(snapshotMetrics, startTcpClientsRequest.getClientCount());
            } else {
                checkDetailedMetrics(snapshotMetrics, startTcpClientsRequest.getClientCount());
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
            } else if (metric.getKey().endsWith(".disconnectedOk")) {
                fetchValue(metric, requestersDisconnectedOk, respondersDisconnectedOk);
            } else if (metric.getKey().endsWith(".disconnectedKo")) {
                fetchValue(metric, requestersDisconnectedKo, respondersDisconnectedKo);
            } else if (metric.getKey().endsWith(".sentBytes")) {
                fetchValue(metric, requestersSentBytes, respondersSentBytes);
            } else if (metric.getKey().endsWith(".sendFailures")) {
                fetchValue(metric, requestersSentFailures, respondersSentFailures);
            } else if (metric.getKey().endsWith(".sentEos")) {
                fetchValue(metric, requestersSentEos, respondersSentEos);
            } else if (metric.getKey().endsWith(".receivedBytes")) {
                fetchValue(metric, requestersReceivedBytes, respondersReceivedBytes);
            } else if (metric.getKey().endsWith(".receivedEos")) {
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
            } else if (metric.getKey().endsWith(".connectionEstablished")) {
                fetchValue(metric, requestersConnected, respondersConnected);
            } else if (metric.getKey().endsWith(".disconnected")) {
                fetchValue(metric, requestersDisconnected, respondersDisconnected);
            } else if (metric.getKey().endsWith(".sentBytes")) {
                fetchValue(metric, requestersSentBytes, respondersSentBytes);
            } else if (metric.getKey().endsWith(".sendFailures")) {
                fetchValue(metric, requestersSentFailures, respondersSentFailures);
            } else if (metric.getKey().endsWith(".sentEos")) {
                fetchValue(metric, requestersSentEos, respondersSentEos);
            } else if (metric.getKey().endsWith(".receivedBytes")) {
                fetchValue(metric, requestersReceivedBytes, respondersReceivedBytes);
            } else if (metric.getKey().endsWith(".receivedEos")) {
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
