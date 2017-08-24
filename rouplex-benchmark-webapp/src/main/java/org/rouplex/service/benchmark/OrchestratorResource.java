package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.platform.jaxrs.security.RouplexSecurityContext;
import org.rouplex.service.benchmark.auth.AuthException;
import org.rouplex.service.benchmark.auth.AuthServiceProvider;
import org.rouplex.service.benchmark.auth.UserInfo;
import org.rouplex.service.benchmark.orchestrator.CreateTcpEchoBenchmarkRequest;
import org.rouplex.service.benchmark.orchestrator.OrchestratorService;
import org.rouplex.service.benchmark.orchestrator.OrchestratorServiceProvider;
import org.rouplex.service.benchmark.orchestrator.TcpEchoBenchmark;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import java.util.Set;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@Api(value = "Benchmark Orchestrator", description = "Service offering distributed benchmarking functionality")
public class OrchestratorResource extends ResourceConfig implements OrchestratorService {

    @Context
    HttpServletRequest httpServletRequest;
    @Context
    RouplexSecurityContext rouplexSecurityContext;
    @Context
    HttpServletResponse httpServletResponse;

    @ApiOperation(value = "Create a benchmark with one server and a number of client instances")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public TcpEchoBenchmark createTcpEchoBenchmark(CreateTcpEchoBenchmarkRequest request) throws Exception {
        return OrchestratorServiceProvider.get().createTcpBenchmark(null, request, getUserInfo());
    }

    @ApiOperation(value = "Create a benchmark with one server and a number of client instances")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public TcpEchoBenchmark createTcpEchoBenchmark(String id, CreateTcpEchoBenchmarkRequest request) throws Exception {
        return OrchestratorServiceProvider.get().createTcpBenchmark(id, request, getUserInfo());
    }

    @ApiOperation(value = "List all benchmarks (executed or executing)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public Set<String> listTcpEchoBenchmarks(String includePublic) throws Exception {
        getUserInfo();
        return OrchestratorServiceProvider.get().listTcpEchoBenchmarks(includePublic);
    }

    @ApiOperation(value = "Get the benchmark details and execution state")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public TcpEchoBenchmark getTcpEchoBenchmark(String benchmarkId) throws Exception {
        getUserInfo();
        return OrchestratorServiceProvider.get().getTcpEchoBenchmark(benchmarkId);
    }

    private UserInfo getUserInfo() throws Exception {
        UserInfo userInfo = AuthServiceProvider.get().getUserInfo(httpServletRequest.getHeader("Rouplex-SessionId"));
        if (userInfo == null) {
            throw new AuthException("No userInfo", AuthException.Reason.BadCredentials);
        }

        return userInfo;
    }
}
