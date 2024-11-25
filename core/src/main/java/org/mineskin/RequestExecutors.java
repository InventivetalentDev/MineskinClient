package org.mineskin;

import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

public record RequestExecutors(
    Executor getExecutor,
    Executor generateExecutor,
    ScheduledExecutorService generateRequestScheduler,
    ScheduledExecutorService getRequestScheduler,
    ScheduledExecutorService jobCheckScheduler
) {
}
