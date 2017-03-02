package org.rouplex.nio.channels;

import com.codahale.metrics.MetricRegistry;
import org.junit.Assert;
import org.junit.Test;
import org.rouplex.service.benchmarkservice.BenchmarkService;
import org.rouplex.service.benchmarkservice.BenchmarkServiceProvider;
import org.rouplex.service.benchmarkservice.tcp.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class SSLSelectorWithMixSslAndPlainChannels {
    @Test
    public void testConnectSendAndReceive() throws Exception {
        BenchmarkService bmService = BenchmarkServiceProvider.get();

        StartTcpServerRequest startTcpServerRequest = new StartTcpServerRequest();
        startTcpServerRequest.setUseNiossl(false);
        startTcpServerRequest.setHostname(null); // any local address
        startTcpServerRequest.setPort(0); // any port
        startTcpServerRequest.setSsl(false);
        StartTcpServerResponse startTcpServerResponse = bmService.startTcpServer(startTcpServerRequest);

        StartTcpClientsRequest startTcpClientsRequest = new StartTcpClientsRequest();
        startTcpClientsRequest.setUseNiossl(false);
        startTcpClientsRequest.setHostname(startTcpServerResponse.getHostname());
        startTcpClientsRequest.setPort(startTcpServerResponse.getPort());
        startTcpClientsRequest.setSsl(false);
        startTcpClientsRequest.setClientCount(100);
        startTcpClientsRequest.setMinDelayMillisBeforeCreatingClient(10);
        startTcpClientsRequest.setMaxDelayMillisBeforeCreatingClient(10000);
        startTcpClientsRequest.setMinClientLifeMillis(1000);
        startTcpClientsRequest.setMaxClientLifeMillis(1001);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(10);
        startTcpClientsRequest.setMinDelayMillisBetweenSends(100);
        startTcpClientsRequest.setMinPayloadSize(1000);
        startTcpClientsRequest.setMaxPayloadSize(1001);
        StartTcpClientsResponse startTcpClientsResponse = bmService.startTcpClients(startTcpClientsRequest);

        for (int i = 0; i < 10000; i++) {
            Thread.sleep(1000);
            GetSnapshotMetricsResponse getSnapshotMetricsResponse = bmService.getSnapshotMetricsResponse(new GetSnapshotMetricsRequest());
           // System.out.println(gson.toJson(getSnapshotMetricsResponse));

            SnapMeter failures = getSnapshotMetricsResponse.getMeters().get(MetricRegistry.name(BenchmarkServiceProvider.EchoClient.class.getSimpleName(), "sizeCheckFailures"));
            if (failures != null) {
                System.out.println("F " + failures.getCount());
                Assert.assertEquals(0, failures.getCount());
            }
        }
    }
}
