package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.auth.BenchmarkAuthService;
import org.rouplex.service.benchmark.auth.BenchmarkAuthServiceProvider;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "Benchmark Authenticator", description = "Service offering authentication and authorization of users")
public class BenchmarkAuthServiceResource extends ResourceConfig implements BenchmarkAuthService {

    @ApiOperation(value = "Log the user in")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})

    @GET
    @Path("/login1")
    @Produces(MediaType.TEXT_PLAIN)
    public Response login1() throws Exception {
        return Response.ok(BenchmarkAuthServiceProvider.get().login())
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }

    @Override
    public String login() throws Exception {
        return BenchmarkAuthServiceProvider.get().login();
    }
}
