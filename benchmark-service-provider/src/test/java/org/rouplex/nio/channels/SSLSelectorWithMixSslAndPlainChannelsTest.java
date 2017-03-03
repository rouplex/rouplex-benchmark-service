package org.rouplex.nio.channels;

import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.Assert;
import org.junit.Test;
import org.rouplex.service.benchmarkservice.BenchmarkService;
import org.rouplex.service.benchmarkservice.BenchmarkServiceProvider;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class SSLSelectorWithMixSslAndPlainChannelsTest {
    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void testConnectSendAndReceive() throws Exception {
        BenchmarkService bmService = BenchmarkServiceProvider.get();

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setUseNiossl(true);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(false);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setUseNiossl(true);
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostname());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
        startTcpClientsRequest.setSsl(false);
        startTcpClientsRequest.setClientCount(100);
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(10);
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(1000);
        startTcpClientsRequest.setMinClientLifeMillis(10000);
        startTcpClientsRequest.setMaxClientLifeMillis(10001);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientsRequest.setMaxDelayMillisBetweenSends(101);
        startTcpClientsRequest.setMinPayloadSize(10000);
        startTcpClientsRequest.setMaxPayloadSize(10001);
        StartTcpClientsResponse startTcpClientsResponse = bmService.startTcpClients(startTcpClientsRequest);

        int maxDurationMillis = startTcpClientsRequest.getMaxDelayMillisBeforeCreatingClient() + startTcpClientsRequest.getMaxClientLifeMillis();
        GetSnapshotMetricsResponse getSnapshotMetricsResponse = null;

        for (int i = 0; i < maxDurationMillis / 1000 + 5; i++) {
            Thread.sleep(1000);
            getSnapshotMetricsResponse = bmService.getSnapshotMetricsResponse(new GetSnapshotMetricsRequest());
          //  System.out.println(gson.toJson(getSnapshotMetricsResponse));

            SnapMeter failures = getSnapshotMetricsResponse.getMeters().get(MetricRegistry.name(BenchmarkServiceProvider.EchoRequester.class.getSimpleName(), "sizeCheckFailures"));
            if (failures != null) {
                System.out.println("F " + failures.getCount());
                Assert.assertEquals(0, failures.getCount());
            }
        }

        System.out.println(gson.toJson(getSnapshotMetricsResponse));
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

        long actualDisconnectedClients = getSnapshotMetricsResponse.getMeters().get(clientPrefix + ".disconnected").getCount();
        Assert.assertEquals(startTcpClientsRequest.getClientCount(), actualDisconnectedClients);

        long actualDisconnectedResponders = getSnapshotMetricsResponse.getMeters().get(responderPrefix + ".disconnected").getCount();
        Assert.assertEquals(startTcpClientsRequest.getClientCount(), actualDisconnectedResponders);
    }
}
