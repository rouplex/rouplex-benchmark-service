package org.rouplex.service.benchmark.orchestrator;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark/orchestrator")
public interface BenchmarkOrchestratorService {
//    @GET
//    @Path("oauth")
//    String oauth() throws Exception;
//
//    @GET
//    @Path("oauth2callback")
//    String oauth2callback() throws Exception;
//
    @POST
    @Path("tcp/start")
    StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception;
}