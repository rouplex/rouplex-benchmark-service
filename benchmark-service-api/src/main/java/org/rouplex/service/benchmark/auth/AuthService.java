package org.rouplex.service.benchmark.auth;

import javax.ws.rs.*;

@Path("/auth")
public interface AuthService {
//
//    /**
//     * Authenticate the caller by first searching for an active session with sessionIdViaCookie, then by using the
//     * appropriate provider and resolving / authenticating the user via the respective provider params. We anticipate
//     * the query params from various oauth providers may overlap, and that is fine, since they will be used as
//     * semantically defined by the appropriate provider.
//     * <p>
//     * Usually, this will be the first call the index.html page will make, to determine if the user has a currently
//     * active session from the current browser.
//     *
//     * @param sessionIdViaCookie If the user has an active session from his current browser, this param will be sent automatically
//     * @param authProvider       google, rouplex (facebook, amazon coming soon)
//     * @param authEmail          User's email, if authenticating via authEmail / authPassword
//     * @param authPassword       User's password, if authenticating via authEmail / authPassword
//     * @param state              In google's scenario this represents a freshly minted sessionId which was passed to google
//     *                           authentication service as a callback param
//     * @param code               In google's scenario this represents the initial authCode which can be used to retrieve a user token
//     *                           and subsequently get user data (such as authEmail, profile etc.)
//     * @return The response containing a {@link UserInfo} along with the current sessionId if the user was
//     * authenticated or nulls otherwise
//     * @throws Exception If any exception occurred during authentication process
//     */
//    @GET
//    @Path("/sign-in")
//    SessionInfo signIn(@CookieParam("Rouplex-SessionId") String sessionIdViaCookie,
//                          @QueryParam("provider") String authProvider,
//                          @HeaderParam("Rouplex-Auth-Email") String authEmail,
//                          @HeaderParam("Rouplex-Auth-Password") String authPassword,
//                          @QueryParam("state") String state,
//                          @QueryParam("code") String code
//    ) throws Exception;

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
}