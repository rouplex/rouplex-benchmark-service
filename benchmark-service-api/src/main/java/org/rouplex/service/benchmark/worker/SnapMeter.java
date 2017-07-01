package org.rouplex.service.benchmark.worker;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class SnapMeter extends SnapCounter {
    double meanRate, oneMinRate, fiveMinRate, fifteenMinRate;
    String rateUnit;

    public SnapMeter(long count, double meanRate, double oneMinRate, double fiveMinRate, double fifteenMinRate, String rateUnit) {
        super(count);

        this.meanRate = meanRate;
        this.oneMinRate = oneMinRate;
        this.fiveMinRate = fiveMinRate;
        this.fifteenMinRate = fifteenMinRate;
        this.rateUnit = rateUnit;
    }

    public double getMeanRate() {
        return meanRate;
    }

    public void setMeanRate(double meanRate) {
        this.meanRate = meanRate;
    }

    public double getOneMinRate() {
        return oneMinRate;
    }

    public void setOneMinRate(double oneMinRate) {
        this.oneMinRate = oneMinRate;
    }

    public double getFiveMinRate() {
        return fiveMinRate;
    }

    public void setFiveMinRate(double fiveMinRate) {
        this.fiveMinRate = fiveMinRate;
    }

    public double getFifteenMinRate() {
        return fifteenMinRate;
    }

    public void setFifteenMinRate(double fifteenMinRate) {
        this.fifteenMinRate = fifteenMinRate;
    }

    public String getRateUnit() {
        return rateUnit;
    }

    public void setRateUnit(String rateUnit) {
        this.rateUnit = rateUnit;
    }
}