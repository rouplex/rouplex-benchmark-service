package org.rouplex.service.benchmark.orchestrator;

import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Region;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class InstanceDescriptor<T> {
    private Region ec2Region;
    private InstanceType ec2InstanceType;
    private String imageId;
    private String privateIpAddress;
    private String publicIpAddress;
    private long expirationTimestamp;
    private long terminationTimestamp;
    private T metadata;

    public Region getEc2Region() {
        return ec2Region;
    }

    public void setEc2Region(Region ec2Region) {
        this.ec2Region = ec2Region;
    }

    public InstanceType getEc2InstanceType() {
        return ec2InstanceType;
    }

    public void setEc2InstanceType(InstanceType ec2InstanceType) {
        this.ec2InstanceType = ec2InstanceType;
    }

    public String getImageId() {
        return imageId;
    }

    public void setImageId(String imageId) {
        this.imageId = imageId;
    }

    public String getPrivateIpAddress() {
        return privateIpAddress;
    }

    public void setPrivateIpAddress(String privateIpAddress) {
        this.privateIpAddress = privateIpAddress;
    }

    public String getPublicIpAddress() {
        return publicIpAddress;
    }

    public void setPublicIpAddress(String publicIpAddress) {
        this.publicIpAddress = publicIpAddress;
    }

    public long getExpirationTimestamp() {
        return expirationTimestamp;
    }

    public void setExpirationTimestamp(long expirationTimestamp) {
        this.expirationTimestamp = expirationTimestamp;
    }

    public long getTerminationTimestamp() {
        return terminationTimestamp;
    }

    public void setTerminationTimestamp(long terminationTimestamp) {
        this.terminationTimestamp = terminationTimestamp;
    }

    public T getMetadata() {
        return metadata;
    }

    public void setMetadata(T metadata) {
        this.metadata = metadata;
    }
}
