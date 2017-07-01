package org.rouplex.service.benchmark.auth;

public class StartSignInUsingGoogleOauth2Response extends GetSessionInfoResponse {
    private String redirectUrl;

    public StartSignInUsingGoogleOauth2Response(SessionInfo sessionInfo) {
        super(sessionInfo);
    }

    public String getRedirectUrl() {
        return redirectUrl;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }
}
