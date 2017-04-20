package org.rouplex.service;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;
import org.rouplex.platform.jersey.RouplexJerseyApplication;
import org.rouplex.service.benchmarkservice.BenchmarkServiceResource;

import javax.servlet.ServletContext;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import java.io.Closeable;
import java.util.logging.Logger;

/**
 * This is the webapp, or the main jersey {@link javax.ws.rs.core.Application} which binds all the jersey resources.
 * The container searches for the {@link ApplicationPath} annotation and instantiates an instance of this class. It is
 * only in the constructor that we can bind (or add) resources to it, the jersey API does not allow for anything else.
 */
@ApplicationPath("/rouplex")
public class BenchmarkServiceApplication extends RouplexJerseyApplication implements Closeable { // todo ApplicationEventListener
    private static Logger logger = Logger.getLogger(BenchmarkServiceApplication.class.getSimpleName());

    public BenchmarkServiceApplication(@Context ServletContext servletContext) {
        super(servletContext);

        bindRouplexResource(BenchmarkServiceResource.class, true);

        try {
            String instanceId = EC2MetadataUtils.getInstanceId();
            String region = EC2MetadataUtils.getEC2InstanceRegion();

            String publicIp = AmazonEC2Client.builder()
                    .withRegion(region)
                    .build()
                    .describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceId))
                    .getReservations().iterator().next()
                    .getInstances().iterator().next()
                    .getPublicIpAddress();

            getSwaggerBeanConfig().setDescription(String.format(
                    "jconsole service:jmx:rmi://%s:1705/jndi/rmi://%s:1706/jmxrmi", publicIp, publicIp));
        } catch (Throwable t) {
            String errorMessage = String.format("Could not locate the public ip address. Cause: %s: %s",
                    t.getClass().getSimpleName(), t.getMessage());

            logger.severe(errorMessage);
            getSwaggerBeanConfig().setDescription(errorMessage);
        }
    }

    @Override
    public void close() {
//        for (RouplexTcpServer rouplexTcpServer : BenchmarkServiceProvider.benchmarkTcpServers.values()) {
//            try {
//                rouplexTcpServer.close();
//            } catch (IOException ioe) {
//                // log it
//            }
//        }
    }
}
