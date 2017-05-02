package org.rouplex.service.benchmark.management;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark/management")
public interface BenchmarkManagementService {
    @POST
    @Path("/service/state")
    GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception;

    @POST
    @Path("/service/configuration")
    ConfigureServiceResponse configureService(ConfigureServiceRequest request) throws Exception;
}