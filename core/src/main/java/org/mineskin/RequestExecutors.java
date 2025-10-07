package org.mineskin;

import org.mineskin.options.IJobCheckOptions;
import org.mineskin.options.IQueueOptions;

import java.util.concurrent.Executor;

public record RequestExecutors(
    Executor getExecutor,
    Executor generateExecutor,
    IQueueOptions generateQueueOptions,
    IQueueOptions getQueueOptions,
    IJobCheckOptions jobCheckOptions
) {
}
