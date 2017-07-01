package org.rouplex.service.benchmark;

import io.swagger.annotations.*;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.management.*;

@Api(value = "Benchmark Management", description = "Service offering instance lifecycle functionality")
public class ManagementResource extends ResourceConfig implements ManagementService {
    @ApiOperation(value = "Get the state of the benchmark service for this particular instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception {
        return ManagementServiceProvider.get().getServiceState(request);
    }

    @ApiOperation(value = "Configure benchmark service for this particular instance")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public ConfigureServiceResponse configureService(
            @ApiParam(
                examples = @Example(value = {
                    @ExampleProperty(value="{\n\t\"leaseEndAsIsoInstant\": \"" + ConfigureServiceRequest.LEASE_END_ISO_INSTANT_EXAMPLE + "\"\n}")
                })
            )
            ConfigureServiceRequest request) throws Exception {
        return ManagementServiceProvider.get().configureService(request);
    }
}
