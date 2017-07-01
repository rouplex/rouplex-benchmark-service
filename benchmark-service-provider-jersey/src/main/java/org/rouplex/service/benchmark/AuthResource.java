package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.auth.*;

@Api(value = "Benchmark Authenticator", description = "Service offering authentication and authorization of users")
public class AuthResource extends ResourceConfig implements AuthService {

//    @ApiOperation(value = "Returns the UserInfo, if the user is logged in or the redirect url for proper login")
//    @ApiResponses(value = {
//            @ApiResponse(code = 200, message = "Success"),
//            @ApiResponse(code = 500, message = "Error handling request")})
//    @Override
//    public SessionInfo signIn(String sessionIdViaCookie,
//                                 String authProvider, String authEmail, String authPassword,
//                                 String state, String code) throws Exception {
//
//        return AuthServiceProvider.get().signIn(
//                sessionIdViaCookie, authProvider, authEmail, authPassword, state, code);
//    }

    @ApiOperation(value = "Starts the sign in flow using google oauth2 scheme")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public StartSignInUsingGoogleOauth2Response startSignInUsingGoogleOauth2(String state) throws Exception {
        return AuthServiceProvider.get().startSignInUsingGoogleOauth2(state);
    }

    @ApiOperation(value = "Concludes the sign in flow using google oauth2 scheme")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public FinishSignInUsingGoogleOauth2Response finishSignInUsingGoogleOauth2(String state, String code) throws Exception {
        return AuthServiceProvider.get().finishSignInUsingGoogleOauth2(state, code);
    }

    @ApiOperation(value = "Sign in using basic auth scheme (userId/password)")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public SignInUsingBasicAuthResponse signInUsingBasicAuth(String userId, String password) throws Exception {
        return AuthServiceProvider.get().signInUsingBasicAuth(userId, password);
    }

    @ApiOperation(value = "Sign out")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public SignOutResponse signOut(String sessionIdViaHeaderParam) throws Exception {
        return AuthServiceProvider.get().signOut(sessionIdViaHeaderParam);
    }

    @ApiOperation(value = "Get UserInfo for the signed in user")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 500, message = "Error handling request")})
    @Override
    public GetSessionInfoResponse getSessionInfo(String sessionIdViaCookie, String sessionIdViaHeader) throws Exception {
        return AuthServiceProvider.get().getSessionInfo(sessionIdViaCookie, sessionIdViaHeader);
    }

    @Override
    public void setUserPreferences(String sessionIdViaHeader, UserPreferences userPreferences) throws Exception {
        AuthServiceProvider.get().setUserPreferences(sessionIdViaHeader, userPreferences);
    }
}
