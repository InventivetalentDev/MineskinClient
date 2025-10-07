package org.mineskin;

import org.mineskin.options.AutoGenerateQueueOptions;
import org.mineskin.options.IQueueOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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

    /**
     * Creates a QueueOptions instance with default values for generate requests (200ms interval, 1 concurrent request).
     */
    public static QueueOptions createGenerate(ScheduledExecutorService scheduler) {
        return new QueueOptions(scheduler, 200, 1);
    }

    /**
     * Creates a QueueOptions instance with default values for generate requests (200ms interval, 1 concurrent request).
     */
    public static QueueOptions createGenerate() {
        return createGenerate(Executors.newSingleThreadScheduledExecutor());
    }

    /**
     * Creates a QueueOptions instance that automatically adjusts the interval and concurrency based on the user's allowance.
     *
     * @see AutoGenerateQueueOptions
     */
    public static AutoGenerateQueueOptions createAutoGenerate(ScheduledExecutorService scheduler) {
        return new AutoGenerateQueueOptions(scheduler);
    }

    /**
     * Creates a QueueOptions instance that automatically adjusts the interval and concurrency based on the user's allowance.
     *
     * @see AutoGenerateQueueOptions
     */
    public static AutoGenerateQueueOptions createAutoGenerate() {
        return createAutoGenerate(Executors.newSingleThreadScheduledExecutor());
    }

    /**
     * Creates a QueueOptions instance with default values for get requests (100ms interval, 5 concurrent requests).
     */
    public static QueueOptions createGet(ScheduledExecutorService scheduler) {
        return new QueueOptions(scheduler, 100, 5);
    }

    /**
     * Creates a QueueOptions instance with default values for get requests (100ms interval, 5 concurrent requests).
     */
    public static QueueOptions createGet() {
        return createGet(Executors.newSingleThreadScheduledExecutor());
    }

    public QueueOptions withInterval(int interval, TimeUnit unit) {
        return new QueueOptions(scheduler, (int) unit.toMillis(interval), concurrency);
    }

    public QueueOptions withConcurrency(int concurrency) {
        return new QueueOptions(scheduler, intervalMillis, concurrency);
    }

}
