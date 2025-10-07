package org.mineskin;

import com.google.gson.Gson;
import org.mineskin.options.*;
import org.mineskin.request.RequestHandler;
import org.mineskin.request.RequestHandlerConstructor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

public class ClientBuilder {

    private static final int DEFAULT_GENERATE_QUEUE_INTERVAL = 200;
    private static final int DEFAULT_GENERATE_QUEUE_CONCURRENCY = 1;
    private static final int DEFAULT_GET_QUEUE_INTERVAL = 100;
    private static final int DEFAULT_GET_QUEUE_CONCURRENCY = 5;
    private static final int DEFAULT_JOB_CHECK_INTERVAL = 1000;
    private static final int DEFAULT_JOB_CHECK_INITIAL_DELAY = 2000;
    private static final int DEFAULT_JOB_CHECK_MAX_ATTEMPTS = 10;

    private String baseUrl = "https://api.mineskin.org";
    private String userAgent = "MineSkinClient";
    private String apiKey = null;
    private int timeout = 10000;
    private Gson gson = new Gson();
    private Executor getExecutor = null;
    private Executor generateExecutor = null;
    private IQueueOptions generateQueueOptions = null;
    private IQueueOptions getQueueOptions = null;
    private IJobCheckOptions jobCheckOptions = null;
    private RequestHandlerConstructor requestHandlerConstructor = null;

    private ClientBuilder() {
    }

    /**
     * Create a new ClientBuilder
     */
    public static ClientBuilder create() {
        return new ClientBuilder();
    }

    /**
     * Set the base URL for the API
     *
     * @param baseUrl the base URL, e.g. "<a href="https://api.mineskin.org">https://api.mineskin.org</a>"
     */
    public ClientBuilder baseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    /**
     * Set the User-Agent
     */
    public ClientBuilder userAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    /**
     * Set the API key
     */
    public ClientBuilder apiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    /**
     * Set the timeout
     */
    public ClientBuilder timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    /**
     * Set the Gson instance
     */
    public ClientBuilder gson(Gson gson) {
        this.gson = gson;
        return this;
    }

    /**
     * Set the Executor for get requests
     */
    public ClientBuilder getExecutor(Executor getExecutor) {
        this.getExecutor = getExecutor;
        return this;
    }

    /**
     * Set the Executor for generate requests
     */
    public ClientBuilder generateExecutor(Executor generateExecutor) {
        this.generateExecutor = generateExecutor;
        return this;
    }

    /**
     * Set the ScheduledExecutorService for submitting queue jobs
     *
     * @deprecated use {@link #generateQueueOptions(IQueueOptions)} instead
     */
    @Deprecated
    public ClientBuilder generateRequestScheduler(ScheduledExecutorService scheduledExecutor) {
        this.generateQueueOptions = new QueueOptions(scheduledExecutor, DEFAULT_GENERATE_QUEUE_INTERVAL, DEFAULT_GENERATE_QUEUE_CONCURRENCY);
        return this;
    }

    /**
     * Set the options for submitting queue jobs<br/>
     * defaults to 200ms interval and 1 concurrent request<br/>
     * For example:
     * <pre>
     * {@code
     * GenerateQueueOptions.create()
     *         .withInterval(200, TimeUnit.MILLISECONDS)
     *         .withConcurrency(2)
     *  }
     * </pre>
     *
     * @see GenerateQueueOptions
     * @see QueueOptions
     */
    public ClientBuilder generateQueueOptions(IQueueOptions queueOptions) {
        this.generateQueueOptions = queueOptions;
        return this;
    }

    /**
     * Set the ScheduledExecutorService for get requests, e.g. getting skins
     *
     * @deprecated use {@link #getQueueOptions(IQueueOptions)} instead
     */
    @Deprecated
    public ClientBuilder getRequestScheduler(ScheduledExecutorService scheduledExecutor) {
        this.getQueueOptions = new QueueOptions(scheduledExecutor, DEFAULT_GET_QUEUE_INTERVAL, DEFAULT_GET_QUEUE_CONCURRENCY);
        return this;
    }

    /**
     * Set the options for get requests, e.g. getting skins<br/>
     * defaults to 100ms interval and 5 concurrent requests<br/>
     * For example:
     * <pre>
     * {@code
     * GetQueueOptions.create()
     *         .withInterval(500, TimeUnit.MILLISECONDS)
     *  }
     * </pre>
     *
     * @see GetQueueOptions
     * @see QueueOptions
     */
    public ClientBuilder getQueueOptions(IQueueOptions queueOptions) {
        this.getQueueOptions = queueOptions;
        return this;
    }

