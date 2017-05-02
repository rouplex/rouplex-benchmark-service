package org.rouplex.service.benchmark;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.glassfish.jersey.server.ResourceConfig;
import org.rouplex.service.benchmark.orchestrator.BenchmarkOrchestratorService;
import org.rouplex.service.benchmark.orchestrator.BenchmarkOrchestratorServiceProvider;
import org.rouplex.service.benchmark.orchestrator.StartDistributedTcpBenchmarkRequest;
import org.rouplex.service.benchmark.orchestrator.StartDistributedTcpBenchmarkResponse;

@Api(value = "Benchmark Orchestrator", description = "Service offering multipoint benchmarking functionality")
public class BenchmarkOrchestratorServiceResource extends ResourceConfig implements BenchmarkOrchestratorService {

    @ApiOperation(value = "Start a distributed benchmarking scenario with one server and a number of client instances")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success"),
            @ApiResponse(code = 500, message = "Error handling request")})
    @Override
//    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public StartDistributedTcpBenchmarkResponse startDistributedTcpBenchmark(StartDistributedTcpBenchmarkRequest request) throws Exception {
        return BenchmarkOrchestratorServiceProvider.get().startDistributedTcpBenchmark(request);
    }
}
