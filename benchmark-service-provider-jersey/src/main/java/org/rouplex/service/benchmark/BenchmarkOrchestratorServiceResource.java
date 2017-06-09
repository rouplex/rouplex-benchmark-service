package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
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
//    @ApiImplicitParams({
//            @ApiImplicitParam(name = "Authorization", value = "Authorization token",
//                    required = true, dataType = "string", paramType = "header")
//    })
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(
            StartDistributedTcpBenchmarkRequest request) throws Exception {

        if (httpServletRequest.getHeader("signIn-token") == null) {
            throw new NotAuthorizedException(httpServletRequest.getContextPath());
        }

        return BenchmarkOrchestratorServiceProvider.get().startDistributedTcpBenchmark(request);
    }
}
