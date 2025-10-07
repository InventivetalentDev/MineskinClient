package org.mineskin;

import org.mineskin.options.AutoGenerateQueueOptions;
import org.mineskin.options.GenerateQueueOptions;
import org.mineskin.options.GetQueueOptions;
import org.mineskin.options.IQueueOptions;

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
     *
     * @deprecated use {@link org.mineskin.options.GenerateQueueOptions#create(ScheduledExecutorService)}
     */
    @Deprecated
    public static QueueOptions createGenerate(ScheduledExecutorService scheduler) {
        return GenerateQueueOptions.create(scheduler);
    }

    /**
     * Creates a QueueOptions instance with default values for generate requests (200ms interval, 1 concurrent request).
     *
     * @deprecated use {@link org.mineskin.options.GenerateQueueOptions#create()}
     */
    @Deprecated
    public static QueueOptions createGenerate() {
        return GenerateQueueOptions.create();
    }

    /**
     * Creates a QueueOptions instance that automatically adjusts the interval and concurrency based on the user's allowance.
     *
     * @see AutoGenerateQueueOptions
     * @deprecated use {@link GenerateQueueOptions#createAuto(ScheduledExecutorService)}
     */
    @Deprecated
    public static AutoGenerateQueueOptions createAutoGenerate(ScheduledExecutorService scheduler) {
        return GenerateQueueOptions.createAuto(scheduler);
    }

    /**
     * Creates a QueueOptions instance that automatically adjusts the interval and concurrency based on the user's allowance.
     *
     * @see AutoGenerateQueueOptions
     * @deprecated use {@link GenerateQueueOptions#createAuto()}
     */
    @Deprecated
    public static AutoGenerateQueueOptions createAutoGenerate() {
        return GenerateQueueOptions.createAuto();
    }

    /**
     * Creates a QueueOptions instance with default values for get requests (100ms interval, 5 concurrent requests).
     *
     * @deprecated use {@link org.mineskin.options.GetQueueOptions#create(ScheduledExecutorService)}
     */
    @Deprecated
    public static QueueOptions createGet(ScheduledExecutorService scheduler) {
        return GetQueueOptions.create(scheduler);
    }

    /**
     * Creates a QueueOptions instance with default values for get requests (100ms interval, 5 concurrent requests).
     *
     * @deprecated use {@link GetQueueOptions#create()}
     */
    @Deprecated
    public static QueueOptions createGet() {
        return GetQueueOptions.create();
    }

    public QueueOptions withInterval(int interval, TimeUnit unit) {
        return new QueueOptions(scheduler, (int) unit.toMillis(interval), concurrency);
    }

    public QueueOptions withConcurrency(int concurrency) {
        return new QueueOptions(scheduler, intervalMillis, concurrency);
    }

}
