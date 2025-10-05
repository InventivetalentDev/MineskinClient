package org.mineskin;

import org.mineskin.options.IQueueOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @param scheduler      Executor service to run the queue - this should be a single-threaded scheduler
 * @param intervalMillis Interval in milliseconds between each request
 * @param concurrency    Maximum number of concurrent requests
 */
public record QueueOptions(
        ScheduledExecutorService scheduler,
        int intervalMillis,
        int concurrency
) implements IQueueOptions {

    public QueueOptions(int intervalMillis, int concurrency) {
        this(Executors.newSingleThreadScheduledExecutor(), intervalMillis, concurrency);
    }

}
