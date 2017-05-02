package org.rouplex.service.benchmark.orchestrator;

public enum GeoLocation {
    EC2_US_WEST_2("us-west-2"),
    EC2_GovCloud("us-gov-west-1"),
    EC2_US_EAST_1("us-east-1"),
    EC2_US_EAST_2("us-east-2"),
    EC2_US_WEST_1("us-west-1"),
    EC2_EU_WEST_1("eu-west-1"),
    EC2_EU_WEST_2("eu-west-2"),
    EC2_EU_CENTRAL_1("eu-central-1"),
    EC2_AP_SOUTH_1("ap-south-1"),
    EC2_AP_SOUTHEAST_1("ap-southeast-1"),
    EC2_AP_SOUTHEAST_2("ap-southeast-2"),
    EC2_AP_NORTHEAST_1("ap-northeast-1"),
    EC2_AP_NORTHEAST_2("ap-northeast-2"),
    EC2_SA_EAST_1("sa-east-1"),
    EC2_CN_NORTH_1("cn-north-1"),
    EC2_CA_CENTRAL_1("ca-central-1");

    private final String name;

    GeoLocation(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    public static GeoLocation fromName(String name) {
        GeoLocation[] var1 = values();
        int var2 = var1.length;

        for(int var3 = 0; var3 < var2; ++var3) {
            GeoLocation region = var1[var3];
            if(name.equals(region.toString())) {
                return region;
            }
        }

        throw new IllegalArgumentException("Cannot create enum from " + name + " value!");
    }
}
