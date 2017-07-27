package org.rouplex.service.benchmark.auth;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class GetSessionInfoResponse {
    private final SessionInfo sessionInfo;

    public GetSessionInfoResponse(SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    public SessionInfo getSessionInfo() {
        return sessionInfo;
    }
}
