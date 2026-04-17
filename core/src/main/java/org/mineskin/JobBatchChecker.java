package org.mineskin;

import org.mineskin.data.JobInfo;
import org.mineskin.data.JobReference;
import org.mineskin.data.JobStatus;
import org.mineskin.data.NullJobReference;
import org.mineskin.exception.MineskinException;
import org.mineskin.options.IJobCheckOptions;
import org.mineskin.response.JobListResponse;
import org.mineskin.response.JobResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * Shared job checker that batches status lookups across multiple concurrent
 * {@link MineSkinClient#queue()} waitForCompletion calls.
 *
 * <p>When more than {@link #BATCH_THRESHOLD} jobs are pending, polls the list
 * endpoint once per tick instead of hitting the per-job endpoint for each.
 * Only fetches the full per-job response (which includes the skin object) for
 * jobs that have transitioned to {@link JobStatus#COMPLETED}.</p>
 */
public class JobBatchChecker {

    /** Switch to list-based polling when more than this many jobs are pending. */
    public static final int BATCH_THRESHOLD = 4;

    private final MineSkinClient client;
    private final IJobCheckOptions options;

    private final Map<String, Pending> pending = new ConcurrentHashMap<>();
    private ScheduledFuture<?> scheduledCheck;

    public JobBatchChecker(MineSkinClient client, IJobCheckOptions options) {
        this.client = client;
        this.options = options;
    }

    /**
     * Register a job to be polled until done. If the same job id is already
     * registered, returns the existing future.
     */
    public CompletableFuture<JobReference> register(JobInfo jobInfo) {
        long firstDelayMillis;
        if (options.useEta() && jobInfo.eta() > 1) {
            long etaDelay = jobInfo.eta() - System.currentTimeMillis();
            firstDelayMillis = etaDelay > 0 ? etaDelay : options.initialDelayMillis();
        } else {
            firstDelayMillis = options.initialDelayMillis();
        }
        long nextCheckAt = System.currentTimeMillis() + firstDelayMillis;

        Pending p = new Pending(jobInfo, nextCheckAt);
        Pending prev = pending.putIfAbsent(jobInfo.id(), p);
        if (prev != null) {
            return prev.future;
        }
        client.getLogger().log(Level.FINER, "Registered job {0} for batch checking (first check in {1}ms)",
                new Object[]{jobInfo.id(), firstDelayMillis});
        rescheduleIfNeeded();
        return p.future;
    }

    private synchronized void rescheduleIfNeeded() {
        if (scheduledCheck != null) {
            scheduledCheck.cancel(false);
            scheduledCheck = null;
        }
        if (pending.isEmpty()) return;

        long soonest = Long.MAX_VALUE;
        for (Pending p : pending.values()) {
            if (p.nextCheckAt < soonest) soonest = p.nextCheckAt;
        }
        long delay = Math.max(0, soonest - System.currentTimeMillis());
        scheduledCheck = options.scheduler().schedule(this::tick, delay, TimeUnit.MILLISECONDS);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        List<Pending> due = new ArrayList<>();
        for (Pending p : pending.values()) {
            if (p.nextCheckAt > now) continue;
            int attempt = p.attempts.incrementAndGet();
            if (attempt > options.maxAttempts()) {
                p.future.completeExceptionally(new MineskinException("Max attempts reached")
                        .withBreadcrumb(p.jobInfo.getBreadcrumb()));
                pending.remove(p.jobInfo.id());
                continue;
            }
            // Push nextCheckAt forward immediately so that a concurrent reschedule
            // (e.g. from register()) arriving before the async response doesn't see
            // this job as still-due and double-count its attempts. The response
            // handler will overwrite with a correct `responseTime + interval`.
            p.nextCheckAt = now + options.interval().getInterval(attempt);
            due.add(p);
        }

        if (due.isEmpty()) {
            rescheduleIfNeeded();
            return;
        }

        if (pending.size() > BATCH_THRESHOLD) {
            checkViaList(due);
        } else {
            checkIndividually(due);
        }
    }

    private void checkViaList(List<Pending> due) {
        client.getLogger().log(Level.FINER, "Batch-checking {0} pending jobs via list endpoint",
                pending.size());
        client.queue().list().whenComplete((response, throwable) -> {
            try {
                if (throwable != null) {
                    // Match existing JobChecker semantics: request exceptions fail the waiting jobs.
                    for (Pending p : due) {
                        p.future.completeExceptionally(throwable);
                        pending.remove(p.jobInfo.id());
                    }
                    return;
                }
                Map<String, JobInfo> byId = indexById(response);
                long now = System.currentTimeMillis();
                // Process ALL pending jobs — the list response gives us everything for free.
                for (Pending p : new ArrayList<>(pending.values())) {
                    JobInfo updated = byId.get(p.jobInfo.id());
                    if (updated != null) {
                        p.jobInfo = updated;
                    } else if (!due.contains(p)) {
                        // Not due and not in list response — leave as-is for its own tick.
                        continue;
                    }
                    handleStatus(p, now);
                }
            } finally {
                rescheduleIfNeeded();
            }
        });
    }

    private void checkIndividually(List<Pending> due) {
        AtomicInteger remaining = new AtomicInteger(due.size());
        for (Pending p : due) {
            client.queue().get(p.jobInfo).whenComplete((response, throwable) -> {
                try {
                    if (throwable != null) {
                        p.future.completeExceptionally(throwable);
                        pending.remove(p.jobInfo.id());
                        return;
                    }
                    JobInfo info = response.getBody();
                    if (info != null) {
                        p.jobInfo = info;
                    }
                    JobStatus status = p.jobInfo.status();
                    if (status == JobStatus.FAILED ||
                            (status == JobStatus.COMPLETED && p.jobInfo.result().isPresent())) {
                        p.future.complete(response);
                        pending.remove(p.jobInfo.id());
                    } else {
                        p.nextCheckAt = System.currentTimeMillis()
                                + options.interval().getInterval(p.attempts.get());
                    }
                } finally {
                    if (remaining.decrementAndGet() == 0) {
                        rescheduleIfNeeded();
                    }
                }
            });
        }
    }

    private void handleStatus(Pending p, long now) {
        JobStatus status = p.jobInfo.status();
        if (status == JobStatus.FAILED) {
            // List endpoint doesn't return error details, and the result field is empty
            // for failures — return a NullJobReference so callers still get the updated JobInfo.
            p.future.complete(new NullJobReference(p.jobInfo));
            pending.remove(p.jobInfo.id());
            return;
        }
        if (status == JobStatus.COMPLETED && p.jobInfo.result().isPresent()) {
            // List only has the skin uuid in `result`; fetch the full response for the skin object.
            pending.remove(p.jobInfo.id());
            fetchFullAndComplete(p);
            return;
        }
        // Still pending — back off based on the current attempt number.
        p.nextCheckAt = now + options.interval().getInterval(p.attempts.get());
    }

    private void fetchFullAndComplete(Pending p) {
        client.queue().get(p.jobInfo).whenComplete((JobResponse response, Throwable throwable) -> {
            if (throwable != null) {
                p.future.completeExceptionally(throwable);
            } else {
                p.future.complete(response);
            }
        });
    }

    private static Map<String, JobInfo> indexById(JobListResponse response) {
        List<JobInfo> jobs = response.getJobs();
        if (jobs == null || jobs.isEmpty()) return Map.of();
        Map<String, JobInfo> byId = new HashMap<>(jobs.size());
        for (JobInfo j : jobs) {
            if (j.id() != null) byId.put(j.id(), j);
        }
        return byId;
    }

    private static final class Pending {
        volatile JobInfo jobInfo;
        final CompletableFuture<JobReference> future = new CompletableFuture<>();
        final AtomicInteger attempts = new AtomicInteger(0);
        volatile long nextCheckAt;

        Pending(JobInfo jobInfo, long nextCheckAt) {
            this.jobInfo = jobInfo;
            this.nextCheckAt = nextCheckAt;
        }
    }

}
