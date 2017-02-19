package org.rouplex.service.benchmarkservice;

import org.rouplex.service.benchmarkservice.tcp.StartTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StartTcpServerResponse;
import org.rouplex.service.benchmarkservice.tcp.StopTcpServerRequest;
import org.rouplex.service.benchmarkservice.tcp.StopTcpServerResponse;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

@Path("/benchmark")
public interface BenchmarkService {
    @GET
    @Path("/ping")
    PingResponse ping();

    @POST
    @Path("/ping")
    PingResponse ping(PingRequest pingRequest);

    @POST
    @Path("/tcp/server/start")
    StartTcpServerResponse startTcpServer(StartTcpServerRequest request);

    @POST
    @Path("/tcp/server/stop")
    StopTcpServerResponse stopTcpServer(StopTcpServerRequest request);
}