    /**
     * Set the ScheduledExecutorService for checking job status
     *
     * @deprecated use {@link #jobCheckOptions(IJobCheckOptions)} instead
     */
    @Deprecated
    public ClientBuilder jobCheckScheduler(ScheduledExecutorService scheduledExecutor) {
        this.jobCheckOptions = new JobCheckOptions(scheduledExecutor, DEFAULT_JOB_CHECK_INTERVAL, DEFAULT_JOB_CHECK_INITIAL_DELAY, DEFAULT_JOB_CHECK_MAX_ATTEMPTS, false);
        return this;
    }

    /**
     * Set the options for checking job status<br/>
     * defaults to 1000ms interval, 2000ms initial delay, and 10 max attempts<br/>
     * For example:
     * <pre>
     * {@code
     * JobCheckOptions.create()
     *         .withInitialDelay(1000, TimeUnit.MILLISECONDS)
     *         .withInterval(RequestInterval.exponential())
     *         .withMaxAttempts(50)
     * }
     * </pre>
     *
     * @see JobCheckOptions
     */
    public ClientBuilder jobCheckOptions(IJobCheckOptions jobCheckOptions) {
        this.jobCheckOptions = jobCheckOptions;
        return this;
    }

    /**
     * Set the constructor for the RequestHandler
     */
    public ClientBuilder requestHandler(RequestHandlerConstructor requestHandlerConstructor) {
        this.requestHandlerConstructor = requestHandlerConstructor;
        return this;
    }

    /**
     * Build the MineSkinClient
     */
    public MineSkinClient build() {
        if (requestHandlerConstructor == null) {
            throw new IllegalStateException("RequestHandlerConstructor is not set");
        }
        if ("MineSkinClient".equals(userAgent)) {
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Using default User-Agent: MineSkinClient - Please set a custom User-Agent (e.g. AppName/Version) to identify your application");
        }
        if (apiKey == null || apiKey.isBlank()) {
            apiKey = null;
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Creating MineSkinClient without API key - Please get an API key from https://account.mineskin.org/keys");
        } else if (apiKey.startsWith("msk_")) {
            String[] split = apiKey.split("_", 3);
            if (split.length == 3) {
                String id = split[1];
                MineSkinClientImpl.LOGGER.log(Level.FINE, "Creating MineSkinClient with API key: " + id);
            }
        }

        if (getExecutor == null) {
            getExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("MineSkinClient/get");
                return thread;
            });
        }
        if (generateExecutor == null) {
            generateExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("MineSkinClient/generate");
                return thread;
            });
        }

        if (generateQueueOptions == null) {
            generateQueueOptions = new QueueOptions(
                    Executors.newSingleThreadScheduledExecutor(r -> {
                        Thread thread = new Thread(r);
                        thread.setName("MineSkinClient/scheduler");
                        return thread;
                    }),
                    DEFAULT_GENERATE_QUEUE_INTERVAL,
                    DEFAULT_GENERATE_QUEUE_CONCURRENCY
            );
        }
        if (getQueueOptions == null) {
            getQueueOptions = new QueueOptions(
                    generateQueueOptions.scheduler(),
                    DEFAULT_GET_QUEUE_INTERVAL,
                    DEFAULT_GET_QUEUE_CONCURRENCY
            );
        }
        if (jobCheckOptions == null) {
            jobCheckOptions = new JobCheckOptions(
                    generateQueueOptions.scheduler(),
                    DEFAULT_JOB_CHECK_INTERVAL,
                    DEFAULT_JOB_CHECK_INITIAL_DELAY,
                    DEFAULT_JOB_CHECK_MAX_ATTEMPTS,
                    false
            );
        }

        RequestHandler requestHandler = requestHandlerConstructor.construct(baseUrl, userAgent, apiKey, timeout, gson);
        RequestExecutors executors = new RequestExecutors(getExecutor, generateExecutor, generateQueueOptions, getQueueOptions, jobCheckOptions);
        MineSkinClientImpl client = new MineSkinClientImpl(requestHandler, executors);
        if (executors.generateQueueOptions() instanceof AutoGenerateQueueOptions autoGenerate) {
            autoGenerate.setClient(client);
        }
        return client;
    }

}
