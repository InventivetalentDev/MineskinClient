package org.mineskin;

import org.mineskin.data.JobInfo;
import org.mineskin.data.JobReference;
import org.mineskin.data.JobStatus;
import org.mineskin.exception.MineskinException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class JobChecker {

    private final MineSkinClient client;
    private JobInfo jobInfo;

    private final ScheduledExecutorService executor;
    private CompletableFuture<JobReference> future;

    private final AtomicInteger attempts = new AtomicInteger(0);
    private final int maxAttempts;
    private final int initialDelay;
    private final int interval;
    private final TimeUnit timeUnit;

    public JobChecker(MineSkinClient client, JobInfo jobInfo, ScheduledExecutorService executor, int maxAttempts, int initialDelaySeconds, int intervalSeconds) {
        this(client, jobInfo, executor, maxAttempts, initialDelaySeconds, intervalSeconds, TimeUnit.SECONDS);
    }

    public JobChecker(MineSkinClient client, JobInfo jobInfo, JobCheckOptions options) {
        this(client, jobInfo, options.scheduler(), options.maxAttempts(), options.initialDelayMillis(), options.intervalMillis(), TimeUnit.MILLISECONDS);
    }

    public JobChecker(MineSkinClient client, JobInfo jobInfo, ScheduledExecutorService executor, int maxAttempts, int initialDelay, int interval, TimeUnit timeUnit) {
        this.client = client;
        this.jobInfo = jobInfo;
        this.executor = executor;
        this.maxAttempts = maxAttempts;
        this.initialDelay = initialDelay;
        this.interval = interval;
        this.timeUnit = timeUnit;
    }

    public CompletableFuture<JobReference> check() {
        future = new CompletableFuture<>();
        executor.schedule(this::checkJob, initialDelay, timeUnit);
        return future;
    }

    private void checkJob() {
        if (attempts.incrementAndGet() > maxAttempts) {
            future.completeExceptionally(new MineskinException("Max attempts reached"));
            return;
        }
        client.queue().get(jobInfo)
                .thenAccept(response -> {
                    JobInfo info = response.getBody();
                    if (info != null) {
                        jobInfo = info;
                    }
                    if (jobInfo.status() == JobStatus.COMPLETED || jobInfo.status() == JobStatus.FAILED) {
                        future.complete(response);
                    } else {
                        executor.schedule(this::checkJob, interval, timeUnit);
                    }
                })
                .exceptionally(throwable -> {
                    future.completeExceptionally(throwable);
                    return null;
                });
    }

}
