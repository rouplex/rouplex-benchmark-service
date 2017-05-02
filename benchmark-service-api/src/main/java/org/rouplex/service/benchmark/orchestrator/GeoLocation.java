package org.rouplex.service.benchmark.orchestrator;

import java.util.HashMap;
import java.util.Map;

public enum GeoLocation {
    EC2_US_WEST_2,
    EC2_GovCloud,
    EC2_US_EAST_1,
    EC2_US_EAST_2,
    EC2_US_WEST_1,
    EC2_EU_WEST_1,
    EC2_EU_WEST_2,
    EC2_EU_CENTRAL_1,
    EC2_AP_SOUTH_1,
    EC2_AP_SOUTHEAST_1,
    EC2_AP_SOUTHEAST_2,
    EC2_AP_NORTHEAST_1,
    EC2_AP_NORTHEAST_2,
    EC2_SA_EAST_1,
    EC2_CN_NORTH_1,
    EC2_CA_CENTRAL_1;

    static final Map<GeoLocation, String> enumToString = new HashMap<GeoLocation, String>() {{
        put(EC2_US_WEST_2, "us-west-2");
        put(EC2_GovCloud, "us-gov-west-1");
        put(EC2_US_EAST_1, "us-east-1");
        put(EC2_US_EAST_2, "us-east-2");
        put(EC2_US_WEST_1, "us-west-1");
        put(EC2_EU_WEST_1, "eu-west-1");
        put(EC2_EU_WEST_2, "eu-west-2");
        put(EC2_EU_CENTRAL_1, "eu-central-1");
        put(EC2_AP_SOUTH_1, "ap-south-1");
        put(EC2_AP_SOUTHEAST_1, "ap-southeast-1");
        put(EC2_AP_SOUTHEAST_2, "ap-southeast-2");
        put(EC2_AP_NORTHEAST_1, "ap-northeast-1");
        put(EC2_AP_NORTHEAST_2, "ap-northeast-2");
        put(EC2_SA_EAST_1, "sa-east-1");
        put(EC2_CN_NORTH_1, "cn-north-1");
        put(EC2_CA_CENTRAL_1, "ca-central-1");
    }};

    static final Map<String, GeoLocation> stringToEnum = new HashMap<String, GeoLocation>() {{
        for (Map.Entry<GeoLocation, String> entry : enumToString.entrySet()) {
            put(entry.getValue(), entry.getKey());
        }
    }};

    public static GeoLocation fromString(String string) {
        GeoLocation geoLocation = stringToEnum.get(string);
        if (geoLocation != null) {
            return geoLocation;
        }

        throw new IllegalArgumentException("Cannot create enum from " + string + " value!");
    }

    @Override
    public String toString() {
        return enumToString.get(this);
    }
}
