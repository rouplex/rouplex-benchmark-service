package org.rouplex.service.benchmark.orchestrator;

import com.amazonaws.services.ec2.model.Region;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class InstanceDescriptor {
    Region ec2Region;
    String imageId;
    String privateIpAddress;
    String publicIpAddress;
    long expiration;

    public Region getEc2Region() {
        return ec2Region;
    }

    public void setEc2Region(Region ec2Region) {
        this.ec2Region = ec2Region;
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

    public long getExpiration() {
        return expiration;
    }

    public void setExpiration(long expiration) {
        this.expiration = expiration;
    }
}
