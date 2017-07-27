package org.rouplex.service.benchmark.worker;

import javax.ws.rs.*;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@Path("/benchmark/worker")
public interface WorkerService {
    @POST
    @Path("/tcp/servers")
    CreateTcpServerResponse createTcpServer(CreateTcpServerRequest request) throws Exception;

    @GET
    @Path("/tcp/servers/{serverId}")
    PollTcpEndPointStateResponse pollTcpServerState(@PathParam("serverId") String serverId) throws Exception;

    @DELETE
    @Path("/tcp/servers/{serverId}")
    void destroyTcpServer(@PathParam("serverId") String serverId) throws Exception;

    @POST
    @Path("/tcp/client-batches")
    CreateTcpClientBatchResponse createTcpClientBatch(CreateTcpClientBatchRequest request) throws Exception;

    @GET
    @Path("/metrics")
    GetMetricsResponse getMetricsResponse() throws Exception;
}