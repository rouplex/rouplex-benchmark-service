package org.rouplex.service;

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;
import org.rouplex.platform.jersey.RouplexJerseyApplication;
import org.rouplex.service.benchmark.AuthResource;
import org.rouplex.service.benchmark.OrchestratorResource;
import org.rouplex.service.benchmark.auth.AuthException;
import org.rouplex.service.benchmark.auth.AuthServiceProvider;
import org.rouplex.service.benchmark.orchestrator.OrchestratorServiceProvider;
import org.rouplex.service.deployment.DeploymentManagementResource;
import org.rouplex.service.deployment.DeploymentResource;
import org.rouplex.service.deployment.DeploymentServiceProvider;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.util.logging.Logger;

/**
 * This is the webapp, or the main jersey {@link javax.ws.rs.core.Application} which binds all the jersey resources.
 * The container searches for the {@link ApplicationPath} annotation and instantiates an instance of this class. It is
 * only in the constructor that we can bind (or add) resources to it, the jersey API does not allow for anything else.
 */
@ApplicationPath("/rest")
public class BenchmarkServiceApplication extends RouplexJerseyApplication implements Closeable {
    private static Logger logger = Logger.getLogger(BenchmarkServiceApplication.class.getSimpleName());

    public BenchmarkServiceApplication(@Context ServletContext servletContext) {
        super(servletContext);
    }

    @Override
    protected void postConstruct() {
        register(new BenchmarkResponseFilter());

        bindRouplexResource(AuthResource.class, true);
        bindRouplexResource(DeploymentResource.class, true);
        bindRouplexResource(DeploymentManagementResource.class, true);
        bindRouplexResource(OrchestratorResource.class, true);

        try {
            // instantiate early
            AuthServiceProvider.get();
            DeploymentServiceProvider.get();
            OrchestratorServiceProvider.get();
        } catch (Exception e) {
            String errorMessage = String.format("Could not instantiate services. Cause: %s: %s",
                    e.getClass().getSimpleName(), e.getMessage());

            logger.severe(errorMessage);
            getSwaggerBeanConfig().setDescription(errorMessage);
            return;
        }

        try {
            String publicIp = getPublicIp();

            getSwaggerBeanConfig().setDescription(String.format(
                    "jconsole service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi", publicIp, publicIp));
        } catch (Throwable t) {
            String errorMessage = String.format("Could not locate the public ip address. Cause: %s: %s",
                    t.getClass().getSimpleName(), t.getMessage());

            logger.severe(errorMessage);
            getSwaggerBeanConfig().setDescription(errorMessage);
        }

        super.postConstruct();
    }

    @Override
    protected void initExceptionMapper() {
        register(new SevereExceptionMapper());
        register(new AuthExceptionMapper());
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

    private class AuthExceptionMapper implements ExceptionMapper<AuthException> {
        final SevereExceptionMapper severeExceptionMapper = new SevereExceptionMapper();
        @Override
        public Response toResponse(AuthException authException) {
            try {
                return Response
                        .temporaryRedirect(URI.create("/index.html"))
                        .type(MediaType.TEXT_PLAIN_TYPE)
                        .build();
            } catch (Exception e) {
                return severeExceptionMapper.toResponse(e);
            }
        }
    }

    private String getPublicIp() throws IOException {
        try {
            return getPublicIpAssumingEC2Environment();
        } catch (Throwable t) {
            // add Google/Azure/Whatever related calls to get the public ip. Nothing for now, so we rethrow
            throw new IOException("Could not get public ip address", t);
        }
    }

    private String getPublicIpAssumingEC2Environment() {
        String instanceId = EC2MetadataUtils.getInstanceId();
        String region = EC2MetadataUtils.getEC2InstanceRegion();

        return AmazonEC2Client.builder()
                .withRegion(region)
                .build()
                .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId))
                .getReservations().iterator().next()
                .getInstances().iterator().next()
                .getPublicIpAddress();
    }

    @Override
    public void close() throws IOException {
        // todo
    }
}
