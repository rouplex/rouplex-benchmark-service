package org.rouplex.service.benchmark.management;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class ConfigureServiceRequest {
    String leaseEndAsIsoInstant;

    public String getLeaseEndAsIsoInstant() {
        return leaseEndAsIsoInstant;
    }

    public void setLeaseEndAsIsoInstant(String leaseEndAsIsoInstant) {
        this.leaseEndAsIsoInstant = leaseEndAsIsoInstant;
    }
}
