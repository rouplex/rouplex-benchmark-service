package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
import org.rouplex.service.benchmark.auth.BenchmarkAuthServiceProvider;
import org.rouplex.service.benchmark.orchestrator.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

@Api(value = "Benchmark Orchestrator", description = "Service offering multipoint benchmarking functionality")
public class BenchmarkOrchestratorServiceResource extends ResourceConfig implements BenchmarkOrchestratorService {

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

        if (!BenchmarkAuthServiceProvider.get().isSignedIn(httpServletRequest.getHeader("Rouplex-SessionId"))) {
            throw new NotAuthorizedException(httpServletRequest.getContextPath());
        }

        return BenchmarkOrchestratorServiceProvider.get().startTcpBenchmark(request);
    }

    @ApiOperation(value = "Get the status of the benchmark")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public DescribeTcpBenchmarkResponse describeTcpBenchmark(DescribeTcpBenchmarkRequest request) throws Exception {
        return null;
    }
}
