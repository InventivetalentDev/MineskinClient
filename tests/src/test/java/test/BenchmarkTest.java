package test;

import org.junit.Ignore;
import org.junit.Test;
import org.mineskin.*;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.options.AutoGenerateQueueOptions;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.QueueResponse;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import static org.junit.Assert.assertEquals;

@Ignore
public class BenchmarkTest {

    static {
        // set logger to log milliseconds
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT.%1$tL %4$s %2$s: %5$s%6$s%n");

        MineSkinClientImpl.LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        MineSkinClientImpl.LOGGER.addHandler(handler);
    }

    private static int GENERATE_INTERVAL_MS = 100;
    private static int GENERATE_CONCURRENCY = 20;

    private static int GENERATE_AMOUNT = 50;

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private static final MineSkinClient JAVA11 = MineSkinClient.builder()
            .requestHandler(Java11RequestHandler::new)
            .userAgent("MineSkinClient/Benchmark")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .generateExecutor(EXECUTOR)
//            .generateQueueOptions(new QueueOptions(
//                    Executors.newSingleThreadScheduledExecutor(),
//                    GENERATE_INTERVAL_MS, GENERATE_CONCURRENCY
//            ))
            .generateQueueOptions(new AutoGenerateQueueOptions(Executors.newSingleThreadScheduledExecutor()))
            .build();

    private final AtomicInteger per10s = new AtomicInteger();
    private final AtomicInteger perMinute = new AtomicInteger();
    private final AtomicInteger total = new AtomicInteger();
    private long lastLog10s = System.currentTimeMillis();
    private long lastLog = System.currentTimeMillis();

    @Test
    public void benchmark() throws InterruptedException {

        log("Starting benchmark with " + GENERATE_AMOUNT + " skins, interval " + GENERATE_INTERVAL_MS + "ms, concurrency " + GENERATE_CONCURRENCY);

        MineSkinClient client = JAVA11;
        int count = GENERATE_AMOUNT;
        Thread.sleep(1000);

        CompletableFuture.runAsync(() -> {
            while (total.get() < count) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                long now = System.currentTimeMillis();
                if (now - lastLog10s >= 10000) {
                    int per10s = this.per10s.get();
                    log("Last 10s: " + per10s + " (" + (per10s / 10.0) + "/s)");
                    this.per10s.set(0);
                    lastLog10s = now;
                }
                if (now - lastLog >= 60000) {
                    int perMinute = this.perMinute.get();
                    log("Last minute: " + perMinute + " (" + (perMinute / 60.0) + "/s)");
                    this.perMinute.set(0);
                    lastLog = now;
                }
            }
        });

        long start = System.currentTimeMillis();
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int finalI = i;
            futures.add(CompletableFuture.runAsync(() -> {
                try {
                    generateSkin(client, finalI);
                } catch (InterruptedException e) {
                    System.out.println(e);
                }
            }));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log("Generated " + count + " in " + (System.currentTimeMillis() - start) + "ms");

        assertEquals(count, total.get());

        Thread.sleep(1000);
    }

    private void generateSkin(MineSkinClient client, int index) throws InterruptedException {
        long jobStart = System.currentTimeMillis();
        try {
            Thread.sleep(10);
            String name = "mskjva-bnch-" + index + "-" + ThreadLocalRandom.current().nextInt(1000);
            BufferedImage image = ImageUtil.randomImage(64, ThreadLocalRandom.current().nextBoolean() ? 64 : 32);
            GenerateRequest request = GenerateRequest.upload(image)
                    .visibility(Visibility.UNLISTED)
                    .name(name);
            QueueResponse res = client.queue().submit(request).join();
            log("[" + index + "] " + res.getBreadcrumb() + " Queue submit took " + (System.currentTimeMillis() - jobStart) + "ms - " + res.getRateLimit().next());
            log(res);

            client.queue()
                    .waitForCompletion(res.getJob())
                    .thenCompose(jobReference -> {
                        log("[" + index + "] " + res.getBreadcrumb() + " Job took " + (System.currentTimeMillis() - jobStart) + "ms");
                        return jobReference.getOrLoadSkin(client);
                    })
                    .thenAccept(skinInfo -> {
                        log("[" + index + "] " + res.getBreadcrumb() + " Got skin after " + (System.currentTimeMillis() - jobStart) + "ms");
                        log(skinInfo);
                        per10s.incrementAndGet();
                        perMinute.incrementAndGet();
                        total.incrementAndGet();
                    })
                    .exceptionally(throwable -> {
                        if (throwable instanceof CompletionException e && e.getCause() instanceof MineSkinRequestException req) {
                            log(req.getResponse());
                        } else {
                            log(throwable);
                        }
                        return null;
                    })
                    .join();
        } catch (CompletionException | InterruptedException e) {
            if (e.getCause() instanceof MineSkinRequestException req) {
                log(req.getResponse());
            }
            throw e;
        }
    }

    static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    void log(Object message) {
        Date date = new Date();
        System.out.println(String.format("[%s] %s", DATE_FORMAT.format(date), message));
    }

}
