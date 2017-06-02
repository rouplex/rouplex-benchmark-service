package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.auth.BenchmarkAuthService;
import org.rouplex.service.benchmark.auth.BenchmarkAuthServiceProvider;
import org.rouplex.service.benchmark.auth.GoogleAuthResponse;

import javax.ws.rs.core.Response;

@Api(value = "Benchmark Authenticator", description = "Service offering authentication and authorization of users")
public class BenchmarkAuthServiceResource extends ResourceConfig implements BenchmarkAuthService {

    @ApiOperation(value = "Log the user in")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    public Response googleAuthWithCorsHeaders(String code, String authUser, String sessionState, String prompt) throws Exception {
        GoogleAuthResponse googleAuthResponse = BenchmarkAuthServiceProvider.get().googleAuth(code, authUser, sessionState, prompt);
        return Response.ok(googleAuthResponse)
                .header("Access-Control-Allow-Origin", "*")
                .build();
    }

    @Override
    public GoogleAuthResponse googleAuth(String code, String authUser, String sessionState, String prompt) throws Exception {
        return null;
    }

    @Override
    public String rouplexAuth(String email, String password) throws Exception {
        return null;
    }
}
