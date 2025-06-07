package org.mineskin;

import java.util.concurrent.Executor;

public record RequestExecutors(
    Executor getExecutor,
    Executor generateExecutor,
    QueueOptions generateQueueOptions,
    QueueOptions getQueueOptions,
    JobCheckOptions jobCheckOptions
) {
}
