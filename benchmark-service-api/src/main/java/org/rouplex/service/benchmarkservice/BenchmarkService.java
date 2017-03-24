package org.rouplex.service.benchmarkservice;

import org.rouplex.service.benchmarkservice.tcp.*;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark11")
public interface BenchmarkService {
    @POST
    @Path("/tcp/server/start")
    StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception;

    @POST
    @Path("/tcp/server/stop")
    StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception;

    @POST
    @Path("/tcp/clients/start")
    StartTcpClientsResponse startTcpClients(StartTcpClientsRequest request) throws Exception;

    @POST
    @Path("/tcp/metrics/snapshot")
    GetSnapshotMetricsResponse getSnapshotMetricsResponse(GetSnapshotMetricsRequest request) throws Exception;
}