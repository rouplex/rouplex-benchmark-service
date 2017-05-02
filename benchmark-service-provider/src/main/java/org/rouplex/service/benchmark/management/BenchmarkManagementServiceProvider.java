package org.rouplex.service.benchmark.management;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.util.EC2MetadataUtils;

import java.io.Closeable;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BenchmarkManagementServiceProvider implements BenchmarkManagementService, Closeable {
    private static final Logger logger = Logger.getLogger(BenchmarkManagementServiceProvider.class.getSimpleName());
    private static final SimpleDateFormat UTC_DATE_FORMAT = new SimpleDateFormat("YYYY-MM-DD'T'hh:mm:ssZ");

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
    private final ExecutorService executorService =  Executors.newSingleThreadExecutor();

    BenchmarkManagementServiceProvider() {
        executorService.submit(new Runnable() {
            @Override
            public void run() {
                while (!executorService.isShutdown()) {
                    long timeStart = System.currentTimeMillis();

                    // terminate self if beyond the lease
                    if (System.currentTimeMillis() > leaseEnd) {
                        logger.severe("Terminating self. Cause: Expired lease");

                        AmazonEC2 amazonEC2Client = AmazonEC2Client.builder().withCredentials(new AWSCredentialsProvider() {
                            @Override
                            public AWSCredentials getCredentials() {
                                return new BasicAWSCredentials("AKIAI7HYQJMBH36ZZGEQ", "k7rdZDPUfWwD+3bnJB8fPhyHh29LVtB2Wb9ZJ1qL");
                            }

                            @Override
                            public void refresh() {

                            }
                        }).withRegion(EC2MetadataUtils.getEC2InstanceRegion()).build();

                        // aaa temp amazonEC2Client = AmazonEC2Client.builder().withRegion(EC2MetadataUtils.getEC2InstanceRegion()).build();

                        amazonEC2Client.createTags(new CreateTagsRequest().withResources(EC2MetadataUtils.getInstanceId())
                                .withTags(new Tag().withKey("State").withValue("Self terminated due to lease expiration")));

                        amazonEC2Client.terminateInstances(new TerminateInstancesRequest().withInstanceIds(EC2MetadataUtils.getInstanceId()));
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
                leaseEnd = UTC_DATE_FORMAT.parse(request.getLeaseEndAsIsoInstant()).getTime();
                logger.info(String.format("Configured service to auto terminate at %s", request.getLeaseEndAsIsoInstant()));
            } catch (Exception e) {
                logger.warning(String.format("Failed configuring service to auto terminate at %s", request.getLeaseEndAsIsoInstant()));
                // leaseEnd keeps the existing value
            }
        }

        return new ConfigureServiceResponse();
    }

    @Override
    public void close() throws IOException {
        executorService.shutdownNow();
    }
}