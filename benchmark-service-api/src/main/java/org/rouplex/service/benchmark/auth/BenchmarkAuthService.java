package org.rouplex.service.benchmark.auth;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

@Path("/benchmark/auth")
public interface BenchmarkAuthService {
    GoogleAuthResponse googleAuth(String code, String authUser, String sessionState, String prompt) throws Exception;

    @GET
    @Path("/google")
    Response googleAuthWithCorsHeaders(
            @QueryParam("code") String code,
            @QueryParam("authuser") String authUser,
            @QueryParam("session_state") String sessionState,
            @QueryParam("prompt") String prompt) throws Exception;

    @GET
    @Path("/rouplex")
    String rouplexAuth(
            @QueryParam("email") String email,
            @QueryParam("password") String password) throws Exception;
}