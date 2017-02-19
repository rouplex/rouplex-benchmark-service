package org.rouplex.service.benchmarkservice;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerResponse;
import org.rouplex.service.benchmarkservice.tcp.StopTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StopTcpServerResponse;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

@Api(value = "/benchmark", description = "BenchmarkService resource")
public class BenchmarkServiceResource extends ResourceConfig implements BenchmarkService {

    @Context
    HttpServletRequest httpServletRequest;
    @Context
    RouplexSecurityContext rouplexSecurityContext;
    @Context
    HttpServletResponse httpServletResponse;

    @ApiOperation(value = "Respond back with PingResponse, providing routing and plexing info")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 404, message = "Not found") })
    public PingResponse ping() {
        return getBenchmarkServiceServer().ping();
    }

    @ApiOperation(value = "Respond back with PingResponse, providing routing and plexing info")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid request"),
            @ApiResponse(code = 404, message = "Not found") })
    public PingResponse ping(PingRequest request) {
        return getBenchmarkServiceServer().ping(request);
    }

    @ApiOperation(value = "Start a RouplexTcpServer at hte spacified port and properties")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 400, message = "Invalid request")})
    public StartTcpServerResponse startTcpServer(StartTcpServerRequest request) {
        return getBenchmarkServiceServer().startTcpServer(request);
    }

    @Override
    public StopTcpServerResponse stopTcpServer(StopTcpServerRequest request) {
        return getBenchmarkServiceServer().stopTcpServer(request);
    }

    private BenchmarkServiceProvider getBenchmarkServiceServer() {
        return new BenchmarkServiceProvider(httpServletRequest, rouplexSecurityContext, httpServletResponse);
    }
}