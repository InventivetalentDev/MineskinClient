package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.data.*;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.*;
import org.mineskin.request.source.UploadSource;
import org.mineskin.response.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineSkinClientImpl implements MineSkinClient {

    public static final Logger LOGGER = Logger.getLogger(MineSkinClient.class.getName());

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

        this.generateQueue = new RequestQueue(executors.generateQueueOptions());
        this.getQueue = new RequestQueue(executors.getQueueOptions());
    }

    /// //


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
            LOGGER.log(Level.FINER, "Adding upload request to internal queue: {0}", builder);
            return generateQueue.submit(() -> {
                try {
                    Map<String, String> data = builder.options().toMap();
                    UploadSource source = builder.getUploadSource();
                    checkNotNull(source);
                    try (InputStream inputStream = source.getInputStream()) {
                        LOGGER.log(Level.FINER, "Submitting to MineSkin queue: {0}", builder);
                        QueueResponseImpl res = requestHandler.postFormDataFile("/v2/queue", "file", "mineskinjava", inputStream, data, JobInfo.class, QueueResponseImpl::new);
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
            LOGGER.log(Level.FINER, "Adding url request to internal queue: {0}", builder);
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    URL url = builder.getUrl();
                    checkNotNull(url);
                    body.addProperty("url", url.toString());
                    LOGGER.log(Level.FINER, "Submitting to MineSkin queue: {0}", builder);
                    QueueResponseImpl res = requestHandler.postJson("/v2/queue", body, JobInfo.class, QueueResponseImpl::new);
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
            LOGGER.log(Level.FINER, "Adding user request to internal queue: {0}", builder);
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    UUID uuid = builder.getUuid();
                    checkNotNull(uuid);
                    body.addProperty("user", uuid.toString());
                    LOGGER.log(Level.FINER, "Submitting to MineSkin queue: {0}", builder);
                    QueueResponseImpl res = requestHandler.postJson("/v2/queue", body, JobInfo.class, QueueResponseImpl::new);
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
                    return requestHandler.getJson("/v2/queue/" + id, JobInfo.class, JobResponseImpl::new);
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
            JobCheckOptions options = executors.jobCheckOptions();
            return new JobChecker(MineSkinClientImpl.this, jobInfo, options).check();
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
            LOGGER.log(Level.FINER, "Adding upload request to internal generate queue: {0}", builder);
            return generateQueue.submit(() -> {
                try {
                    Map<String, String> data = builder.options().toMap();
                    UploadSource source = builder.getUploadSource();
                    checkNotNull(source);
                    try (InputStream inputStream = source.getInputStream()) {
                        LOGGER.log(Level.FINER, "Submitting to MineSkin generate: {0}", builder);
                        GenerateResponseImpl res = requestHandler.postFormDataFile("/v2/generate", "file", "mineskinjava", inputStream, data, SkinInfo.class, GenerateResponseImpl::new);
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
            LOGGER.log(Level.FINER, "Adding url request to internal generate queue: {0}", builder);
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    URL url = builder.getUrl();
                    checkNotNull(url);
                    body.addProperty("url", url.toString());
                    LOGGER.log(Level.FINER, "Submitting to MineSkin generate: {0}", builder);
                    GenerateResponseImpl res = requestHandler.postJson("/v2/generate", body, SkinInfo.class, GenerateResponseImpl::new);
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
            LOGGER.log(Level.FINER, "Adding user request to internal generate queue: {0}", builder);
            return generateQueue.submit(() -> {
                try {
                    JsonObject body = builder.options().toJson();
                    UUID uuid = builder.getUuid();
                    checkNotNull(uuid);
                    body.addProperty("user", uuid.toString());
                    LOGGER.log(Level.FINER, "Submitting to MineSkin generate: {0}", builder);
                    GenerateResponseImpl res = requestHandler.postJson("/v2/generate", body, SkinInfo.class, GenerateResponseImpl::new);
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
            LOGGER.log(Level.FINER, "Handling generate response: {0}", response0);
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
                    return requestHandler.getJson("/v2/skins/" + uuid, SkinInfo.class, SkinResponseImpl::new);
                } catch (IOException e) {
                    throw new MineskinException(e);
                }
            }, executors.getExecutor());
        }

    }

}
