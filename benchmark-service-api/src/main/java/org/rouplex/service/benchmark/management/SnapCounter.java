package org.rouplex.service.benchmark.management;

/**
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class SnapCounter {
    long count;

    public SnapCounter(long count) {
        this.count = count;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}

