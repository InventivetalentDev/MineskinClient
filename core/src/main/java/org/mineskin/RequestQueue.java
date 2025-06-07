package org.mineskin;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class RequestQueue {

    private final Queue<Supplier<CompletableFuture<?>>> queue = new LinkedList<>();
    private final AtomicInteger running = new AtomicInteger(0);
    private long nextRequest = 0;

    public RequestQueue(QueueOptions options) {
        this(options.scheduler(), options.intervalMillis(), options.concurrency());
    }

    public RequestQueue(ScheduledExecutorService executor, int interval, int concurrency) {
        executor.scheduleAtFixedRate(() -> {
            if (System.currentTimeMillis() < nextRequest) {
                return;
            }
            if (running.get() >= concurrency) {
                return;
            }
            Supplier<CompletableFuture<?>> supplier;
            if ((supplier = queue.poll()) != null) {
                running.incrementAndGet();
                supplier.get();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    public void setNextRequest(long nextRequest) {
        this.nextRequest = nextRequest;
    }

    public long getNextRequest() {
        return nextRequest;
    }

    public int getRunning() {
        return running.get();
    }

    public <T> CompletableFuture<T> submit(Supplier<CompletableFuture<T>> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(() -> supplier.get().whenComplete((result, throwable) -> {
            running.decrementAndGet();
            if (throwable != null) {
                future.completeExceptionally(throwable);
            } else {
                future.complete(result);
            }
        }));
        return future;
    }

    public <T> CompletableFuture<T> submit(Supplier<T> supplier, Executor executor) {
        return submit(() -> CompletableFuture.supplyAsync(supplier, executor));
    }

}
