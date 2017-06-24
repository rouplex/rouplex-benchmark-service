package org.rouplex.service.benchmark.worker;

import java.util.SortedMap;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class GetMetricsResponse {
    SortedMap<String, SnapCounter> counters;
    SortedMap<String, SnapMeter> meters;
    SortedMap<String, SnapHistogram> histograms;
    SortedMap<String, SnapTimer> timers;

    public SortedMap<String, SnapCounter> getCounters() {
        return counters;
    }

    public void setCounters(SortedMap<String, SnapCounter> counters) {
        this.counters = counters;
    }

    public SortedMap<String, SnapMeter> getMeters() {
        return meters;
    }

    public void setMeters(SortedMap<String, SnapMeter> meters) {
        this.meters = meters;
    }

    public SortedMap<String, SnapHistogram> getHistograms() {
        return histograms;
    }

    public void setHistograms(SortedMap<String, SnapHistogram> histograms) {
        this.histograms = histograms;
    }

    public SortedMap<String, SnapTimer> getTimers() {
        return timers;
    }

    public void setTimers(SortedMap<String, SnapTimer> timers) {
        this.timers = timers;
    }


}
