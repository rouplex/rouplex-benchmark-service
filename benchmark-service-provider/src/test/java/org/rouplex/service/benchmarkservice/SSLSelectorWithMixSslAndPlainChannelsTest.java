package org.rouplex.service.benchmarkservice;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapCounter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;

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
        boolean[] secures = {true};
        boolean[] aggregates = {true};

        for (boolean secure : secures) {
            for (boolean aggregate : aggregates) {
                data.add(new Object[]{
                        secure,
                        aggregate
                });
            }
        }

        return data;
    }

    boolean secure;
    boolean aggregated;

    public SSLSelectorWithMixSslAndPlainChannelsTest(boolean secure, boolean aggregated) {
        this.secure = secure;
        this.aggregated = aggregated;
    }

    @Test
    public void testConnectSendAndReceive() throws Exception {
        logger.info("Starting test");
        BenchmarkService bmService = new BenchmarkServiceProvider();

        MetricsAggregation metricsAggregation = new MetricsAggregation();
        metricsAggregation.setAggregateServerAddresses(aggregated);
        metricsAggregation.setAggregateServerPorts(aggregated);
        metricsAggregation.setAggregateClientAddresses(aggregated);
        metricsAggregation.setAggregateClientPorts(aggregated);

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setUseNiossl(secure);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(secure);
//        startTcpServerRequest.setSocketSendBufferSize(150000);
//        startTcpServerRequest.setSocketReceiveBufferSize(150000);
        startTcpServerRequest.setMetricsAggregation(metricsAggregation);
        startTcpServerRequest.setBacklog(1000);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setUseNiossl(secure);
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostname());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
//        startTcpClientsRequest.setHostname("www.rouplex-demo.com");
//        startTcpClientsRequest.setPort(8888);
        startTcpClientsRequest.setSsl(secure);
