package org.rouplex.service.benchmark.auth;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/benchmark/auth")
public interface BenchmarkAuthService {
    @GET
    @Path("/login")
    String login() throws Exception;
}