package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class EC2Metadata {
    private static final EC2Metadata ec2Metadata = new EC2Metadata();

    public static EC2Metadata get() {
        return ec2Metadata;
    }

    private final Map<HostType, EC2InstanceTypeDescriptor> ec2InstanceTypes = new HashMap<HostType, EC2InstanceTypeDescriptor>();

    EC2Metadata() {
        ec2InstanceTypes.put(HostType.EC2_T2Nano, new EC2InstanceTypeDescriptor().withRamMB(512).withCost(0.0059));
        ec2InstanceTypes.put(HostType.EC2_T2Micro, new EC2InstanceTypeDescriptor().withRamMB(1024).withCost(0.0120));
        ec2InstanceTypes.put(HostType.EC2_T2Small, new EC2InstanceTypeDescriptor().withRamMB(2 * 1024).withCost(0.0230));
        ec2InstanceTypes.put(HostType.EC2_T2Medium, new EC2InstanceTypeDescriptor().withRamMB(4 * 1024).withCost(0.0470));
        ec2InstanceTypes.put(HostType.EC2_T2Large, new EC2InstanceTypeDescriptor().withRamMB(8 * 1024).withCost(0.0940));
        ec2InstanceTypes.put(HostType.EC2_T2Xlarge, new EC2InstanceTypeDescriptor().withRamMB(16 * 1024).withCost(0.1880));
        ec2InstanceTypes.put(HostType.EC2_T22xlarge, new EC2InstanceTypeDescriptor().withRamMB(32 * 1024).withCost(0.3760));

        ec2InstanceTypes.put(HostType.EC2_M3Medium, new EC2InstanceTypeDescriptor().withRamMB(3750).withCost(.0670));
        ec2InstanceTypes.put(HostType.EC2_M3Large, new EC2InstanceTypeDescriptor().withRamMB(7500).withCost(.1330));
        ec2InstanceTypes.put(HostType.EC2_M3Xlarge, new EC2InstanceTypeDescriptor().withRamMB(15000).withCost(.2660));
        ec2InstanceTypes.put(HostType.EC2_M32xlarge, new EC2InstanceTypeDescriptor().withRamMB(30000).withCost(.5320));

        ec2InstanceTypes.put(HostType.EC2_M4Large, new EC2InstanceTypeDescriptor().withRamMB(8 * 1024).withCost(.1000));
        ec2InstanceTypes.put(HostType.EC2_M4Xlarge, new EC2InstanceTypeDescriptor().withRamMB(16 * 1024).withCost(.2000));
        ec2InstanceTypes.put(HostType.EC2_M42xlarge, new EC2InstanceTypeDescriptor().withRamMB(32 * 1024).withCost(.4000));
        ec2InstanceTypes.put(HostType.EC2_M44xlarge, new EC2InstanceTypeDescriptor().withRamMB(64 * 1024).withCost(.8000));
        ec2InstanceTypes.put(HostType.EC2_M410xlarge, new EC2InstanceTypeDescriptor().withRamMB(160 * 1024).withCost(2.0000));
        ec2InstanceTypes.put(HostType.EC2_M416xlarge, new EC2InstanceTypeDescriptor().withRamMB(256 * 1024).withCost(3.2000));
    }

    public Map<HostType, EC2InstanceTypeDescriptor> getEc2InstanceTypes() {
        return ec2InstanceTypes;
    }
}
