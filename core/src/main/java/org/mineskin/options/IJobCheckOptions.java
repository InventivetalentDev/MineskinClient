package org.mineskin.options;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Base implementation: {@link org.mineskin.JobCheckOptions}
 */
public interface IJobCheckOptions {
    ScheduledExecutorService scheduler();

    int intervalMillis();

    int initialDelayMillis();

    int maxAttempts();
}
