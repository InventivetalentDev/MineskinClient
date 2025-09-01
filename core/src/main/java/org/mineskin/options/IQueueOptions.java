package org.mineskin.options;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Base implementation: {@link org.mineskin.QueueOptions}
 */
public interface IQueueOptions {
    ScheduledExecutorService scheduler();

    int intervalMillis();

    int concurrency();
}
