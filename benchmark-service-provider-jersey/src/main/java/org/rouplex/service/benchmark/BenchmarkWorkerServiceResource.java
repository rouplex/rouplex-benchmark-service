package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.worker.*;

@Api(value = "Benchmark Worker", description = "Service offering point to point tcp benchmarking functionality")
public class BenchmarkWorkerServiceResource extends ResourceConfig implements BenchmarkWorkerService {

    @ApiOperation(value = "Start an echo RouplexTcpServer on this host at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        return BenchmarkWorkerServiceProvider.get().startTcpServer(request);
    }

    @ApiOperation(value = "Stop the RouplexTcpServer on this host bound at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        return BenchmarkWorkerServiceProvider.get().stopTcpServer(request);
    }

    @Override
    public PollTcpEndPointStateResponse pollTcpServerState(PollTcpEndPointStateRequest request) throws Exception {
        return BenchmarkWorkerServiceProvider.get().pollTcpServerState(request);
    }

    @ApiOperation(value = "Start a number of RouplexTcpClients on this host and connect them to a remote echo tcp server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StartTcpClientsResponse startTcpClients(StartTcpClientsRequest request) throws Exception {
        return BenchmarkWorkerServiceProvider.get().startTcpClients(request);
    }

    @Override
    public PollTcpEndPointStateResponse pollTcpClientsState(PollTcpEndPointStateRequest request) throws Exception {
        return BenchmarkWorkerServiceProvider.get().pollTcpClientsState(request);
    }

    @ApiOperation(value = "Get snapshot metrics of the servers and clients running on this host")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})

    @Override
    public GetMetricsResponse getMetricsResponse(GetMetricsRequest request) throws Exception {
        return BenchmarkWorkerServiceProvider.get().getMetricsResponse(request);
    }
}
