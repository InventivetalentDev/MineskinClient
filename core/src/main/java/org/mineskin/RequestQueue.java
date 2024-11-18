package org.mineskin;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RequestQueue {

    private final Queue<Supplier<CompletableFuture<?>>> queue = new LinkedList<>();

    public RequestQueue(ScheduledExecutorService executor, int interval) {
        executor.scheduleAtFixedRate(() -> {
            Supplier<CompletableFuture<?>> supplier;
            if ((supplier = queue.poll()) != null) {
                supplier.get();
            }
        }, 0, interval, TimeUnit.MILLISECONDS);
    }

    public <T> CompletableFuture<T> submit(Supplier<CompletableFuture<T>> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        queue.add(() -> supplier.get().whenComplete((result, throwable) -> {
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
