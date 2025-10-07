package org.mineskin.options;

import org.mineskin.QueueOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class GenerateQueueOptions {

    /**
     * Creates a QueueOptions instance with default values for generate requests (200ms interval, 1 concurrent request).
     */
    public static QueueOptions create(ScheduledExecutorService scheduler) {
        return new QueueOptions(scheduler, 200, 1);
    }

    /**
     * Creates a QueueOptions instance with default values for generate requests (200ms interval, 1 concurrent request).
     */
    public static QueueOptions create() {
        return create(Executors.newSingleThreadScheduledExecutor());
    }

    /**
     * Creates a QueueOptions instance that automatically adjusts the interval and concurrency based on the user's allowance.
     *
     * @see AutoGenerateQueueOptions
     */
    public static AutoGenerateQueueOptions createAuto(ScheduledExecutorService scheduler) {
        return new AutoGenerateQueueOptions(scheduler);
    }

    /**
     * Creates a QueueOptions instance that automatically adjusts the interval and concurrency based on the user's allowance.
     *
     * @see AutoGenerateQueueOptions
     */
    public static AutoGenerateQueueOptions createAuto() {
        return createAuto(Executors.newSingleThreadScheduledExecutor());
    }

}
