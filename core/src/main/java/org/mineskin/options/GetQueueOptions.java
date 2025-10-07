package org.mineskin.options;

import org.mineskin.QueueOptions;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public abstract class GetQueueOptions {

    /**
     * Creates a QueueOptions instance with default values for get requests (100ms interval, 5 concurrent requests).
     */
    public static QueueOptions create(ScheduledExecutorService scheduler) {
        return new QueueOptions(scheduler, 100, 5);
    }

    /**
     * Creates a QueueOptions instance with default values for get requests (100ms interval, 5 concurrent requests).
     */
    public static QueueOptions create() {
        return create(Executors.newSingleThreadScheduledExecutor());
    }

}
