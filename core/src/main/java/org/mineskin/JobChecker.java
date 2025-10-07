package org.mineskin;

import org.mineskin.data.JobInfo;
import org.mineskin.data.JobReference;
import org.mineskin.data.JobStatus;
import org.mineskin.exception.MineskinException;
import org.mineskin.options.IJobCheckOptions;
import org.mineskin.request.backoff.RequestInterval;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

public class JobChecker {

    private final MineSkinClient client;
    private JobInfo jobInfo;

    private final ScheduledExecutorService executor;
    private CompletableFuture<JobReference> future;

    private final AtomicInteger attempts = new AtomicInteger(0);
    private final int maxAttempts;
    private final int initialDelay;
    private final RequestInterval interval;
    private final TimeUnit timeUnit;
    private final boolean useEta;

    public JobChecker(MineSkinClient client, JobInfo jobInfo, IJobCheckOptions options) {
        this.client = client;
        this.jobInfo = jobInfo;
        this.executor = options.scheduler();
        this.maxAttempts = options.maxAttempts();
        this.initialDelay = options.initialDelayMillis();
        this.interval = options.interval();
        this.timeUnit = TimeUnit.MILLISECONDS;
        this.useEta = options.useEta();
    }

    @Deprecated
    public JobChecker(MineSkinClient client, JobInfo jobInfo, ScheduledExecutorService executor, int maxAttempts, int initialDelaySeconds, int intervalSeconds) {
        this(client, jobInfo, executor, maxAttempts, initialDelaySeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    @Deprecated
    public JobChecker(MineSkinClient client, JobInfo jobInfo, ScheduledExecutorService executor, int maxAttempts, int initialDelay, int interval, TimeUnit timeUnit) {
        this(client, jobInfo, executor, maxAttempts, initialDelay, interval, timeUnit, false);
    }

    @Deprecated
    public JobChecker(MineSkinClient client, JobInfo jobInfo, ScheduledExecutorService executor, int maxAttempts, int initialDelay, int interval, TimeUnit timeUnit, boolean useEta) {
        this.client = client;
        this.jobInfo = jobInfo;
        this.executor = executor;
        this.maxAttempts = maxAttempts;
        this.initialDelay = (int) timeUnit.toMillis(initialDelay);
        this.interval = RequestInterval.constant((int) timeUnit.toMillis(interval));
        this.timeUnit = timeUnit;
        this.useEta = useEta;
    }

    /**
     * Starts checking the job status. Only call this once.
     *
     * @return A future that completes when the job is completed or failed, or exceptionally if an error occurs or max attempts is reached.
     */
    public CompletableFuture<JobReference> check() {
        future = new CompletableFuture<>();

        // Try to use the ETA to schedule the first check
        if (useEta && jobInfo.eta() > 1) {
            long delay = jobInfo.eta() - System.currentTimeMillis();
            if (delay > 0) {
                client.getLogger().log(Level.FINER, "Scheduling first job check in {0}ms based on ETA", delay);
                executor.schedule(this::checkJob, delay, TimeUnit.MILLISECONDS);
                return future;
            }
        }

        // or just use the initial delay
        executor.schedule(this::checkJob, initialDelay, timeUnit);

        return future;
    }

    private void checkJob() {
        int attempt = attempts.incrementAndGet();
        if (attempt > maxAttempts) {
            future.completeExceptionally(new MineskinException("Max attempts reached").withBreadcrumb(jobInfo.getBreadcrumb()));
            return;
        }
        client.queue().get(jobInfo)
                .thenAccept(response -> {
                    JobInfo info = response.getBody();
                    if (info != null) {
                        jobInfo = info;
//                        client.getLogger().log(Level.FINER, "ETA {0} {1}", new Object[]{info.eta(), info.getBreadcrumb()});
                    }
                    if (jobInfo.status() == JobStatus.COMPLETED || jobInfo.status() == JobStatus.FAILED) {
                        future.complete(response);
                    } else {
                        executor.schedule(this::checkJob, interval.getInterval(attempt), timeUnit);
                    }
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
    }

}
