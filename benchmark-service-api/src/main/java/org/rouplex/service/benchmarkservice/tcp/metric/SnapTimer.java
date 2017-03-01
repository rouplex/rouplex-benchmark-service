package org.rouplex.service.benchmarkservice.tcp.metric;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class SnapTimer extends SnapMeter {
    long min, max;
    double mean, stddev, median, pc75, pc95, pc98, pc99, pc999;

    public SnapTimer(long count, double meanRate, double oneMinRate, double fiveMinRate, double fifteenMinRate, String rateUnit,
            long min, long max, double mean, double stddev, double median,
            double pc75, double pc95, double pc98, double pc99, double pc999) {
        super(count, meanRate, oneMinRate, fiveMinRate, fifteenMinRate, rateUnit);

        this.min = min;
        this.max = max;
        this.mean = mean;
        this.stddev = stddev;
        this.median = median;
        this.pc75 = pc75;
        this.pc95 = pc95;
        this.pc98 = pc98;
        this.pc99 = pc99;
        this.pc999 = pc999;
    }

    public long getMin() {
        return min;
    }

    public void setMin(long min) {
        this.min = min;
    }

    public long getMax() {
        return max;
    }

    public void setMax(long max) {
        this.max = max;
    }

    public double getMean() {
        return mean;
    }

    public void setMean(double mean) {
        this.mean = mean;
    }

    public double getStddev() {
        return stddev;
    }

    public void setStddev(double stddev) {
        this.stddev = stddev;
    }

    public double getMedian() {
        return median;
    }

    public void setMedian(double median) {
        this.median = median;
    }

    public double getPc75() {
        return pc75;
    }

    public void setPc75(double pc75) {
        this.pc75 = pc75;
    }

    public double getPc95() {
        return pc95;
    }

    public void setPc95(double pc95) {
        this.pc95 = pc95;
    }

    public double getPc98() {
        return pc98;
    }

    public void setPc98(double pc98) {
        this.pc98 = pc98;
    }

    public double getPc99() {
        return pc99;
    }

    public void setPc99(double pc99) {
        this.pc99 = pc99;
    }

    public double getPc999() {
        return pc999;
    }

    public void setPc999(double pc999) {
        this.pc999 = pc999;
    }
}
