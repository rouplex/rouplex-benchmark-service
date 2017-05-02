package org.rouplex.service.benchmark.orchestrator;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark/orchestrator")
public interface BenchmarkOrchestratorService {
    @POST
    @Path("tcp/start")
    StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception;
}