package org.rouplex.service.benchmarkservice;

import org.rouplex.service.benchmarkservice.tcp.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/benchmark")
public interface BenchmarkService {
    @POST
    @Path("/service-state")
    GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception;

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

    @POST
    @Path("/tcp/distributed/start")
//    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception;
}