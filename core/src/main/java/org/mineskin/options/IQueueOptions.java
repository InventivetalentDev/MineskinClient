package org.mineskin.options;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Base implementation: {@link org.mineskin.QueueOptions}
 */
public interface IQueueOptions {
    ScheduledExecutorService scheduler();

    int intervalMillis();

    int concurrency();

    /**
     * Report that a job handled by this queue failed. Adaptive implementations
     * (see {@link AutoGenerateQueueOptions}) use this signal to temporarily slow
     * down the request rate. No-op by default.
     */
    default void reportFailure() {
    }

}
