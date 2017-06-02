package org.rouplex.service.benchmark.management;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class ConfigureServiceRequest {
    public static final String ISO_INSTANT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String LEASE_END_ISO_INSTANT_EXAMPLE = "2017-12-31T10:00:00.000-0800";

    String leaseEndAsIsoInstant;

    public String getLeaseEndAsIsoInstant() {
        return leaseEndAsIsoInstant;
    }

    public void setLeaseEndAsIsoInstant(String leaseEndAsIsoInstant) {
        this.leaseEndAsIsoInstant = leaseEndAsIsoInstant;
    }
}
