package org.rouplex.service.benchmarkservice;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmarkservice.tcp.*;

@Api(value = "/benchmark", description = "BenchmarkService resource")
public class BenchmarkServiceResource extends ResourceConfig implements BenchmarkService {

    @ApiOperation(value = "Start a RouplexTcpServer on this host at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        return BenchmarkServiceProvider.get().startTcpServer(request);
    }

    @ApiOperation(value = "Stop the RouplexTcpServer on this host bound at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        return BenchmarkServiceProvider.get().stopTcpServer(request);
    }

    @ApiOperation(value = "Start a number of RouplexTcpClients of similar characteristics on this host and connect them to a remote server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StartTcpClientsResponse startTcpClients(StartTcpClientsRequest request) throws Exception {
        return BenchmarkServiceProvider.get().startTcpClients(request);
    }

    @ApiOperation(value = "Get snapshot metrics of the servers and clients running on this host")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public GetSnapshotMetricsResponse getSnapshotMetricsResponse(GetSnapshotMetricsRequest request) throws Exception {
        return BenchmarkServiceProvider.get().getSnapshotMetricsResponse(request);
    }
}
