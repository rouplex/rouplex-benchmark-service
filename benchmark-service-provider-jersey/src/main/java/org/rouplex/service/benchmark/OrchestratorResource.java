package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
import org.rouplex.service.benchmark.auth.AuthServiceProvider;
import org.rouplex.service.benchmark.auth.UserInfo;
import org.rouplex.service.benchmark.orchestrator.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

@Api(value = "Benchmark Orchestrator", description = "Service offering multipoint benchmarking functionality")
public class OrchestratorResource extends ResourceConfig implements OrchestratorService {

    @Context
    HttpServletRequest httpServletRequest;
    @Context
    RouplexSecurityContext rouplexSecurityContext;
    @Context
    HttpServletResponse httpServletResponse;

    @ApiOperation(value = "Start a distributed benchmarking scenario with one server and a number of client instances")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StartTcpBenchmarkResponse startTcpBenchmark(
            StartTcpBenchmarkRequest request) throws Exception {

        UserInfo userInfo = AuthServiceProvider.get().getUserInfo(httpServletRequest.getHeader("Rouplex-SessionId"));
        if (userInfo == null) {
            throw new UnauthenticatedException();
        }

        return OrchestratorServiceProvider.get().startTcpBenchmark(request, userInfo);
    }

    @ApiOperation(value = "Get the status of the benchmark")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public DescribeTcpBenchmarkResponse describeTcpBenchmark(DescribeTcpBenchmarkRequest request) throws Exception {
        return OrchestratorServiceProvider.get().describeTcpBenchmark(request);
    }
}
