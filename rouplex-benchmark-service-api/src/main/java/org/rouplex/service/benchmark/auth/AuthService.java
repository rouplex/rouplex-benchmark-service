package org.rouplex.service.benchmark.auth;

import javax.ws.rs.*;

/**
 * A very simple authentication service to authenticate individuals that will be submitting benchmark jobs.
 * It offers authentication via user email and password as well as oauth2 for users with a google/gmail account.
 *
 * Though defined via jax-rs, this is not a REST service.
 *
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@Path("/benchmark/auth")
public interface AuthService {
    @GET
    @Path("/start-sign-in-using-google-oauth2")
    StartSignInUsingGoogleOauth2Response startSignInUsingGoogleOauth2(
        @QueryParam("state") String state
    ) throws Exception;

    @GET
    @Path("/finish-sign-in-using-google-oauth2")
    FinishSignInUsingGoogleOauth2Response finishSignInUsingGoogleOauth2(
        @QueryParam("state") String state,
        @QueryParam("code") String code
    ) throws Exception;

    @GET
    @Path("/sign-in-using-basic-auth")
    SignInUsingBasicAuthResponse signInUsingBasicAuth(
        @HeaderParam("Rouplex-Auth-UserId") String userId,
        @HeaderParam("Rouplex-Auth-Password") String password
    ) throws Exception;

    @GET
    @Path("/sign-out")
    SignOutResponse signOut(
        @HeaderParam("Rouplex-SessionId") String sessionIdViaHeader
    ) throws Exception;

    @GET
    @Path("/session-info")
    GetSessionInfoResponse getSessionInfo(
        @CookieParam("Rouplex-SessionId") String sessionIdViaCookie,
        @HeaderParam("Rouplex-SessionId") String sessionIdViaHeader
    ) throws Exception;

    @POST
    @Path("/user-preferences")
    void setUserPreferences(
        @HeaderParam("Rouplex-SessionId") String sessionIdViaHeader,
        UserPreferences userPreferences
    ) throws Exception;
}