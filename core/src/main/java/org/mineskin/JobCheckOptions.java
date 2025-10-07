package org.mineskin;

import org.mineskin.options.IJobCheckOptions;
import org.mineskin.request.backoff.RequestInterval;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Example:
 * <pre>
 *     JobCheckOptions.create()
 *           .withUseEta()
 *           .withInterval(RequestInterval.exponential())
 *           .withMaxAttempts(50)
 * </pre>
 */
public final class JobCheckOptions implements IJobCheckOptions {

    private final ScheduledExecutorService scheduler;
    private final RequestInterval interval;
    private final int initialDelayMillis;
    private final int maxAttempts;
    private final boolean useEta;

    /**
     * @param scheduler          Executor service to run the job checks - this should be a single-threaded scheduler
     * @param interval           Interval strategy between each request, see {@link RequestInterval}
     * @param initialDelayMillis Initial delay in milliseconds before the first job check, default is 2000
     * @param maxAttempts        Maximum number of attempts to check the job status, default is 10
     * @param useEta             Whether to use the estimated completion time provided by the server to schedule the first check, default is false
     */
    @Deprecated
    public JobCheckOptions(
            ScheduledExecutorService scheduler,
            RequestInterval interval,
            int initialDelayMillis,
            int maxAttempts,
            boolean useEta
    ) {
        this.scheduler = scheduler;
        this.interval = interval;
        this.initialDelayMillis = initialDelayMillis;
        this.maxAttempts = maxAttempts;
        this.useEta = useEta;
    }

    /**
     * @param scheduler          Executor service to run the job checks - this should be a single-threaded scheduler
     * @param intervalMillis     Interval in milliseconds between each job check, default is 1000
     * @param initialDelayMillis Initial delay in milliseconds before the first job check, default is 2000
     * @param maxAttempts        Maximum number of attempts to check the job status, default is 10
     * @param useEta             Whether to use the estimated completion time provided by the server to schedule the first check, default is false
     */
    @Deprecated
    public JobCheckOptions(
            ScheduledExecutorService scheduler,
            int intervalMillis,
            int initialDelayMillis,
            int maxAttempts,
            boolean useEta
    ) {
        this.scheduler = scheduler;
        this.interval = RequestInterval.constant(intervalMillis);
        this.initialDelayMillis = initialDelayMillis;
        this.maxAttempts = maxAttempts;
        this.useEta = useEta;
    }

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

    public JobCheckOptions withInterval(RequestInterval interval) {
        return new JobCheckOptions(scheduler, interval, initialDelayMillis, maxAttempts, useEta);
    }

    public JobCheckOptions withInitialDelay(int initialDelayMillis) {
        return new JobCheckOptions(scheduler, interval, initialDelayMillis, maxAttempts, useEta);
    }

    public JobCheckOptions withInitialDelay(int initialDelay, TimeUnit unit) {
        return new JobCheckOptions(scheduler, interval, (int) unit.toMillis(initialDelay), maxAttempts, useEta);
    }

    public JobCheckOptions withMaxAttempts(int maxAttempts) {
        return new JobCheckOptions(scheduler, interval, initialDelayMillis, maxAttempts, useEta);
    }

    /**
     * Sets the option to use the estimated completion time provided by the server to schedule the first check.
     */
    public JobCheckOptions withUseEta() {
        return new JobCheckOptions(scheduler, interval, initialDelayMillis, maxAttempts, true);
    }

    @Override
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public RequestInterval interval() {
        return interval;
    }

    @Deprecated
    @Override
    public int intervalMillis() {
        return interval.getInterval(1);
    }

    @Override
    public int initialDelayMillis() {
        return initialDelayMillis;
    }

    @Override
    public int maxAttempts() {
        return maxAttempts;
    }

    @Override
    public boolean useEta() {
        return useEta;
    }

}
