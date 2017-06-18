package org.rouplex.service.benchmark.orchestrator;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark/orchestrator")
public interface BenchmarkOrchestratorService {
    @POST
    @Path("tcp/start")
    StartTcpBenchmarkResponse startTcpBenchmark(StartTcpBenchmarkRequest request) throws Exception;

    @POST
    @Path("tcp/describe")
    DescribeTcpBenchmarkResponse describeTcpBenchmark(DescribeTcpBenchmarkRequest request) throws Exception;
}