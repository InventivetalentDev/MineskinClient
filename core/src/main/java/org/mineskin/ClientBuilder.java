package org.mineskin;

import com.google.gson.Gson;
import org.mineskin.request.RequestHandler;
import org.mineskin.request.RequestHandlerConstructor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;

public class ClientBuilder {

    private String baseUrl = "https://api.mineskin.org";
    private String userAgent = "MineSkinClient";
    private String apiKey = null;
    private int timeout = 10000;
    private Gson gson = new Gson();
    private Executor getExecutor = null;
    private Executor generateExecutor = null;
    private ScheduledExecutorService generateRequestScheduler = null;
    private ScheduledExecutorService getRequestScheduler = null;
    private ScheduledExecutorService jobCheckScheduler = null;
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
     */
    public ClientBuilder generateRequestScheduler(ScheduledExecutorService scheduledExecutor) {
        this.generateRequestScheduler = scheduledExecutor;
        return this;
    }

    /**
     * Set the ScheduledExecutorService for get requests, e.g. getting skins
     */
    public ClientBuilder getRequestScheduler(ScheduledExecutorService scheduledExecutor) {
        this.getRequestScheduler = scheduledExecutor;
        return this;
    }

    /**
     * Set the ScheduledExecutorService for checking job status
     */
    public ClientBuilder jobCheckScheduler(ScheduledExecutorService scheduledExecutor) {
        this.jobCheckScheduler = scheduledExecutor;
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

        if (generateRequestScheduler == null) {
            generateRequestScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r);
                thread.setName("MineSkinClient/scheduler");
                return thread;
            });
        }
        if (getRequestScheduler == null) {
            getRequestScheduler = generateRequestScheduler;
        }
        if (jobCheckScheduler == null) {
            jobCheckScheduler = generateRequestScheduler;
        }

        RequestHandler requestHandler = requestHandlerConstructor.construct(baseUrl, userAgent, apiKey, timeout, gson);
        RequestExecutors executors = new RequestExecutors(getExecutor, generateExecutor, generateRequestScheduler, getRequestScheduler, jobCheckScheduler);
        return new MineSkinClientImpl(requestHandler, executors);
    }

}
