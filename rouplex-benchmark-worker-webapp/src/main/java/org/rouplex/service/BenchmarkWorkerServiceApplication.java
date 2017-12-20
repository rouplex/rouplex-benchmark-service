package org.rouplex.service;

import org.rouplex.platform.jersey.RouplexJerseyApplication;
import org.rouplex.service.benchmark.WorkerResource;
import org.rouplex.service.benchmark.worker.RouplexWorkerServiceProvider;
import org.rouplex.service.benchmark.worker.WorkerService;
import org.rouplex.service.deployment.management.agent.DeploymentAgent;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.util.logging.Logger;

/**
 * This application hosts an instance of a {@link WorkerResource} to get and handle {@link WorkerService} requests.
 *
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
@ApplicationPath("/rest")
public class BenchmarkWorkerServiceApplication extends RouplexJerseyApplication implements Closeable {
    private static Logger logger = Logger.getLogger(BenchmarkWorkerServiceApplication.class.getSimpleName());

    private DeploymentAgent deploymentAgent;

    public BenchmarkWorkerServiceApplication(@Context ServletContext servletContext) {
        super(servletContext);
    }

    @Override
    protected void postConstruct() {
        bindRouplexResource(WorkerResource.class, true);

        try {
            // instantiate early
            RouplexWorkerServiceProvider.get();
            deploymentAgent = DeploymentAgent.get();
        } catch (Exception e) {
            String errorMessage = String.format("Could not instantiate services. Cause: %s: %s",
                    e.getClass().getSimpleName(), e.getMessage());

            logger.severe(errorMessage);
            getSwaggerBeanConfig().setDescription(errorMessage);
            return;
        }

        super.postConstruct();
    }

    @Override
    protected void initExceptionMapper() {
        register(new SevereExceptionMapper());
    }

    private class SevereExceptionMapper implements ExceptionMapper<Exception> {
        @Override
        public Response toResponse(Exception exception) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            exception.printStackTrace(new PrintStream(os));
            logger.severe("Stack trace: " + new String(os.toByteArray()));

            return Response.status(500).entity(new ExceptionEntity(exception)).build();
        }
    }

    @Override
    public void close() throws IOException {
        // todo
        if (deploymentAgent != null) {
            deploymentAgent.close();
        }
    }
}
