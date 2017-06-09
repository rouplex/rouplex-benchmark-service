package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.auth.BenchmarkAuthService;
import org.rouplex.service.benchmark.auth.BenchmarkAuthServiceProvider;
import org.rouplex.service.benchmark.auth.SignInResponse;
import org.rouplex.service.benchmark.auth.SignOutResponse;

@Api(value = "Benchmark Authenticator", description = "Service offering authentication and authorization of users")
public class BenchmarkAuthServiceResource extends ResourceConfig implements BenchmarkAuthService {

    @ApiOperation(value = "Returns the UserInfo, if the user is logged in or the redirect url for proper login")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public SignInResponse signIn(String sessionIdViaCookie,
                                 String authProvider, String authEmail, String authPassword,
                                 String sessionIdViaQueryParam, String code) throws Exception {

        return BenchmarkAuthServiceProvider.get().signIn(
                sessionIdViaCookie, authProvider, authEmail, authPassword, sessionIdViaQueryParam, code);
    }

    @Override
    public SignOutResponse signOut(String sessionIdViaHeaderParam) throws Exception {
        return BenchmarkAuthServiceProvider.get().signOut(sessionIdViaHeaderParam);
    }
}
