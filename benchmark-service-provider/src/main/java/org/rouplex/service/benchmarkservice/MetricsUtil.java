package org.rouplex.service.benchmarkservice;

import com.codahale.metrics.*;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapCounter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapHistogram;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapMeter;
import org.rouplex.service.benchmarkservice.tcp.metric.SnapTimer;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class MetricsUtil {
    private final double rateFactor;
    private final String rateUnit;

    MetricsUtil(TimeUnit rateUnit) {
        this.rateFactor = (double)rateUnit.toSeconds(1L);
        String s = rateUnit.toString().toLowerCase(Locale.US);
        this.rateUnit = s.substring(0, s.length() - 1);
    }

    SnapCounter snapCounter(Counter counter) {
        return new SnapCounter(counter.getCount());
    }

    SnapMeter snapMeter(Meter meter) {
        return new SnapMeter(meter.getCount(), convertRate(meter.getMeanRate()), convertRate(meter.getOneMinuteRate()),
                convertRate(meter.getFiveMinuteRate()), convertRate(meter.getFifteenMinuteRate()), rateUnit);
    }

    SnapHistogram snapHistogram(Histogram histogram) {
        Snapshot snapshot = histogram.getSnapshot();
        return new SnapHistogram(histogram.getCount(), snapshot.getMin(), snapshot.getMax(), snapshot.getMean(),
                snapshot.getStdDev(), snapshot.getMedian(), snapshot.get75thPercentile(), snapshot.get95thPercentile(),
                snapshot.get98thPercentile(), snapshot.get99thPercentile(), snapshot.get999thPercentile());
    }

    SnapTimer snapTimer(Timer timer) {
        Snapshot snapshot = timer.getSnapshot();
        return new SnapTimer(timer.getCount(), convertRate(timer.getMeanRate()), convertRate(timer.getOneMinuteRate()),
                convertRate(timer.getFiveMinuteRate()), convertRate(timer.getFifteenMinuteRate()), rateUnit,
                snapshot.getMin(), snapshot.getMax(), snapshot.getMean(), snapshot.getStdDev(), snapshot.getMedian(),
                snapshot.get75thPercentile(), snapshot.get95thPercentile(), snapshot.get98thPercentile(),
                snapshot.get99thPercentile(), snapshot.get999thPercentile());
    }

    protected double convertRate(double rate) {
        return rate * this.rateFactor;
    }
}
