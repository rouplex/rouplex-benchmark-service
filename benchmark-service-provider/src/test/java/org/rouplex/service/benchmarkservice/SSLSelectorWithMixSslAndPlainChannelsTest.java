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

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@RunWith(Parameterized.class)
public class SSLSelectorWithMixSslAndPlainChannelsTest {
    static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Parameterized.Parameters
    public static Collection<Object[]> data() throws IOException {
        List<Object[]> data = new ArrayList<Object[]>();
        boolean[] secures = {false, true};
        boolean[] aggregates = {false, true};

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
        BenchmarkService bmService = new BenchmarkServiceProvider();

        MetricsAggregation metricsAggregation = new MetricsAggregation();
        metricsAggregation.setAggregateServerAddresses(true);
        metricsAggregation.setAggregateServerPorts(true);
        metricsAggregation.setAggregateClientAddresses(true);
        metricsAggregation.setAggregateClientPorts(true);
        metricsAggregation.setAggregateClientIds(aggregated);

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setUseNiossl(secure);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(secure);
        startTcpServerRequest.setMetricsAggregation(metricsAggregation);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setUseNiossl(secure);
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostname());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
        startTcpClientsRequest.setSsl(secure);
        startTcpClientsRequest.setClientCount(100);
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(1);
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(1001);
        startTcpClientsRequest.setMinClientLifeMillis(10000);
        startTcpClientsRequest.setMaxClientLifeMillis(10001);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientsRequest.setMaxDelayMillisBetweenSends(101);
        startTcpClientsRequest.setMinPayloadSize(1000);
        startTcpClientsRequest.setMaxPayloadSize(1001);
        startTcpClientsRequest.setMetricsAggregation(metricsAggregation);
        StartTcpClientsResponse startTcpClientsResponse = bmService.startTcpClients(startTcpClientsRequest);

        EchoReporter echoResponderReporter = new EchoReporter(startTcpServerRequest, null, EchoResponder.class);
        EchoReporter echoRequesterReporter = new EchoReporter(startTcpClientsRequest, null, EchoRequester.class);

        int maxRoundtripMillis = 20000;
        int maxRequestMillis = startTcpClientsRequest.getMaxDelayMillisBeforeCreatingClient() + startTcpClientsRequest.getMaxClientLifeMillis();
        int sleepDurationMillis = 2000;

        boolean allRequestersDisconnected = false;
        boolean allRespondersDisconnected = false;
        GetSnapshotMetricsResponse lastGetSnapshotMetricsResponse = null;

        for (int i = 0; i < (maxRequestMillis + maxRoundtripMillis) / sleepDurationMillis; i++) {
            Thread.sleep(sleepDurationMillis);
            GetSnapshotMetricsResponse getSnapshotMetricsResponse = bmService.getSnapshotMetricsResponse(new GetSnapshotMetricsRequest());

            if (compare(lastGetSnapshotMetricsResponse, getSnapshotMetricsResponse)) {
              //  break;
            }

            SnapMeter disconnectedRequesters = getSnapshotMetricsResponse
                    .getMeters().get(echoRequesterReporter.getAggregatedId() + ".disconnected");
            if (disconnectedRequesters != null) {
                allRequestersDisconnected = startTcpClientsRequest.getClientCount() == disconnectedRequesters.getCount();
                System.out.println("Requester disconnects: " + disconnectedRequesters.getCount());
            }

            SnapMeter disconnectedResponders = getSnapshotMetricsResponse
                    .getMeters().get(echoResponderReporter.getAggregatedId() + ".disconnected");
            if (disconnectedResponders != null) {
                allRespondersDisconnected = startTcpClientsRequest.getClientCount() == disconnectedResponders.getCount();
                System.out.println("Responder disconnects: " + disconnectedResponders.getCount());
            }

            lastGetSnapshotMetricsResponse = getSnapshotMetricsResponse;
            if (allRequestersDisconnected && allRespondersDisconnected) {
                break;
            }
        }

        System.out.println(gson.toJson(lastGetSnapshotMetricsResponse));

        Assert.assertTrue(allRequestersDisconnected);
        Assert.assertTrue(allRespondersDisconnected);

        if (metricsAggregation.isAggregateClientIds()) {
            SnapMeter requesterSentBytes = lastGetSnapshotMetricsResponse
                    .getMeters().get(echoRequesterReporter.getAggregatedId() + ".sentBytes");

            SnapMeter responderReceivedBytes = lastGetSnapshotMetricsResponse
                    .getMeters().get(echoResponderReporter.getAggregatedId() + ".receivedBytes");

            Assert.assertEquals(requesterSentBytes.getCount(), responderReceivedBytes.getCount());
        } else {
            for (int cc = 1; cc <= startTcpClientsRequest.getClientCount(); cc++) {

                echoRequesterReporter.setClientId(cc);
                SnapMeter requesterSentBytes = lastGetSnapshotMetricsResponse
                        .getMeters().get(echoRequesterReporter.getAggregatedId() + ".sentBytes");

                echoResponderReporter.setClientId(cc);
                SnapMeter responderReceivedBytes = lastGetSnapshotMetricsResponse
                        .getMeters().get(echoResponderReporter.getAggregatedId() + ".receivedBytes");

                Assert.assertEquals(requesterSentBytes.getCount(), responderReceivedBytes.getCount());
            }
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
