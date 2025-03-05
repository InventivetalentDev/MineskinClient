package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.data.JobInfo;
import org.mineskin.data.JobReference;
import org.mineskin.data.NullJobReference;
import org.mineskin.data.RateLimitInfo;
import org.mineskin.data.SkinInfo;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.GenerateRequest;
import org.mineskin.request.RequestHandler;
import org.mineskin.request.UploadRequestBuilder;
import org.mineskin.request.UrlRequestBuilder;
import org.mineskin.request.UserRequestBuilder;
import org.mineskin.request.source.UploadSource;
import org.mineskin.response.GenerateResponse;
import org.mineskin.response.GenerateResponseImpl;
import org.mineskin.response.JobResponse;
import org.mineskin.response.JobResponseImpl;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.QueueResponse;
import org.mineskin.response.QueueResponseImpl;
import org.mineskin.response.SkinResponse;
import org.mineskin.response.SkinResponseImpl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineSkinClientImpl implements MineSkinClient {

    public static final Logger LOGGER = Logger.getLogger(MineSkinClient.class.getName());

    private static final String API_BASE = "https://api.mineskin.org";

    private final RequestExecutors executors;

    private final RequestHandler requestHandler;
    private final RequestQueue generateQueue;
    private final RequestQueue getQueue;

    private final QueueClient queueClient = new QueueClientImpl();
    private final GenerateClient generateClient = new GenerateClientImpl();
    private final SkinsClient skinsClient = new SkinsClientImpl();

    public MineSkinClientImpl(RequestHandler requestHandler, RequestExecutors executors) {
        this.requestHandler = checkNotNull(requestHandler);
        this.executors = checkNotNull(executors);

        this.generateQueue = new RequestQueue(executors.generateRequestScheduler(), 200, 1);
        this.getQueue = new RequestQueue(executors.jobCheckScheduler(), 100, 5);
    }

    /////


    @Override
    public QueueClient queue() {
        return queueClient;
    }

    @Override
    public GenerateClient generate() {
        return generateClient;
    }

    @Override
    public SkinsClient skins() {
        return skinsClient;
    }

    class QueueClientImpl implements QueueClient {

        @Override
        public CompletableFuture<QueueResponse> submit(GenerateRequest request) {
            if (request instanceof UploadRequestBuilder uploadRequestBuilder) {
                return queueUpload(uploadRequestBuilder);
            } else if (request instanceof UrlRequestBuilder urlRequestBuilder) {
                return queueUrl(urlRequestBuilder);
            } else if (request instanceof UserRequestBuilder userRequestBuilder) {
                return queueUser(userRequestBuilder);
            }
            throw new MineskinException("Unknown request builder type: " + request.getClass());
        }

        CompletableFuture<QueueResponse> queueUpload(UploadRequestBuilder builder) {
            return generateQueue.submit(() -> {
                try {
                    Map<String, String> data = builder.options().toMap();
                    UploadSource source = builder.getUploadSource();
                    checkNotNull(source);
                    try (InputStream inputStream = source.getInputStream()) {
                        QueueResponseImpl res = requestHandler.postFormDataFile(API_BASE + "/v2/queue", "file", "mineskinjava", inputStream, data, JobInfo.class, QueueResponseImpl::new);
                        handleGenerateResponse(res);
                        return res;
                    }
                } catch (IOException e) {
                    throw new MineskinException(e);
                } catch (MineSkinRequestException e) {
                    handleGenerateResponse(e.getResponse());
                    throw e;
                }
            }, executors.generateExecutor());
        }

        CompletableFuture<QueueResponse> queueUrl(UrlRequestBuilder builder) {
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    URL url = builder.getUrl();
                    checkNotNull(url);
                    body.addProperty("url", url.toString());
                    QueueResponseImpl res = requestHandler.postJson(API_BASE + "/v2/queue", body, JobInfo.class, QueueResponseImpl::new);
                    handleGenerateResponse(res);
                    return res;
                } catch (IOException e) {
                    throw new MineskinException(e);
                } catch (MineSkinRequestException e) {
                    handleGenerateResponse(e.getResponse());
                    throw e;
                }
            }, executors.generateExecutor());
        }

        CompletableFuture<QueueResponse> queueUser(UserRequestBuilder builder) {
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    UUID uuid = builder.getUuid();
                    checkNotNull(uuid);
                    body.addProperty("user", uuid.toString());
                    QueueResponseImpl res = requestHandler.postJson(API_BASE + "/v2/queue", body, JobInfo.class, QueueResponseImpl::new);
                    handleGenerateResponse(res);
                    return res;
                } catch (IOException e) {
                    throw new MineskinException(e);
                } catch (MineSkinRequestException e) {
                    handleGenerateResponse(e.getResponse());
                    throw e;
                }
            }, executors.generateExecutor());
        }

        private void handleGenerateResponse(MineSkinResponse<?> response0) {
            if (!(response0 instanceof QueueResponse response)) return;
            RateLimitInfo rateLimit = response.getRateLimit();
            if (rateLimit == null) return;
            long nextRelative = rateLimit.next().relative();
            if (nextRelative > 0) {
                generateQueue.setNextRequest(Math.max(generateQueue.getNextRequest(), System.currentTimeMillis() + nextRelative));
            }
        }

        @Override
        public CompletableFuture<JobResponse> get(JobInfo jobInfo) {
            checkNotNull(jobInfo);
            return get(jobInfo.id());
        }

        @Override
        public CompletableFuture<JobResponse> get(String id) {
            checkNotNull(id);
            return CompletableFuture.supplyAsync(() -> {
                try {
                    return requestHandler.getJson(API_BASE + "/v2/queue/" + id, JobInfo.class, JobResponseImpl::new);
                } catch (IOException e) {
                    throw new MineskinException(e);
                }
            }, executors.getExecutor());
        }

        @Override
        public CompletableFuture<JobReference> waitForCompletion(JobInfo jobInfo) {
            checkNotNull(jobInfo);
            if (jobInfo.id() == null) {
                return CompletableFuture.completedFuture(new NullJobReference(jobInfo));
            }
            return new JobChecker(MineSkinClientImpl.this, jobInfo, executors.jobCheckScheduler(), 10, 2, 1).check();
        }


    }

    class GenerateClientImpl implements GenerateClient {

        @Override
        public CompletableFuture<GenerateResponse> submitAndWait(GenerateRequest request) {
            if (request instanceof UploadRequestBuilder uploadRequestBuilder) {
                return generateUpload(uploadRequestBuilder);
            } else if (request instanceof UrlRequestBuilder urlRequestBuilder) {
                return generateUrl(urlRequestBuilder);
            } else if (request instanceof UserRequestBuilder userRequestBuilder) {
                return generateUser(userRequestBuilder);
            }
            throw new MineskinException("Unknown request builder type: " + request.getClass());
        }

        CompletableFuture<GenerateResponse> generateUpload(UploadRequestBuilder builder) {
            return generateQueue.submit(() -> {
                try {
                    Map<String, String> data = builder.options().toMap();
                    UploadSource source = builder.getUploadSource();
                    checkNotNull(source);
                    try (InputStream inputStream = source.getInputStream()) {
                        GenerateResponseImpl res = requestHandler.postFormDataFile(API_BASE + "/v2/generate", "file", "mineskinjava", inputStream, data, SkinInfo.class, GenerateResponse::new);
                        handleGenerateResponse(res);
                        return res;
                    }
                } catch (IOException e) {
                    throw new MineskinException(e);
                } catch (MineSkinRequestException e) {
                    handleGenerateResponse(e.getResponse());
                    throw e;
                }
            }, executors.generateExecutor());
        }

        CompletableFuture<GenerateResponse> generateUrl(UrlRequestBuilder builder) {
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    URL url = builder.getUrl();
                    checkNotNull(url);
                    body.addProperty("url", url.toString());
                    GenerateResponseImpl res = requestHandler.postJson(API_BASE + "/v2/generate", body, SkinInfo.class, GenerateResponseImpl::new);
                    handleGenerateResponse(res);
                    return res;
                } catch (IOException e) {
                    throw new MineskinException(e);
                } catch (MineSkinRequestException e) {
                    handleGenerateResponse(e.getResponse());
                    throw e;
                }
            }, executors.generateExecutor());
        }

        CompletableFuture<GenerateResponse> generateUser(UserRequestBuilder builder) {
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    UUID uuid = builder.getUuid();
                    checkNotNull(uuid);
                    body.addProperty("user", uuid.toString());
                    GenerateResponseImpl res = requestHandler.postJson(API_BASE + "/v2/generate", body, SkinInfo.class, GenerateResponseImpl::new);
                    handleGenerateResponse(res);
                    return res;
                } catch (IOException e) {
                    throw new MineskinException(e);
                } catch (MineSkinRequestException e) {
                    handleGenerateResponse(e.getResponse());
                    throw e;
                }
            }, executors.generateExecutor());
        }

        private void handleGenerateResponse(MineSkinResponse<?> response0) {
            if (!(response0 instanceof GenerateResponse response)) return;
            RateLimitInfo rateLimit = response.getRateLimit();
            if (rateLimit == null) return;
            long nextRelative = rateLimit.next().relative();
            if (nextRelative > 0) {
                generateQueue.setNextRequest(Math.max(generateQueue.getNextRequest(), System.currentTimeMillis() + nextRelative));
            }
        }

    }

    class SkinsClientImpl implements SkinsClient {

        /**
         * Get an existing skin by UUID (Note: not the player's UUID)
         */
        @Override
        public CompletableFuture<SkinResponse> get(UUID uuid) {
            checkNotNull(uuid);
            return get(uuid.toString());
        }

        /**
         * Get an existing skin by UUID (Note: not the player's UUID)
         */
        @Override
        public CompletableFuture<SkinResponse> get(String uuid) {
            checkNotNull(uuid);
            return getQueue.submit(() -> {
                try {
                    return requestHandler.getJson(API_BASE + "/v2/skins/" + uuid, SkinInfo.class, SkinResponseImpl::new);
                } catch (IOException e) {
                    throw new MineskinException(e);
                }
            }, executors.getExecutor());
        }

    }

}
