package org.mineskin;

import org.mineskin.options.IQueueOptions;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @param scheduler Executor service to run the queue
 * @param intervalMillis Interval in milliseconds between each request
 * @param concurrency Maximum number of concurrent requests
 */
public record QueueOptions(
        ScheduledExecutorService scheduler,
        int intervalMillis,
        int concurrency
) implements IQueueOptions {
}
