package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.worker.*;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@Api(value = "Benchmark Worker", description = "Service offering point to point tcp benchmarking functionality")
public class WorkerResource extends ResourceConfig implements WorkerService {

    @ApiOperation(value = "Start an echo RouplexTcpServer on this host at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    public CreateTcpServerResponse createTcpServer(CreateTcpServerRequest request) throws Exception {
        return WorkerServiceProvider.get().createTcpServer(request);
    }

    @ApiOperation(value = "Stop the RouplexTcpServer on this host bound at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public void destroyTcpServer(String serverId) throws Exception {
        WorkerServiceProvider.get().destroyTcpServer(serverId);
    }

    @Override
    public PollTcpEndPointStateResponse pollTcpServerState(String serverId) throws Exception {
        return WorkerServiceProvider.get().pollTcpServerState(serverId);
    }

    @ApiOperation(value = "Start a number of RouplexTcpClients on this host and connect them to a remote echo tcp server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public CreateTcpClientBatchResponse createTcpClientBatch(CreateTcpClientBatchRequest request) throws Exception {
        return WorkerServiceProvider.get().createTcpClientBatch(request);
    }

    @ApiOperation(value = "Get snapshot metrics of the servers and clients running on this host")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})

    @Override
    public GetMetricsResponse getMetricsResponse() throws Exception {
        return WorkerServiceProvider.get().getMetricsResponse();
    }
}
