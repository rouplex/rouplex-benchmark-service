package org.rouplex.service.benchmark.management;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;
import org.rouplex.service.benchmark.Util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class BenchmarkManagementServiceProvider implements BenchmarkManagementService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkManagementServiceProvider.class.getSimpleName());

    private static BenchmarkManagementService benchmarkManagementService;

    public static BenchmarkManagementService get() throws Exception {
        synchronized (BenchmarkManagementServiceProvider.class) {
            if (benchmarkManagementService == null) {
                benchmarkManagementService = new BenchmarkManagementServiceProvider();
            }

            return benchmarkManagementService;
        }
    }

    // just under one hour, which is the unit of pricing for EC2 -- this will be updated by orchestrator anyway
    private long leaseEnd = System.currentTimeMillis() + 55 * 60 * 1000;
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    BenchmarkManagementServiceProvider() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (!executorService.isShutdown()) {
                    long timeStart = System.currentTimeMillis();

                    // terminate self if beyond the lease
                    if (System.currentTimeMillis() > leaseEnd) {
                        logger.severe("Terminating self. Cause: Expired lease");

                        try {
                            AmazonEC2 amazonEC2Client = AmazonEC2ClientBuilder.standard()
                                    .withRegion(EC2MetadataUtils.getEC2InstanceRegion()).build();

                            amazonEC2Client.createTags(new CreateTagsRequest()
                                    .withResources(EC2MetadataUtils.getInstanceId())
                                    .withTags(new Tag()
                                            .withKey("State")
                                            .withValue("Self terminated due to lease expiration")));

                            amazonEC2Client.terminateInstances(new TerminateInstancesRequest()
                                    .withInstanceIds(EC2MetadataUtils.getInstanceId()));
                        } catch (RuntimeException re) {
                            logger.severe(String.format(
                                    "Could not terminate self. Cause: %s: %s", re.getClass(), re.getMessage()));
                        }
                    }

                    // once a minute lease updates are more than enough
                    long waitMillis = timeStart + 60 * 1000 - System.currentTimeMillis();
                    if (waitMillis > 0) {
                        synchronized (executorService) {
                            try {
                                executorService.wait(waitMillis);
                            } catch (InterruptedException ie) {
                                break;
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    public GetServiceStateResponse getServiceState(GetServiceStateRequest request) throws Exception {
        GetServiceStateResponse response = new GetServiceStateResponse();
        response.setServiceState(ServiceState.RUNNING);
        return response;
    }

    @Override
    public ConfigureServiceResponse configureService(ConfigureServiceRequest request) throws Exception {
        synchronized (executorService) {
            try {
                leaseEnd = Util.convertIsoInstantToMillis(request.getLeaseEndAsIsoInstant());
                logger.info(String.format("Configured service to auto terminate at %s", request.getLeaseEndAsIsoInstant()));

                executorService.notifyAll();
            } catch (Exception e) {
                logger.warning(String.format("Failed configuring service to auto terminate at %s. Cause: %s: %s",
                        request.getLeaseEndAsIsoInstant(), e.getClass(), e.getMessage()));
                // leaseEnd keeps the existing value
            }
        }

        return new ConfigureServiceResponse();
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
        synchronized (executorService) {
            executorService.notifyAll();
        }
    }
}