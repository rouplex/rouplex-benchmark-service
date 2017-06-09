package org.rouplex.service;

import org.rouplex.service.benchmark.auth.SignInResponse;
import org.rouplex.service.benchmark.auth.SignOutResponse;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import java.io.IOException;

//@Provider
public class BenchmarkResponseFilter implements ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext request,  ContainerResponseContext response) throws IOException {
        if (response.getEntity() instanceof SignInResponse) {
            String sessionId = ((SignInResponse) response.getEntity()).getSessionId();
            response.getHeaders().add("Rouplex-SessionId", sessionId);

            String cookieEnabled = request.getHeaders().getFirst("Rouplex-Cookie-Enabled");
            if (cookieEnabled != null && "true".equals(cookieEnabled.toLowerCase())) {
                response.getHeaders().add("Set-Cookie", "Rouplex-SessionId=" + sessionId);
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
        response.getHeaders().add("Access-Control-Allow-Headers", "Rouplex-Auth-Email, Rouplex-Auth-Password, Rouplex-Cookie-Enabled, Rouplex-SessionId, origin, content-type, accept, authorization");
        response.getHeaders().add("Access-Control-Allow-Credentials", "true");
        response.getHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
    }
}
