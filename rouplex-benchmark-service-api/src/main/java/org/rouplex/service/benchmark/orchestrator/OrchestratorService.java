package org.rouplex.service.benchmark.orchestrator;

import javax.ws.rs.*;
import java.util.Set;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@Path("/benchmark/orchestrator")
public interface OrchestratorService {
    @POST
    @Path("tcp-echo-benchmarks")
    TcpEchoBenchmark createTcpEchoBenchmark(CreateTcpEchoBenchmarkRequest request) throws Exception;

    @PUT
    @Path("tcp-echo-benchmarks/{id}")
    TcpEchoBenchmark createTcpEchoBenchmark(@PathParam("id") String id, CreateTcpEchoBenchmarkRequest request) throws Exception;

    @GET
    @Path("tcp-echo-benchmarks")
    Set<String> listTcpEchoBenchmarks(@QueryParam("includePublic") String includePublic) throws Exception;

    @GET
    @Path("tcp-echo-benchmarks/{benchmarkId}")
    TcpEchoBenchmark getTcpEchoBenchmark(@PathParam("benchmarkId") String benchmarkId) throws Exception;
}