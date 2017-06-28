package org.rouplex.service.benchmark.orchestrator;

import javax.ws.rs.*;

@Path("/orchestrator")
public interface OrchestratorService {
    @POST
    @Path("tcp-echo-benchmarks")
    StartTcpEchoBenchmarkResponse startTcpEchoBenchmark(StartTcpEchoBenchmarkRequest request) throws Exception;

    @GET
    @Path("tcp-echo-benchmarks")
    ListTcpEchoBenchmarksResponse listTcpEchoBenchmarks(@QueryParam("includePublic") String includePublic) throws Exception;

    @GET
    @Path("tcp-echo-benchmarks/{benchmarkId}")
    DescribeTcpEchoBenchmarkResponse describeTcpEchoBenchmark(@PathParam("benchmarkId") String benchmarkId) throws Exception;
}