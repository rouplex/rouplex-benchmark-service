package org.rouplex.service.benchmark.management;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/management")
public interface ManagementService {
    @POST
    @Path("/service/state")
    GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception;

    @POST
    @Path("/service/configuration")
    ConfigureServiceResponse configureService(ConfigureServiceRequest request) throws Exception;
}