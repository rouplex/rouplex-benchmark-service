package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.management.*;

@Api(value = "Benchmark Orchestrator", description = "Service offering instance lifecycle functionality")
public class BenchmarkManagementServiceResource extends ResourceConfig implements BenchmarkManagementService {

    @ApiOperation(value = "Get the state of the benchmark service for this particular instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception {
        return BenchmarkManagementServiceProvider.get().getServiceState(request);
    }

    @ApiOperation(value = "Configure benchmark service for this particular instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public ConfigureServiceResponse configureService(ConfigureServiceRequest request) throws Exception {
        return BenchmarkManagementServiceProvider.get().configureService(request);
    }
}
