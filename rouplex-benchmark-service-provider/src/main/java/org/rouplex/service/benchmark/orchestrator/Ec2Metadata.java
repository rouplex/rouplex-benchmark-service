package org.rouplex.service.benchmark.orchestrator;

import org.rouplex.service.deployment.HostType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class Ec2Metadata {
    private static final Ec2Metadata EC_2_METADATA = new Ec2Metadata();

    public static Ec2Metadata get() {
        return EC_2_METADATA;
    }

    private final Map<HostType, Ec2HostTypeInfo> ec2HostTypes = new HashMap<HostType, Ec2HostTypeInfo>();

    Ec2Metadata() {
        ec2HostTypes.put(HostType.T2Nano, new Ec2HostTypeInfo().withRamMB(512).withCost(0.0059));
        ec2HostTypes.put(HostType.T2Micro, new Ec2HostTypeInfo().withRamMB(1024).withCost(0.0120));
        ec2HostTypes.put(HostType.T2Small, new Ec2HostTypeInfo().withRamMB(2 * 1024).withCost(0.0230));
        ec2HostTypes.put(HostType.T2Medium, new Ec2HostTypeInfo().withRamMB(4 * 1024).withCost(0.0470));
        ec2HostTypes.put(HostType.T2Large, new Ec2HostTypeInfo().withRamMB(8 * 1024).withCost(0.0940));
        ec2HostTypes.put(HostType.T2Xlarge, new Ec2HostTypeInfo().withRamMB(16 * 1024).withCost(0.1880));
        ec2HostTypes.put(HostType.T22xlarge, new Ec2HostTypeInfo().withRamMB(32 * 1024).withCost(0.3760));

        ec2HostTypes.put(HostType.M3Medium, new Ec2HostTypeInfo().withRamMB(3750).withCost(.0670));
        ec2HostTypes.put(HostType.M3Large, new Ec2HostTypeInfo().withRamMB(7500).withCost(.1330));
        ec2HostTypes.put(HostType.M3Xlarge, new Ec2HostTypeInfo().withRamMB(15000).withCost(.2660));
        ec2HostTypes.put(HostType.M32xlarge, new Ec2HostTypeInfo().withRamMB(30000).withCost(.5320));

        ec2HostTypes.put(HostType.M4Large, new Ec2HostTypeInfo().withRamMB(8 * 1024).withCost(.1000));
        ec2HostTypes.put(HostType.M4Xlarge, new Ec2HostTypeInfo().withRamMB(16 * 1024).withCost(.2000));
        ec2HostTypes.put(HostType.M42xlarge, new Ec2HostTypeInfo().withRamMB(32 * 1024).withCost(.4000));
        ec2HostTypes.put(HostType.M44xlarge, new Ec2HostTypeInfo().withRamMB(64 * 1024).withCost(.8000));
        ec2HostTypes.put(HostType.M410xlarge, new Ec2HostTypeInfo().withRamMB(160 * 1024).withCost(2.0000));
        ec2HostTypes.put(HostType.M416xlarge, new Ec2HostTypeInfo().withRamMB(256 * 1024).withCost(3.2000));
    }

    public Map<HostType, Ec2HostTypeInfo> getEc2HostTypes() {
        return ec2HostTypes;
    }
}
