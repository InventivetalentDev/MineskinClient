package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.data.DelayInfo;
import org.mineskin.data.ExistingSkin;
import org.mineskin.data.GeneratedSkin;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.RequestHandler;
import org.mineskin.response.GenerateResponse;
import org.mineskin.response.GetSkinResponse;
import org.mineskin.response.MineSkinResponse;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineSkinClientImpl implements MineSkinClient {

    public static final Logger LOGGER = Logger.getLogger(MineSkinClientImpl.class.getName());

    private static final String API_BASE = "https://api.mineskin.org";
    private static final String GENERATE_BASE = API_BASE + "/generate";
    private static final String GET_BASE = API_BASE + "/get";

    private static final Map<String, AtomicLong> NEXT_REQUESTS = new ConcurrentHashMap<>();

    private final Executor generateExecutor;
    private final Executor getExecutor;

    private final RequestHandler requestHandler;

    public MineSkinClientImpl(RequestHandler requestHandler, Executor generateExecutor, Executor getExecutor) {
        this.requestHandler = checkNotNull(requestHandler);
        this.generateExecutor = checkNotNull(generateExecutor);
        this.getExecutor = checkNotNull(getExecutor);
    }

    public long getNextRequest() {
        String key = String.valueOf(requestHandler.getApiKey());
        return NEXT_REQUESTS.computeIfAbsent(key, k -> new AtomicLong(0)).get();
    }

    /////

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    public CompletableFuture<GetSkinResponse> getSkinByUuid(UUID uuid) {
        checkNotNull(uuid);
        return getSkinByUuid(uuid.toString());
    }

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    public CompletableFuture<GetSkinResponse> getSkinByUuid(String uuid) {
        checkNotNull(uuid);
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requestHandler.getJson(GET_BASE + "/uuid/" + uuid, ExistingSkin.class, GetSkinResponse::new);
            } catch (IOException e) {
                throw new MineskinException(e);
            }
        }, getExecutor);
    }

    /**
     * Generates skin data from an URL
     */
    public CompletableFuture<GenerateResponse> generateUrl(String url) {
        checkNotNull(url);
        return generateUrl(url, GenerateOptions.create());
    }

    /**
     * Generates skin data from an URL with custom options
     */
    public CompletableFuture<GenerateResponse> generateUrl(String url, GenerateOptions options) {
        checkNotNull(url);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                delayUntilNext();

                JsonObject body = options.toJson();
                body.addProperty("url", url);

                GenerateResponse res = requestHandler.postJson(GENERATE_BASE + "/url", body, GeneratedSkin.class, GenerateResponse::new);
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

    /**
     * Generates skin data by uploading an image (with default options)
     */
    public CompletableFuture<GenerateResponse> generateUpload(InputStream is) {
        return generateUpload(is, GenerateOptions.create(), null);
    }

    /**
     * Generates skin data by uploading an image with custom options
     */
    public CompletableFuture<GenerateResponse> generateUpload(InputStream is, GenerateOptions options) {
        checkNotNull(options);
        return generateUpload(is, options, options.getName() + ".png");
    }

    /**
     * Uploads and generates skin data by uploading an image (with default options)
     */
    public CompletableFuture<GenerateResponse> generateUpload(InputStream is, String fileName) {
        return generateUpload(is, GenerateOptions.create(), fileName);
    }

    /**
     * Uploads and generates skin data by uploading an image with custom options
     */
    public CompletableFuture<GenerateResponse> generateUpload(InputStream is, GenerateOptions options, String fileName) {
        checkNotNull(is);
        checkNotNull(options);
        checkNotNull(fileName);
        return CompletableFuture.supplyAsync(() -> {
            try {
                delayUntilNext();

                Map<String, String> data = options.toMap();
                GenerateResponse res = requestHandler.postFormDataFile(GENERATE_BASE + "/upload", "file", fileName, is, data, GeneratedSkin.class, GenerateResponse::new);
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

    /**
     * Uploads and generates skin data from a local file (with default options)
     */
    public CompletableFuture<GenerateResponse> generateUpload(File file) throws FileNotFoundException {
        return generateUpload(file, GenerateOptions.create());
    }

    /**
     * Uploads and generates skin data from a local file with custom options
     */
    public CompletableFuture<GenerateResponse> generateUpload(File file, GenerateOptions options) throws FileNotFoundException {
        checkNotNull(file);
        checkNotNull(options);
        return generateUpload(new FileInputStream(file), options, file.getName());
    }

    /**
     * Uploads and generates skin data from a RenderedImage object (with default options)
     */
    public CompletableFuture<GenerateResponse> generateUpload(RenderedImage image) throws IOException {
        return generateUpload(image, GenerateOptions.create());
    }

    /**
     * Uploads and generates skin data from a RenderedImage object with custom options
     */
    public CompletableFuture<GenerateResponse> generateUpload(RenderedImage image, GenerateOptions options) throws IOException {
        checkNotNull(image);
        checkNotNull(options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return generateUpload(new ByteArrayInputStream(baos.toByteArray()), options);
    }

    /**
     * Loads skin data from an existing player
     */
    public CompletableFuture<GenerateResponse> generateUser(UUID uuid) {
        return generateUser(uuid, GenerateOptions.create());
    }


    /**
     * Loads skin data from an existing player with custom options
     */
    public CompletableFuture<GenerateResponse> generateUser(UUID uuid, GenerateOptions options) {
        checkNotNull(uuid);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                delayUntilNext();

                JsonObject body = options.toJson();
                body.addProperty("uuid", uuid.toString());

                GenerateResponse res = requestHandler.postJson(GENERATE_BASE + "/user", body, GeneratedSkin.class, GenerateResponse::new);
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

    private void handleResponse(MineSkinResponse<?> response) {
        if (response instanceof GenerateResponse generateResponse) {
            handleDelayInfo(generateResponse.getDelayInfo());
        }
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
        String key = String.valueOf(requestHandler.getApiKey());
        NEXT_REQUESTS.compute(key, (k, v) -> {
            if (v == null) {
                v = new AtomicLong(System.currentTimeMillis());
            }
            v.addAndGet(delayInfo.millis() + 10);
            return v;
        });
    }

}
