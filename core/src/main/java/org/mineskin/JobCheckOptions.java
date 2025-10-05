package org.mineskin;

import org.mineskin.options.IJobCheckOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @param scheduler          Executor service to run the job checks - this should be a single-threaded scheduler
 * @param intervalMillis     Interval in milliseconds between each job check, default is 1000
 * @param initialDelayMillis Initial delay in milliseconds before the first job check, default is 2000
 * @param maxAttempts        Maximum number of attempts to check the job status, default is 10
 * @param useEta             Whether to use the estimated completion time provided by the server to schedule the first check, default is false
 */
public record JobCheckOptions(
        ScheduledExecutorService scheduler,
        int intervalMillis,
        int initialDelayMillis,
        int maxAttempts,
        boolean useEta
) implements IJobCheckOptions {

    @Deprecated
    public JobCheckOptions(
            ScheduledExecutorService scheduler,
            int intervalMillis,
            int initialDelayMillis,
            int maxAttempts
    ) {
        this(scheduler, intervalMillis, initialDelayMillis, maxAttempts, false);
    }

    /**
     * Creates a JobCheckOptions instance with default values.
     */
    public static JobCheckOptions create(ScheduledExecutorService scheduler) {
        return new JobCheckOptions(
                scheduler,
                1000,
                2000,
                10,
                false
        );
    }

    /**
     * Creates a JobCheckOptions instance with default values.
     */
    public static JobCheckOptions create() {
        return create(Executors.newSingleThreadScheduledExecutor());
    }

    public JobCheckOptions withInterval(int intervalMillis) {
        return new JobCheckOptions(scheduler, intervalMillis, initialDelayMillis, maxAttempts, useEta);
    }

    public JobCheckOptions withInitialDelay(int initialDelayMillis) {
        return new JobCheckOptions(scheduler, intervalMillis, initialDelayMillis, maxAttempts, useEta);
    }

    public JobCheckOptions withMaxAttempts(int maxAttempts) {
        return new JobCheckOptions(scheduler, intervalMillis, initialDelayMillis, maxAttempts, useEta);
    }

    /**
     * Sets the option to use the estimated completion time provided by the server to schedule the first check.
     */
    public JobCheckOptions withUseEta() {
        return new JobCheckOptions(scheduler, intervalMillis, initialDelayMillis, maxAttempts, true);
    }

}
