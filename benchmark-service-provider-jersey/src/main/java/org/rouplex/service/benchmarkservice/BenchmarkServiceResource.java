package org.rouplex.service.benchmarkservice;

import io.swagger.annotations.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
import org.rouplex.service.benchmarkservice.tcp.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

@Api(value = "/benchmark", description = "BenchmarkService resource")
public class BenchmarkServiceResource extends ResourceConfig implements BenchmarkService {
static final String KKK = "{\n"+
            "  \"hostname\": \"string\",\n"+
            "  \"port\": 0,\n"+
            "  \"ssl\": false,\n"+
            "  \"clientCount\": 0,\n"+
            "  \"samplingPeriodMillis\": 0,\n"+
            "  \"minPayloadSize\": 0,\n"+
            "  \"maxPayloadSize\": 11110,\n"+
            "  \"minDelayMillisBetweenSends\": 0,\n"+
            "  \"maxDelayMillisBetweenSends\": 0,\n"+
            "  \"minDelayMillisBeforeCreatingClient\": 0,\n"+
            "  \"maxDelayMillisBeforeCreatingClient\": 0,\n"+
            "  \"minClientLifeMillis\": 0,\n"+
            "  \"maxClientLifeMillis\": 0\n"+
            "}";

    @Context
    HttpServletRequest httpServletRequest;
    @Context
    RouplexSecurityContext rouplexSecurityContext;
    @Context
    HttpServletResponse httpServletResponse;

    @ApiOperation(value = "Start a RouplexTcpServer at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid request")})
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) throws Exception {
        return BenchmarkServiceProvider.get().startTcpServer(request);
    }

    @ApiOperation(value = "Stops a RouplexTcpServer bound at the specified local address:port")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid request")})
    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) throws Exception {
        return BenchmarkServiceProvider.get().stopTcpServer(request);
    }

    @ApiOperation(value = "Start a number of RouplexTcpClients of similar characteristics and connect them to a remote server")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid request")})
    @Override
    public StartTcpClientsResponse startTcpClients(StartTcpClientsRequest request) throws Exception {
        return BenchmarkServiceProvider.get().startTcpClients(request);
    }

    @ApiOperation(value = "Get snapshot metrics of the servers and clients running ")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid request")})
    @Override
    public GetSnapshotMetricsResponse getSnapshotMetricsResponse(GetSnapshotMetricsRequest request) throws Exception {
        return BenchmarkServiceProvider.get().getSnapshotMetricsResponse(request);
    }
}