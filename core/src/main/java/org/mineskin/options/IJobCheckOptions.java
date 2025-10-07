package org.mineskin.options;

import org.mineskin.request.backoff.RequestInterval;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Base implementation: {@link org.mineskin.JobCheckOptions}
 */
public interface IJobCheckOptions {
    ScheduledExecutorService scheduler();

    RequestInterval interval();

    @Deprecated
    int intervalMillis();

    int initialDelayMillis();

    int maxAttempts();

    boolean useEta();
}
