package test;

import org.junit.jupiter.api.Test;
import org.mineskin.JobBatchChecker;
import org.mineskin.JobCheckOptions;
import org.mineskin.MineSkinClient;
import org.mineskin.GenerateClient;
import org.mineskin.MiscClient;
import org.mineskin.QueueClient;
import org.mineskin.SkinsClient;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.data.JobInfo;
import org.mineskin.data.JobReference;
import org.mineskin.data.JobStatus;
import org.mineskin.data.SkinInfo;
import org.mineskin.request.GenerateRequest;
import org.mineskin.request.backoff.RequestInterval;
import org.mineskin.response.JobListResponse;
import org.mineskin.response.JobResponse;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.QueueResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

public class JobBatchCheckerTest {

    private static final Logger LOGGER = Logger.getLogger("JobBatchCheckerTest");

    @Test
    public void batchesViaListWhenManyPending() throws Exception {
        FakeQueueClient queue = new FakeQueueClient();
        MineSkinClient client = new FakeMineSkinClient(queue);
        for (int i = 0; i < 5; i++) {
            queue.setStatus("job" + i, JobStatus.WAITING, null);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            JobCheckOptions options = JobCheckOptions.create(scheduler)
                    .withInitialDelay(30)
                    .withInterval(RequestInterval.constant(40))
                    .withMaxAttempts(50);
            JobBatchChecker checker = new JobBatchChecker(client, options);

            List<CompletableFuture<JobReference>> futures = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                JobInfo job = new JobInfo("job" + i, JobStatus.WAITING, System.currentTimeMillis(), null);
                futures.add(checker.register(job));
            }

            // Let at least one tick run against all-pending state.
            Thread.sleep(150);
            int listCallsBeforeCompletion = queue.listCalls.get();
            assertTrue(listCallsBeforeCompletion >= 1,
                    "list() should be used when >4 jobs pending, calls=" + listCallsBeforeCompletion);
            assertTrue(queue.getCallsForId.isEmpty(),
                    "get() should not be called while jobs are still pending, calls=" + queue.getCallsForId);

            // Mark jobs done: 3 completed, 1 failed, 1 still waiting.
            queue.setStatus("job0", JobStatus.COMPLETED, "uuid-0");
            queue.setStatus("job1", JobStatus.COMPLETED, "uuid-1");
            queue.setStatus("job2", JobStatus.COMPLETED, "uuid-2");
            queue.setStatus("job3", JobStatus.FAILED, null);

            // Wait for the four finished futures to resolve.
            for (int i = 0; i < 4; i++) {
                JobReference ref = awaitFuture(futures.get(i), 2000);
                assertEquals("job" + i, ref.getJob().id());
            }
            assertFalse(futures.get(4).isDone(), "still-waiting job should not have completed");

            // Each COMPLETED job should trigger exactly one per-job get() (for skin). FAILED should not.
            assertEquals(1, queue.getCallsForId.getOrDefault("job0", 0).intValue());
            assertEquals(1, queue.getCallsForId.getOrDefault("job1", 0).intValue());
            assertEquals(1, queue.getCallsForId.getOrDefault("job2", 0).intValue());
            assertEquals(0, queue.getCallsForId.getOrDefault("job3", 0).intValue(),
                    "FAILED jobs should be resolved from list response without a per-job get()");
        } finally {
            scheduler.shutdownNow();
        }
    }

    @Test
    public void usesIndividualGetsWhenFewPending() throws Exception {
        FakeQueueClient queue = new FakeQueueClient();
        MineSkinClient client = new FakeMineSkinClient(queue);
        for (int i = 0; i < 3; i++) {
            queue.setStatus("job" + i, JobStatus.WAITING, null);
        }

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        try {
            JobCheckOptions options = JobCheckOptions.create(scheduler)
                    .withInitialDelay(30)
                    .withInterval(RequestInterval.constant(40))
                    .withMaxAttempts(50);
            JobBatchChecker checker = new JobBatchChecker(client, options);

            List<CompletableFuture<JobReference>> futures = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                JobInfo job = new JobInfo("job" + i, JobStatus.WAITING, System.currentTimeMillis(), null);
                futures.add(checker.register(job));
            }

            Thread.sleep(150);
            assertEquals(0, queue.listCalls.get(),
                    "list() should not be used when pending <= threshold");
            assertTrue(queue.getCallsForId.size() > 0,
                    "get() should have been called for each pending job");

            queue.setStatus("job0", JobStatus.COMPLETED, "uuid-0");
            queue.setStatus("job1", JobStatus.COMPLETED, "uuid-1");
            queue.setStatus("job2", JobStatus.COMPLETED, "uuid-2");

            for (int i = 0; i < 3; i++) {
                JobReference ref = awaitFuture(futures.get(i), 2000);
                assertEquals("job" + i, ref.getJob().id());
            }
            assertEquals(0, queue.listCalls.get(), "list() must not be used in small-batch mode");
        } finally {
            scheduler.shutdownNow();
        }
    }

    private static JobReference awaitFuture(CompletableFuture<JobReference> future, long timeoutMs)
            throws InterruptedException, TimeoutException, ExecutionException {
        try {
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            fail("Timed out after " + timeoutMs + "ms waiting for job");
            throw e;
        }
    }

    // ---------- fakes ----------

    private static final class FakeMineSkinClient implements MineSkinClient {
        private final QueueClient queue;

        FakeMineSkinClient(QueueClient queue) {
            this.queue = queue;
        }

        @Override public QueueClient queue() { return queue; }
        @Override public GenerateClient generate() { return null; }
        @Override public SkinsClient skins() { return null; }
        @Override public MiscClient misc() { return null; }
        @Override public Logger getLogger() { return LOGGER; }
    }

    private static final class FakeQueueClient implements QueueClient {
        final AtomicInteger listCalls = new AtomicInteger();
        final Map<String, Integer> getCallsForId = new ConcurrentHashMap<>();
        private final Map<String, JobInfo> state = new ConcurrentHashMap<>();

        void setStatus(String id, JobStatus status, String result) {
            state.put(id, new JobInfo(id, status, System.currentTimeMillis(), result));
        }

        @Override
        public CompletableFuture<QueueResponse> submit(GenerateRequest request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CompletableFuture<JobResponse> get(JobInfo jobInfo) {
            return get(jobInfo.id());
        }

        @Override
        public CompletableFuture<JobResponse> get(String id) {
            getCallsForId.merge(id, 1, Integer::sum);
            JobInfo current = state.getOrDefault(id, new JobInfo(id, JobStatus.UNKNOWN, System.currentTimeMillis(), null));
            SkinInfo skin = null; // tests don't need the real skin object
            return CompletableFuture.completedFuture(new FakeJobResponse(current, skin));
        }

        @Override
        public CompletableFuture<JobListResponse> list() {
            listCalls.incrementAndGet();
            List<JobInfo> snapshot = new ArrayList<>(state.values());
            return CompletableFuture.completedFuture(new FakeJobListResponse(snapshot));
        }

        @Override
        public CompletableFuture<JobReference> waitForCompletion(JobInfo jobInfo) {
            throw new UnsupportedOperationException();
        }
    }

    private static abstract class FakeResponse<T> implements MineSkinResponse<T> {
        @Override public boolean isSuccess() { return true; }
        @Override public int getStatus() { return 200; }
        @Override public List<CodeAndMessage> getMessages() { return Collections.emptyList(); }
        @Override public Optional<CodeAndMessage> getFirstMessage() { return Optional.empty(); }
        @Override public List<CodeAndMessage> getErrors() { return Collections.emptyList(); }
        @Override public boolean hasErrors() { return false; }
        @Override public Optional<CodeAndMessage> getFirstError() { return Optional.empty(); }
        @Override public Optional<CodeAndMessage> getErrorOrMessage() { return Optional.empty(); }
        @Override public List<CodeAndMessage> getWarnings() { return Collections.emptyList(); }
        @Override public Optional<CodeAndMessage> getFirstWarning() { return Optional.empty(); }
        @Override public String getServer() { return "test"; }
        @Override public String getBreadcrumb() { return null; }
    }

    private static final class FakeJobResponse extends FakeResponse<JobInfo> implements JobResponse {
        private final JobInfo job;
        private final SkinInfo skin;

        FakeJobResponse(JobInfo job, SkinInfo skin) {
            this.job = job;
            this.skin = skin;
        }

        @Override public JobInfo getBody() { return job; }
        @Override public JobInfo getJob() { return job; }
        @Override public Optional<SkinInfo> getSkin() { return Optional.ofNullable(skin); }
        @Override public CompletableFuture<SkinInfo> getOrLoadSkin(MineSkinClient client) {
            return CompletableFuture.completedFuture(skin);
        }
    }

    private static final class FakeJobListResponse extends FakeResponse<List<JobInfo>> implements JobListResponse {
        private final List<JobInfo> jobs;

        FakeJobListResponse(List<JobInfo> jobs) {
            this.jobs = jobs;
        }

        @Override public List<JobInfo> getBody() { return jobs; }
        @Override public List<JobInfo> getJobs() { return jobs; }
    }

}