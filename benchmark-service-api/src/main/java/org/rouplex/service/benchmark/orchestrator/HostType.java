package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

public enum HostType {
    //    T1Micro("t1.micro"), // not available anymore
    EC2_T2Nano,
    EC2_T2Micro,
    EC2_T2Small,
    EC2_T2Medium,
    EC2_T2Large,
    EC2_T2Xlarge,
    EC2_T22xlarge,
    EC2_M1Small,
    EC2_M1Medium,
    EC2_M1Large,
    EC2_M1Xlarge,
    EC2_M3Medium,
    EC2_M3Large,
    EC2_M3Xlarge,
    EC2_M32xlarge,
    EC2_M4Large,
    EC2_M4Xlarge,
    EC2_M42xlarge,
    EC2_M44xlarge,
    EC2_M410xlarge,
    EC2_M416xlarge,
    EC2_M2Xlarge,
    EC2_M22xlarge,
    EC2_M24xlarge,
    EC2_Cr18xlarge,
    EC2_R3Large,
    EC2_R3Xlarge,
    EC2_R32xlarge,
    EC2_R34xlarge,
    EC2_R38xlarge,
    EC2_R4Large,
    EC2_R4Xlarge,
    EC2_R42xlarge,
    EC2_R44xlarge,
    EC2_R48xlarge,
    EC2_R416xlarge,
    EC2_X116xlarge,
    EC2_X132xlarge,
    EC2_I2Xlarge,
    EC2_I22xlarge,
    EC2_I24xlarge,
    EC2_I28xlarge,
    EC2_I3Large,
    EC2_I3Xlarge,
    EC2_I32xlarge,
    EC2_I34xlarge,
    EC2_I38xlarge,
    EC2_I316xlarge,
    EC2_Hi14xlarge,
    EC2_Hs18xlarge,
    EC2_C1Medium,
    EC2_C1Xlarge,
    EC2_C3Large,
    EC2_C3Xlarge,
    EC2_C32xlarge,
    EC2_C34xlarge,
    EC2_C38xlarge,
    EC2_C4Large,
    EC2_C4Xlarge,
    EC2_C42xlarge,
    EC2_C44xlarge,
    EC2_C48xlarge,
    EC2_Cc14xlarge,
    EC2_Cc28xlarge,
    EC2_G22xlarge,
    EC2_G28xlarge,
    EC2_Cg14xlarge,
    EC2_P2Xlarge,
    EC2_P28xlarge,
    EC2_P216xlarge,
    EC2_D2Xlarge,
    EC2_D22xlarge,
    EC2_D24xlarge,
    EC2_D28xlarge,
    EC2_F12xlarge,
    EC2_F116xlarge;

