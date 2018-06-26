package org.rouplex.service.benchmark.worker;

import java.io.Closeable;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ScheduledExecutorService;

/**
 * A simple scheduler which seems to perform much better than its classic counterpart {@link ScheduledExecutorService}
 * This scheduler will use about 20%-30% less cpu in all cases we have tested, especially with lots of scheduled light
 * weight tasks.
 *
 * @author Andi Mullaraj (andimullaraj at gmail.com)
 */
public class RouplexScheduledExecutorFixedThreads implements Closeable {

    private final SortedMap<Long, Runnable> scheduledTasks = new TreeMap<>();
    private long nextTime;
    private boolean empty = true;
    private boolean closed;

    RouplexScheduledExecutorFixedThreads(int fixedThreadCount) {
        for (; fixedThreadCount > 0; fixedThreadCount--) {
            (new Thread() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            Runnable task;
                            synchronized (scheduledTasks) {
                                if (closed) {
                                    break;
                                }

                                if (empty) {
                                    scheduledTasks.wait();
                                    continue;
                                }

                                long delay = nextTime - System.nanoTime();
                                if (delay > 0) {
                                    scheduledTasks.wait(delay / 1_000_000, (int) (delay % 1_000_000));
                                    continue;
                                }

                                task = scheduledTasks.remove(nextTime);
                                if (!(empty = (scheduledTasks.size() == 0))) {
                                    if ((nextTime = scheduledTasks.firstKey()) - System.nanoTime() <= 0) {
                                        scheduledTasks.notify();
                                    }
                                }
                            }

                            task.run();
                        }
                    } catch (InterruptedException ie) {
                        // out of loop, return
                    }
                }
            }).start();
        }
    }

    void schedule(final Runnable runnable, long delayNano) {
        long timeNano = System.nanoTime() + delayNano;
        synchronized (scheduledTasks) {
            final Runnable taskAtSameTime;
            if ((taskAtSameTime = scheduledTasks.put(timeNano, runnable)) != null) {
                scheduledTasks.put(timeNano, new Runnable() {
                    @Override
                    public void run() {
                        taskAtSameTime.run();
                        runnable.run();
                    }
                });
            }

            if (empty || timeNano < nextTime) {
                empty = false;
                nextTime = timeNano;
                scheduledTasks.notify();
            }
        }
    }

    @Override
    public void close() {
        synchronized (scheduledTasks) {
            closed = true;
            scheduledTasks.notifyAll();
        }
    }
}
