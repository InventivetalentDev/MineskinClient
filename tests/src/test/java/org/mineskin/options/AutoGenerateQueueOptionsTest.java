package org.mineskin.options;

import org.junit.jupiter.api.Test;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AutoGenerateQueueOptionsTest {

    @Test
    public void intervalReturnsBaselineWhenNoFailures() {
        AutoGenerateQueueOptions options = new AutoGenerateQueueOptions(
                Executors.newSingleThreadScheduledExecutor(), () -> 0L);
        // Baseline defaults to MAX_INTERVAL_MILLIS (1000) until grants are loaded.
        assertEquals(1000, options.intervalMillis());
    }

    @Test
    public void reportFailureAddsPenaltyImmediately() {
        AtomicLong now = new AtomicLong(10_000);
        AutoGenerateQueueOptions options = new AutoGenerateQueueOptions(
                Executors.newSingleThreadScheduledExecutor(), now::get);

        int before = options.intervalMillis();
        options.reportFailure();
        int after = options.intervalMillis();

        assertEquals(before + 500, after, "single failure adds FAILURE_STEP_MILLIS");
    }

    @Test
    public void penaltyDecaysLinearlyBackToBaseline() {
        AtomicLong now = new AtomicLong(10_000);
        AutoGenerateQueueOptions options = new AutoGenerateQueueOptions(
                Executors.newSingleThreadScheduledExecutor(), now::get);
        int baseline = options.intervalMillis();

        options.reportFailure();
        assertEquals(baseline + 500, options.intervalMillis());

        // Decay rate is 100ms/s, so a 500ms penalty clears in 5 seconds. Halfway through: 250ms left.
        now.addAndGet(2500);
        assertEquals(baseline + 250, options.intervalMillis());

        // Fully decayed after another 2.5s (+ a little).
        now.addAndGet(2600);
        assertEquals(baseline, options.intervalMillis());
    }

    @Test
    public void repeatedFailuresStackOnDecayingPenalty() {
        AtomicLong now = new AtomicLong(10_000);
        AutoGenerateQueueOptions options = new AutoGenerateQueueOptions(
                Executors.newSingleThreadScheduledExecutor(), now::get);
        int baseline = options.intervalMillis();

        options.reportFailure();
        assertEquals(baseline + 500, options.intervalMillis());

        // After 2s, penalty has decayed from 500 to 300.
        now.addAndGet(2000);
        assertEquals(baseline + 300, options.intervalMillis());

        // New failure stacks on the current (decayed) penalty: 300 + 500 = 800.
        options.reportFailure();
        assertEquals(baseline + 800, options.intervalMillis());
    }

    @Test
    public void penaltyClampsAtMax() {
        AtomicLong now = new AtomicLong(10_000);
        AutoGenerateQueueOptions options = new AutoGenerateQueueOptions(
                Executors.newSingleThreadScheduledExecutor(), now::get);

        // 25 failures back-to-back would naively reach 12_500ms — should cap at 10_000.
        for (int i = 0; i < 25; i++) {
            options.reportFailure();
        }

        int effective = options.intervalMillis();
        // Baseline (1000) + capped penalty (10_000) would be 11_000, but effective clamps at 10_000.
        assertTrue(effective <= 10_000, "effective interval clamped at MAX_EFFECTIVE_INTERVAL_MILLIS, got " + effective);
        assertTrue(effective >= 10_000 - 100, "should be near the ceiling, got " + effective);
    }

    @Test
    public void realClockConstructorStillWorks() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            AutoGenerateQueueOptions options = new AutoGenerateQueueOptions(scheduler);
            int baseline = options.intervalMillis();
            options.reportFailure();
            assertEquals(baseline + 500, options.intervalMillis());
        } finally {
            scheduler.shutdownNow();
        }
    }
}
