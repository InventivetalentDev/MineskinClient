package org.mineskin.options;

import org.mineskin.MineSkinClient;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.User;
import org.mineskin.response.UserResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;
import java.util.logging.Level;

public class AutoGenerateQueueOptions implements IQueueOptions {

    private static final int MIN_INTERVAL_MILLIS = 100;
    private static final int MAX_INTERVAL_MILLIS = 1000;
    private static final int MIN_CONCURRENCY = 1;
    private static final int MAX_CONCURRENCY = 30;

    private static final int FAILURE_STEP_MILLIS = 500;
    private static final int MAX_PENALTY_MILLIS = 10_000;
    private static final int DECAY_MILLIS_PER_SECOND = 100;
    private static final int MAX_EFFECTIVE_INTERVAL_MILLIS = 10_000;

    private final ScheduledExecutorService scheduler;
    private final LongSupplier clock;
    private MineSkinClient client;

    private final AtomicLong lastRefresh = new AtomicLong(0);

    private final AtomicInteger intervalMillis = new AtomicInteger(MAX_INTERVAL_MILLIS);
    private final AtomicInteger concurrency = new AtomicInteger(MIN_INTERVAL_MILLIS);

    private final AtomicInteger peakPenaltyMillis = new AtomicInteger(0);
    private final AtomicLong lastFailureAt = new AtomicLong(0);

    public AutoGenerateQueueOptions(
            ScheduledExecutorService scheduler
    ) {
        this(scheduler, System::currentTimeMillis);
    }

    AutoGenerateQueueOptions(ScheduledExecutorService scheduler, LongSupplier clock) {
        this.scheduler = scheduler;
        this.clock = clock;
    }

    public AutoGenerateQueueOptions() {
        this(Executors.newSingleThreadScheduledExecutor());
    }

    public void setClient(MineSkinClient client) {
        this.client = client;
        // Initial load
        reloadGrants().exceptionally(throwable -> {
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Failed to load grants", throwable);
            return null;
        });
    }

    @Override
    public ScheduledExecutorService scheduler() {
        return scheduler;
    }

    @Override
    public int intervalMillis() {
        reloadIfNeeded();
        int effective = intervalMillis.get() + currentPenalty();
        return Math.min(effective, MAX_EFFECTIVE_INTERVAL_MILLIS);
    }

    @Override
    public int concurrency() {
        reloadIfNeeded();
        return concurrency.get();
    }

    @Override
    public void reportFailure() {
        int penalty;
        int baseline;
        synchronized (this) {
            int existing = currentPenalty();
            penalty = Math.min(MAX_PENALTY_MILLIS, existing + FAILURE_STEP_MILLIS);
            peakPenaltyMillis.set(penalty);
            lastFailureAt.set(clock.getAsLong());
            baseline = intervalMillis.get();
        }
        int effective = Math.min(baseline + penalty, MAX_EFFECTIVE_INTERVAL_MILLIS);
        MineSkinClientImpl.LOGGER.log(Level.INFO,
                "[QueueOptions] Slowing down after failure: penalty {0}ms, effective {1}ms (baseline {2}ms)",
                new Object[]{penalty, effective, baseline});
    }

    private int currentPenalty() {
        int peak = peakPenaltyMillis.get();
        if (peak == 0) return 0;
        long elapsed = clock.getAsLong() - lastFailureAt.get();
        if (elapsed <= 0) return peak;
        long decayed = peak - (elapsed * DECAY_MILLIS_PER_SECOND / 1000);
        return (int) Math.max(0, decayed);
    }

    private void reloadIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastRefresh.get() > TimeUnit.MINUTES.toMillis(5)) { // 5 minutes
            reloadGrants().exceptionally(throwable -> {
                MineSkinClientImpl.LOGGER.log(Level.WARNING, "Failed to reload grants", throwable);
                return null;
            });
        }
    }

    public CompletableFuture<Void> reloadGrants() {
        if (client == null) {
            return CompletableFuture.completedFuture(null);
        }
        lastRefresh.set(System.currentTimeMillis());

        return client.misc().getUser()
                .thenApply(UserResponse::getUser)
                .thenApply(User::grants)
                .thenAccept(grants -> {
                    grants.concurrency().ifPresent(rawConcurrent -> {
                        int concurrent = Math.min(Math.max(rawConcurrent, MIN_CONCURRENCY), MAX_CONCURRENCY);
                        int previous = concurrency.getAndSet(concurrent);
                        if (previous != rawConcurrent) {
                            client.getLogger().log(Level.FINE, "[QueueOptions] Updated concurrency from {0} to {1}", new Object[]{previous, concurrent});
                        }
                    });
                    grants.perMinute().ifPresent(rawPerMinute -> {
                        int interval = Math.min(Math.max(60_000 / rawPerMinute, MIN_INTERVAL_MILLIS), MAX_INTERVAL_MILLIS);
                        int previous = intervalMillis.getAndSet(interval);
                        if (previous != interval) {
                            client.getLogger().log(Level.FINE, "[QueueOptions] Updated interval from {0}ms to {1}ms (perMinute={2})", new Object[]{previous, interval, rawPerMinute});
                        }
                    });
                });
    }

}