    static final Map<HostType, String> enumToString = new HashMap<HostType, String>() {{
        put(EC2_T2Nano, "t2.nano");
        put(EC2_T2Micro, "t2.micro");
        put(EC2_T2Small, "t2.small");
        put(EC2_T2Medium, "t2.medium");
        put(EC2_T2Large, "t2.large");
        put(EC2_T2Xlarge, "t2.xlarge");
        put(EC2_T22xlarge, "t2.2xlarge");
        put(EC2_M1Small, "m1.small");
        put(EC2_M1Medium, "m1.medium");
        put(EC2_M1Large, "m1.large");
        put(EC2_M1Xlarge, "m1.xlarge");
        put(EC2_M3Medium, "m3.medium");
        put(EC2_M3Large, "m3.large");
        put(EC2_M3Xlarge, "m3.xlarge");
        put(EC2_M32xlarge, "m3.2xlarge");
        put(EC2_M4Large, "m4.large");
        put(EC2_M4Xlarge, "m4.xlarge");
        put(EC2_M42xlarge, "m4.2xlarge");
        put(EC2_M44xlarge, "m4.4xlarge");
        put(EC2_M410xlarge, "m4.10xlarge");
        put(EC2_M416xlarge, "m4.16xlarge");
        put(EC2_M2Xlarge, "m2.xlarge");
        put(EC2_M22xlarge, "m2.2xlarge");
        put(EC2_M24xlarge, "m2.4xlarge");
        put(EC2_Cr18xlarge, "cr1.8xlarge");
        put(EC2_R3Large, "r3.large");
        put(EC2_R3Xlarge, "r3.xlarge");
        put(EC2_R32xlarge, "r3.2xlarge");
        put(EC2_R34xlarge, "r3.4xlarge");
        put(EC2_R38xlarge, "r3.8xlarge");
        put(EC2_R4Large, "r4.large");
        put(EC2_R4Xlarge, "r4.xlarge");
        put(EC2_R42xlarge, "r4.2xlarge");
        put(EC2_R44xlarge, "r4.4xlarge");
        put(EC2_R48xlarge, "r4.8xlarge");
        put(EC2_R416xlarge, "r4.16xlarge");
        put(EC2_X116xlarge, "x1.16xlarge");
        put(EC2_X132xlarge, "x1.32xlarge");
        put(EC2_I2Xlarge, "i2.xlarge");
        put(EC2_I22xlarge, "i2.2xlarge");
        put(EC2_I24xlarge, "i2.4xlarge");
        put(EC2_I28xlarge, "i2.8xlarge");
        put(EC2_I3Large, "i3.large");
        put(EC2_I3Xlarge, "i3.xlarge");
        put(EC2_I32xlarge, "i3.2xlarge");
        put(EC2_I34xlarge, "i3.4xlarge");
        put(EC2_I38xlarge, "i3.8xlarge");
        put(EC2_I316xlarge, "i3.16xlarge");
        put(EC2_Hi14xlarge, "hi1.4xlarge");
        put(EC2_Hs18xlarge, "hs1.8xlarge");
        put(EC2_C1Medium, "c1.medium");
        put(EC2_C1Xlarge, "c1.xlarge");
        put(EC2_C3Large, "c3.large");
        put(EC2_C3Xlarge, "c3.xlarge");
        put(EC2_C32xlarge, "c3.2xlarge");
        put(EC2_C34xlarge, "c3.4xlarge");
        put(EC2_C38xlarge, "c3.8xlarge");
        put(EC2_C4Large, "c4.large");
        put(EC2_C4Xlarge, "c4.xlarge");
        put(EC2_C42xlarge, "c4.2xlarge");
        put(EC2_C44xlarge, "c4.4xlarge");
        put(EC2_C48xlarge, "c4.8xlarge");
        put(EC2_Cc14xlarge, "cc1.4xlarge");
        put(EC2_Cc28xlarge, "cc2.8xlarge");
        put(EC2_G22xlarge, "g2.2xlarge");
        put(EC2_G28xlarge, "g2.8xlarge");
        put(EC2_Cg14xlarge, "cg1.4xlarge");
        put(EC2_P2Xlarge, "p2.xlarge");
        put(EC2_P28xlarge, "p2.8xlarge");
        put(EC2_P216xlarge, "p2.16xlarge");
        put(EC2_D2Xlarge, "d2.xlarge");
        put(EC2_D22xlarge, "d2.2xlarge");
        put(EC2_D24xlarge, "d2.4xlarge");
        put(EC2_D28xlarge, "d2.8xlarge");
        put(EC2_F12xlarge, "f1.2xlarge");
        put(EC2_F116xlarge, "f1.16xlarge");
    }};

    static final Map<String, HostType> stringToEnum = new HashMap<String, HostType>() {{
        for (Map.Entry<HostType, String> entry : enumToString.entrySet()) {
            put(entry.getValue(), entry.getKey());
        }
    }};

    public static HostType fromString(String string) {
        HostType hostType = stringToEnum.get(string);
        if (hostType != null) {
            return hostType;
        }

        throw new IllegalArgumentException("Cannot create enum from " + string + " value!");
    }

    @Override
    public String toString() {
        return enumToString.get(this);
    }
}