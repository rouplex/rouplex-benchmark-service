package org.rouplex.nio.channels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.rouplex.service.benchmarkservice.BenchmarkService;
import org.rouplex.service.benchmarkservice.BenchmarkServiceProvider;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapCounter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class SSLSelectorWithMixSslAndPlainChannelsTest {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void testConnectSendAndReceive() throws Exception {
        BenchmarkService bmService = BenchmarkServiceProvider.get();

        boolean mergeMetrics = true;
        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setUseNiossl(true);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(true);
        startTcpServerRequest.setMergeClientMetrics(mergeMetrics);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setUseNiossl(true);
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostname());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
        startTcpClientsRequest.setSsl(true);
        startTcpClientsRequest.setClientCount(100);
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(1);
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(1001);
        startTcpClientsRequest.setMinClientLifeMillis(10000);
        startTcpClientsRequest.setMaxClientLifeMillis(10001);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientsRequest.setMaxDelayMillisBetweenSends(101);
        startTcpClientsRequest.setMinPayloadSize(1000);
        startTcpClientsRequest.setMaxPayloadSize(1001);
        startTcpClientsRequest.setMergeClientMetrics(mergeMetrics);
        StartTcpClientsResponse startTcpClientsResponse = bmService.startTcpClients(startTcpClientsRequest);

        String responderPrefix = String.format("%s.%s.%s.%s:%s",
                startTcpServerRequest.isUseNiossl() ? "N" : "C",
                startTcpServerRequest.isSsl() ? "S" : "P",
                BenchmarkServiceProvider.EchoResponder.class.getSimpleName(),
                startTcpServerResponse.getHostname().replace('.', '-'),
                startTcpServerResponse.getPort());

        String clientPrefix = String.format("%s.%s.%s",
                startTcpServerRequest.isUseNiossl() ? "N" : "C",
                startTcpServerRequest.isSsl() ? "S" : "P",
                BenchmarkServiceProvider.EchoRequester.class.getSimpleName());

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
                break;
            }

            SnapMeter disconnectedRequesters = getSnapshotMetricsResponse.getMeters().get(clientPrefix + ".disconnected");
            if (disconnectedRequesters != null) {
                allRequestersDisconnected = startTcpClientsRequest.getClientCount() == disconnectedRequesters.getCount();
                System.out.println("Requester disconnects: " + disconnectedRequesters.getCount());
            }

            SnapMeter disconnectedResponders = getSnapshotMetricsResponse.getMeters().get(responderPrefix + ".disconnected");
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

        if (mergeMetrics) {
            SnapMeter requesterSentBytes = lastGetSnapshotMetricsResponse.getMeters().get(
                    String.format("%s.%s", clientPrefix, "sentBytes"));

            SnapMeter responderReceivedBytes = lastGetSnapshotMetricsResponse.getMeters().get(
                    String.format("%s.%s", responderPrefix, "receivedBytes"));

            Assert.assertEquals(requesterSentBytes.getCount(), responderReceivedBytes.getCount());
        } else {
            for (int cc = 1; cc <= startTcpClientsRequest.getClientCount(); cc++) {
                SnapMeter requesterSentBytes = lastGetSnapshotMetricsResponse.getMeters().get(
                        String.format("%s.%s.%s", clientPrefix, cc, "sentBytes"));

                SnapMeter responderReceivedBytes = lastGetSnapshotMetricsResponse.getMeters().get(
                        String.format("%s.%s.%s", responderPrefix, cc, "receivedBytes"));

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
