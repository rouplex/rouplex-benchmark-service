package org.rouplex.service.benchmark;

import com.google.gson.Gson;
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
import java.util.Arrays;
import java.util.UUID;

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
    public StartTcpEchoBenchmarkResponse startTcpEchoBenchmark(
            StartTcpEchoBenchmarkRequest request) throws Exception {

        UserInfo userInfo = AuthServiceProvider.get().getUserInfo(httpServletRequest.getHeader("Rouplex-SessionId"));
        if (userInfo == null) {
            throw new UnauthenticatedException();
        }

        return OrchestratorServiceProvider.get().startTcpBenchmark(request, userInfo);
    }

    @Override
    public ListTcpEchoBenchmarksResponse listTcpEchoBenchmarks(String includePublic) throws Exception {
        ListTcpEchoBenchmarksResponse response = new ListTcpEchoBenchmarksResponse();
        response.setBenchmarkIds(Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString()));
        return response;
    }

    @ApiOperation(value = "Get the status of the benchmark")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public DescribeTcpEchoBenchmarkResponse describeTcpEchoBenchmark(String benchmarkId) throws Exception {
        String jsonResponse = "{\n" +
            "  \"benchmarkId\" : \"bb\",\n" +
            "  \"imageId\" : \"ami-08edfa71\",\n" +
            "  \"serverHostType\" : null,\n" +
            "  \"serverGeoLocation\" : null,\n" +
            "  \"serverIpAddress\" : null,\n" +
            "  \"clientsHostType\" : null,\n" +
            "  \"clientsGeoLocation\" : null,\n" +
            "  \"clientIpAddresses\" : [ ],\n" +
            "  \"jconsoleJmxLink\" : \"jconsole\",\n" +
            "  \"tcpServerExpectation\" : {\n" +
            "    \"rampUpInMillis\" : 1,\n" +
            "    \"connectionsPerSecond\" : 1000.0,\n" +
            "    \"maxSimultaneousConnections\" : 1,\n" +
            "    \"maxUploadSpeedInBitsPerSecond\" : 8000,\n" +
            "    \"maxDownloadSpeedInBitsPerSecond\" : 8000,\n" +
            "    \"startAsIsoInstant\" : \"2017-06-27T05:12:57\",\n" +
            "    \"finishRampUpAsIsoInstant\" : \"2017-06-27T05:12:57.612+0000\",\n" +
            "    \"finishAsIsoInstant\" : \"2017-06-27T05:12:57\",\n" +
            "    \"maxUploadSpeed\" : \"8000 Bps\",\n" +
            "    \"maxDownloadSpeed\" : \"8000 Bps\"\n" +
            "  },\n" +
            "  \"tcpClientsExpectation\" : {\n" +
            "    \"rampUpInMillis\" : 1,\n" +
            "    \"connectionsPerSecond\" : 1000.0,\n" +
            "    \"maxSimultaneousConnections\" : 1,\n" +
            "    \"maxUploadSpeedInBitsPerSecond\" : 8000,\n" +
            "    \"maxDownloadSpeedInBitsPerSecond\" : 8000,\n" +
            "    \"startAsIsoInstant\" : \"2017-06-27T05:12:57.611+0000\",\n" +
            "    \"finishRampUpAsIsoInstant\" : \"2017-06-27T05:12:57.612+0000\",\n" +
            "    \"finishAsIsoInstant\" : \"2017-06-27T05:12:57\",\n" +
            "    \"maxUploadSpeed\" : \"8000 Bps\",\n" +
            "    \"maxDownloadSpeed\" : \"8000 Bps\"\n" +
            "  }\n" +
        "  }\n";

        DescribeTcpEchoBenchmarkResponse response = new Gson().fromJson(jsonResponse, DescribeTcpEchoBenchmarkResponse.class);
        response.setBenchmarkId(UUID.randomUUID().toString());
        return response;

//        return OrchestratorServiceProvider.get().describeTcpEchoBenchmark(request);
    }
}
