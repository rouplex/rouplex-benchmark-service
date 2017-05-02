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
    EC2_M416xlarge;

//    removing these temporary till we have authentication/authorization in place
//    M2Xlarge("m2.xlarge"),
//    M22xlarge("m2.2xlarge"),
//    M24xlarge("m2.4xlarge"),
//    Cr18xlarge("cr1.8xlarge"),
//    R3Large("r3.large"),
//    R3Xlarge("r3.xlarge"),
//    R32xlarge("r3.2xlarge"),
//    R34xlarge("r3.4xlarge"),
//    R38xlarge("r3.8xlarge"),
//    R4Large("r4.large"),
//    R4Xlarge("r4.xlarge"),
//    R42xlarge("r4.2xlarge"),
//    R44xlarge("r4.4xlarge"),
//    R48xlarge("r4.8xlarge"),
//    R416xlarge("r4.16xlarge"),
//    X116xlarge("x1.16xlarge"),
//    X132xlarge("x1.32xlarge"),
//    I2Xlarge("i2.xlarge"),
//    I22xlarge("i2.2xlarge"),
//    I24xlarge("i2.4xlarge"),
//    I28xlarge("i2.8xlarge"),
//    I3Large("i3.large"),
//    I3Xlarge("i3.xlarge"),
//    I32xlarge("i3.2xlarge"),
//    I34xlarge("i3.4xlarge"),
//    I38xlarge("i3.8xlarge"),
//    I316xlarge("i3.16xlarge"),
//    Hi14xlarge("hi1.4xlarge"),
//    Hs18xlarge("hs1.8xlarge"),
//    C1Medium("c1.medium"),
//    C1Xlarge("c1.xlarge"),
//    C3Large("c3.large"),
//    C3Xlarge("c3.xlarge"),
//    C32xlarge("c3.2xlarge"),
//    C34xlarge("c3.4xlarge"),
//    C38xlarge("c3.8xlarge"),
//    C4Large("c4.large"),
//    C4Xlarge("c4.xlarge"),
//    C42xlarge("c4.2xlarge"),
//    C44xlarge("c4.4xlarge"),
//    C48xlarge("c4.8xlarge"),
//    Cc14xlarge("cc1.4xlarge"),
//    Cc28xlarge("cc2.8xlarge"),
//    G22xlarge("g2.2xlarge"),
//    G28xlarge("g2.8xlarge"),
//    Cg14xlarge("cg1.4xlarge"),
//    P2Xlarge("p2.xlarge"),
//    P28xlarge("p2.8xlarge"),
//    P216xlarge("p2.16xlarge"),
//    D2Xlarge("d2.xlarge"),
//    D22xlarge("d2.2xlarge"),
//    D24xlarge("d2.4xlarge"),
//    D28xlarge("d2.8xlarge"),
//    F12xlarge("f1.2xlarge"),
//    F116xlarge("f1.16xlarge");

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