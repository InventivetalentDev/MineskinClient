package org.mineskin;

import org.mineskin.options.IJobCheckOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @param scheduler          Executor service to run the job checks - this should be a single-threaded scheduler
 * @param intervalMillis     Interval in milliseconds between each job check, default is 1000
 * @param initialDelayMillis Initial delay in milliseconds before the first job check, default is 2000
 * @param maxAttempts        Maximum number of attempts to check the job status, default is 10
 * @param useEta             Whether to use the ETA provided by the server to schedule the first check, default is false
 */
public record JobCheckOptions(
        ScheduledExecutorService scheduler,
        int intervalMillis,
        int initialDelayMillis,
        int maxAttempts,
        boolean useEta
) implements IJobCheckOptions {

    public JobCheckOptions(
            int intervalMillis,
            int initialDelayMillis,
            int maxAttempts
    ) {
        this(Executors.newSingleThreadScheduledExecutor(), intervalMillis, initialDelayMillis, maxAttempts, false);
    }

    public JobCheckOptions(
            int intervalMillis,
            int initialDelayMillis,
            int maxAttempts,
            boolean useEta
    ) {
        this(Executors.newSingleThreadScheduledExecutor(), intervalMillis, initialDelayMillis, maxAttempts, useEta);
    }

    public JobCheckOptions(
            ScheduledExecutorService scheduler,
            int intervalMillis,
            int initialDelayMillis,
            int maxAttempts
    ) {
        this(scheduler, intervalMillis, initialDelayMillis, maxAttempts, false);
    }

}
