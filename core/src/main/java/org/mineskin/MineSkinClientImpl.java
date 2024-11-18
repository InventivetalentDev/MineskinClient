package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.data.DelayInfo;
import org.mineskin.data.JobInfo;
import org.mineskin.data.SkinInfo;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.RequestBuilder;
import org.mineskin.request.RequestHandler;
import org.mineskin.request.upload.UploadRequestBuilder;
import org.mineskin.request.url.UrlRequestBuilder;
import org.mineskin.request.user.UserRequestBuilder;
import org.mineskin.request.source.UploadSource;
import org.mineskin.response.JobResponse;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.QueueResponse;
import org.mineskin.response.SkinResponse;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineSkinClientImpl implements MineSkinClient {

    public static final Logger LOGGER = Logger.getLogger(MineSkinClient.class.getName());

    private static final String API_BASE = "https://toast.api.mineskin.org"; //FIXME
    private static final String GENERATE_BASE = API_BASE + "/generate";
    private static final String GET_BASE = API_BASE + "/get";

    private static final Map<String, AtomicLong> DELAYS = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> NEXT_REQUESTS = new ConcurrentHashMap<>();

    private final Executor generateExecutor;
    private final Executor getExecutor;
    private final ScheduledExecutorService jobCheckScheduler;

    private final RequestHandler requestHandler;

    public MineSkinClientImpl(RequestHandler requestHandler, Executor generateExecutor, Executor getExecutor, ScheduledExecutorService jobCheckScheduler) {
        this.requestHandler = checkNotNull(requestHandler);
        this.generateExecutor = checkNotNull(generateExecutor);
        this.getExecutor = checkNotNull(getExecutor);
        this.jobCheckScheduler = checkNotNull(jobCheckScheduler);
    }

    public long getNextRequest() {
        String key = String.valueOf(requestHandler.getApiKey());
        return NEXT_REQUESTS.computeIfAbsent(key, k -> new AtomicLong(0)).get();
    }

    /////

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    public CompletableFuture<SkinResponse> getSkin(UUID uuid) {
        checkNotNull(uuid);
        return getSkin(uuid.toString());
    }

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    public CompletableFuture<SkinResponse> getSkin(String uuid) {
        checkNotNull(uuid);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requestHandler.getJson(API_BASE + "/v2/skins/" + uuid, SkinInfo.class, SkinResponse::new);
            } catch (IOException e) {
                throw new MineskinException(e);
            }
        }, getExecutor);
    }

    @Override
    public CompletableFuture<QueueResponse> queue(RequestBuilder builder) {
        if (builder instanceof UploadRequestBuilder uploadRequestBuilder) {
            return queueUpload(uploadRequestBuilder);
        } else if (builder instanceof UrlRequestBuilder urlRequestBuilder) {
            return queueUrl(urlRequestBuilder);
        } else if (builder instanceof UserRequestBuilder userRequestBuilder) {
            return queueUser(userRequestBuilder);
        }
        throw new MineskinException("Unknown request builder type: " + builder.getClass());
    }

    CompletableFuture<QueueResponse> queueUpload(UploadRequestBuilder builder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Map<String, String> data = builder.getOptions().toMap();
                UploadSource source = builder.getUploadSource();
                checkNotNull(source);
                try (InputStream inputStream = source.getInputStream()) {
                    QueueResponse res = requestHandler.postFormDataFile(API_BASE + "/v2/queue", "file", "mineskinjava", inputStream, data, JobInfo.class, QueueResponse::new);
                    handleResponse(res);
                    return res;
                }
            } catch (IOException e) {
                throw new MineskinException(e);
            } catch (MineSkinRequestException e) {
                handleResponse(e.getResponse());
                throw e;
            }
        }, generateExecutor);
    }

    CompletableFuture<QueueResponse> queueUrl(UrlRequestBuilder builder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = builder.getOptions().toJson();
                URL url = builder.getUrl();
                checkNotNull(url);
                body.addProperty("url", url.toString());
                QueueResponse res = requestHandler.postJson(API_BASE + "/v2/queue", body, JobInfo.class, QueueResponse::new);
                handleResponse(res);
                return res;
            } catch (IOException e) {
                throw new MineskinException(e);
            } catch (MineSkinRequestException e) {
                handleResponse(e.getResponse());
                throw e;
            }
        }, generateExecutor);
    }

    CompletableFuture<QueueResponse> queueUser(UserRequestBuilder builder) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JsonObject body = builder.getOptions().toJson();
                UUID uuid = builder.getUuid();
                checkNotNull(uuid);
                body.addProperty("user", uuid.toString());
                QueueResponse res = requestHandler.postJson(API_BASE + "/v2/queue", body, JobInfo.class, QueueResponse::new);
                handleResponse(res);
                return res;
            } catch (IOException e) {
                throw new MineskinException(e);
            } catch (MineSkinRequestException e) {
                handleResponse(e.getResponse());
                throw e;
            }
        }, generateExecutor);
    }

    @Override
    public CompletableFuture<JobResponse> getJobStatus(JobInfo jobInfo) {
        return getJobStatus(jobInfo.id());
    }

    public CompletableFuture<JobResponse> getJobStatus(String id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requestHandler.getJson(API_BASE + "/v2/queue/" + id, JobInfo.class, JobResponse::new);
            } catch (IOException e) {
                throw new MineskinException(e);
            }
        }, getExecutor);
    }

    public CompletableFuture<JobInfo> waitForJob(JobInfo jobInfo) {
        return new JobChecker(this, jobInfo, jobCheckScheduler, 10, 2, 1).check();
    }

    private void handleResponse(MineSkinResponse<?> response) {
//        if (response instanceof GenerateResponse generateResponse) {
//            handleDelayInfo(generateResponse.getDelayInfo());
//        }
    }

    private void delayUntilNext() {
        if (System.currentTimeMillis() < getNextRequest()) {
            long delay = (getNextRequest() - System.currentTimeMillis());
            try {
                LOGGER.finer("Waiting for " + delay + "ms until next request");
                Thread.sleep(delay + 1);
            } catch (InterruptedException e) {
                throw new MineskinException("Interrupted while waiting for next request", e);
            }
        }
    }

    private void handleDelayInfo(DelayInfo delayInfo) {
        if (delayInfo == null) {
            return;
        }
        String key = String.valueOf(requestHandler.getApiKey());
        AtomicLong delay = DELAYS.compute(key, (k, v) -> {
            if (v == null) {
                v = new AtomicLong(0);
            }
            if (delayInfo.millis() > v.get()) {
                // use the highest delay
                v.set(delayInfo.millis());
            }
            return v;
        });
        LOGGER.finer("Delaying next request by " + delay.get() + "ms");
        NEXT_REQUESTS.compute(key, (k, v) -> {
            if (v == null) {
                v = new AtomicLong(System.currentTimeMillis());
            }
            long next = System.currentTimeMillis() + delay.get() + 1;
            if (next > v.get()) {
                v.set(next);
            }
            return v;
        });
    }

}
