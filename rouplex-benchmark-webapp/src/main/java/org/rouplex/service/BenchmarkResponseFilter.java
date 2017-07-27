package org.rouplex.service;

import org.rouplex.service.benchmark.auth.GetSessionInfoResponse;
import org.rouplex.service.benchmark.auth.SignOutResponse;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.NewCookie;
import java.io.IOException;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
//@Provider
public class BenchmarkResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (response.getEntity() instanceof GetSessionInfoResponse) {
            String sessionId = ((GetSessionInfoResponse) response.getEntity()).getSessionInfo().getSessionId();
            response.getHeaders().add("Rouplex-SessionId", sessionId);

            String cookieEnabled = request.getHeaders().getFirst("Rouplex-Cookie-Enabled");
            if (cookieEnabled != null && "true".equals(cookieEnabled.toLowerCase())) {
                NewCookie newCookie = new NewCookie("Rouplex-SessionId", sessionId, null/*path*/, null/*domain*/, "Session id", -1, false);
                response.getHeaders().add("Set-Cookie", newCookie);
            }
        }

        if (response.getEntity() instanceof SignOutResponse) {
            response.getHeaders().add("Rouplex-SessionId", null);

            String cookieEnabled = request.getHeaders().getFirst("Rouplex-Cookie-Enabled");
            if (cookieEnabled != null && "true".equals(cookieEnabled.toLowerCase())) {
                response.getHeaders().add("Set-Cookie", "Rouplex-SessionId=");
            }
        }

        response.getHeaders().add("Access-Control-Allow-Origin", "*");
        response.getHeaders().add("Access-Control-Allow-Headers", "Rouplex-Auth-UserId, Rouplex-Auth-Password, Rouplex-Cookie-Enabled, Rouplex-SessionId, Rouplex-TimeOffsetInMinutes, origin, content-type, accept, authorization");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}
