package org.rouplex.service.benchmark.worker;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark/worker")
public interface BenchmarkWorkerService {
    @POST
    @Path("/tcp/server/start")
    StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception;

    @POST
    @Path("/tcp/server/stop")
    StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception;

    @POST
    @Path("/tcp/server/state")
    PollTcpEndPointStateResponse pollTcpServerState(PollTcpEndPointStateRequest request) throws Exception;

    @POST
    @Path("/tcp/clients/start")
    StartTcpClientsResponse startTcpClients(StartTcpClientsRequest request) throws Exception;

    @POST
    @Path("/tcp/clients/state")
    PollTcpEndPointStateResponse pollTcpClientsState(PollTcpEndPointStateRequest request) throws Exception;

    @POST
    @Path("/metrics")
    GetMetricsResponse getMetricsResponse(GetMetricsRequest request) throws Exception;
}