//        startTcpClientsRequest.setSocketSendBufferSize(150000);
//        startTcpClientsRequest.setSocketReceiveBufferSize(150000);
        startTcpClientsRequest.setClientCount(1000);
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(1);
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(1001);
        startTcpClientsRequest.setMinClientLifeMillis(1000);
        startTcpClientsRequest.setMaxClientLifeMillis(1001);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientsRequest.setMaxDelayMillisBetweenSends(101);
        startTcpClientsRequest.setMinPayloadSize(1000);
        startTcpClientsRequest.setMaxPayloadSize(1001);
        startTcpClientsRequest.setMetricsAggregation(metricsAggregation);
        StartTcpClientsResponse startTcpClientsResponse = bmService.startTcpClients(startTcpClientsRequest);

        int maxRoundtripMillis = 60000;
        int maxRequestMillis = startTcpClientsRequest.getMaxDelayMillisBeforeCreatingClient() + startTcpClientsRequest.getMaxClientLifeMillis();
        int sleepDurationMillis = 1000;

        GetSnapshotMetricsResponse snapshotMetrics = null;
        long lastUsefulMetrics = 0;

        for (int i = 0; i < (maxRequestMillis + maxRoundtripMillis) / sleepDurationMillis; i++) {
            Thread.sleep(sleepDurationMillis);
            GetSnapshotMetricsResponse currentSnapshotMetrics = bmService.getSnapshotMetricsResponse(new GetSnapshotMetricsRequest());

            if (snapshotMetrics != null && compare(currentSnapshotMetrics, snapshotMetrics)) {
                if (System.currentTimeMillis() - lastUsefulMetrics > 10000) {
                    break; // no update during the delta time? then we are done
                }
            } else {
                lastUsefulMetrics = System.currentTimeMillis();
            }

            try {
                snapshotMetrics = currentSnapshotMetrics;
                checkAggregatedMetrics(snapshotMetrics, startTcpClientsRequest.getClientCount());
                break;
            } catch (AssertionError e) {
                // ok, give it more time
            }
        }

        logger.warning(gson.toJson(snapshotMetrics));

        if (aggregated) {
            try {
                checkAggregatedMetrics(snapshotMetrics, startTcpClientsRequest.getClientCount());
            } catch (AssertionError e) {
                // ok, give it more time
                e.printStackTrace();
                Thread.sleep(100000000);
            }
        }
    }

    void checkAggregatedMetrics(GetSnapshotMetricsResponse snapshotMetrics, int clientCount) {
        AtomicLong connectionStarted = new AtomicLong();
        AtomicLong connectionFailed = new AtomicLong();

        AtomicLong reqConnected = new AtomicLong();
        AtomicLong respConnected = new AtomicLong();
        AtomicLong reqDisconnected = new AtomicLong();
        AtomicLong respDisconnected = new AtomicLong();

        AtomicLong reqSentBytes = new AtomicLong();
        AtomicLong respSentBytes = new AtomicLong();
        AtomicLong reqSentEos = new AtomicLong();
        AtomicLong respSentEos = new AtomicLong();
        AtomicLong reqSentFailures = new AtomicLong();
        AtomicLong respSentFailures = new AtomicLong();
        AtomicLong reqReceivedBytes = new AtomicLong();
        AtomicLong respReceivedBytes = new AtomicLong();
        AtomicLong reqReceivedEos = new AtomicLong();
        AtomicLong respReceivedEos = new AtomicLong();
        AtomicLong reqReceivedDisconnect = new AtomicLong();
        AtomicLong respReceivedDisconnect = new AtomicLong();

        for (Map.Entry<String, SnapMeter> metric : snapshotMetrics.getMeters().entrySet()) {
            if (metric.getKey().equals("connection.started")) {
                connectionStarted.addAndGet(metric.getValue().getCount());
            }

            if (metric.getKey().equals("connection.failed")) {
                connectionFailed.addAndGet(metric.getValue().getCount());
            }
            else if (metric.getKey().endsWith(".connected")) {
                fetchValue(metric, reqConnected, respConnected);
            }
            else if (metric.getKey().endsWith(".disconnected")) {
                fetchValue(metric, reqDisconnected, respDisconnected);
            }

            else if (metric.getKey().endsWith(".sentBytes")) {
                fetchValue(metric, reqSentBytes, respSentBytes);
            }
            else if (metric.getKey().endsWith(".sendFailures")) {
                fetchValue(metric, reqSentFailures, respSentFailures);
            }
            else if (metric.getKey().endsWith(".sentEos")) {
                fetchValue(metric, reqSentEos, respSentEos);
            }
            else if (metric.getKey().endsWith(".receivedBytes")) {
                fetchValue(metric, reqReceivedBytes, respReceivedBytes);
            }
            else if (metric.getKey().endsWith(".receivedEos")) {
                fetchValue(metric, reqReceivedEos, respReceivedEos);
            }
            else if (metric.getKey().endsWith(".receivedDisconnect")) {
                fetchValue(metric, reqReceivedDisconnect, respReceivedDisconnect);
            }
        }

        Assert.assertEquals(clientCount, connectionStarted.get());

        Assert.assertEquals(connectionStarted.get() - connectionFailed.get(), reqConnected.get());
        Assert.assertEquals(reqConnected.get(), respConnected.get());

        Assert.assertEquals(0, reqSentFailures.get());
        Assert.assertEquals(reqSentBytes.get(), respReceivedBytes.get());
        Assert.assertTrue(reqSentBytes.get() >= respSentBytes.get());
        Assert.assertTrue(respSentBytes.get() >= reqReceivedBytes.get());

        Assert.assertEquals(reqConnected.get(), reqSentEos.get());
        Assert.assertEquals(reqConnected.get(), respReceivedEos.get() + respReceivedDisconnect.get());
        Assert.assertEquals(reqConnected.get(), reqReceivedEos.get() + reqReceivedDisconnect.get());
    }

    void fetchValue(Map.Entry<String, SnapMeter> entry, AtomicLong val1, AtomicLong val2) {
        if (entry.getKey().contains(EchoRequester.class.getSimpleName())) {
            val1.addAndGet(entry.getValue().getCount());
        } else {
            val2.addAndGet(entry.getValue().getCount());
        }
    }

    private boolean compare(GetSnapshotMetricsResponse response1, GetSnapshotMetricsResponse response2) {
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
