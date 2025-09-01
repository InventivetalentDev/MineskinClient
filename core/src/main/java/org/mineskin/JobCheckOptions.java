package org.mineskin;

import org.mineskin.options.IJobCheckOptions;

import java.util.concurrent.ScheduledExecutorService;

/**
 * @param scheduler Executor service to run the job checks
 * @param intervalMillis Interval in milliseconds between each job check, default is 1000
 * @param initialDelayMillis Initial delay in milliseconds before the first job check, default is 2000
 * @param maxAttempts Maximum number of attempts to check the job status, default is 10
 */
public record JobCheckOptions(
        ScheduledExecutorService scheduler,
        int intervalMillis,
        int initialDelayMillis,
        int maxAttempts
) implements IJobCheckOptions {
}
