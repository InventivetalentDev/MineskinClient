package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.data.DelayInfo;
import org.mineskin.exception.MineskinException;
import org.mineskin.data.Skin;
import org.mineskin.request.RequestHandler;
import org.mineskin.response.GenerateResponse;
import org.mineskin.response.GetSkinResponse;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import static com.google.common.base.Preconditions.checkNotNull;

public class MineSkinClient {

    public static final Logger LOGGER = Logger.getLogger(MineSkinClient.class.getName());

    private static final String API_BASE = "https://api.mineskin.org";
    private static final String GENERATE_BASE = API_BASE + "/generate";
    private static final String GET_BASE = API_BASE + "/get";


    private final Executor generateExecutor;
    private final Executor getExecutor;

    private final RequestHandler requestHandler;

    private final AtomicLong nextRequest = new AtomicLong(0);

    public MineSkinClient(RequestHandler requestHandler, Executor generateExecutor, Executor getExecutor) {
        this.requestHandler = checkNotNull(requestHandler);
        this.generateExecutor = checkNotNull(generateExecutor);
        this.getExecutor = checkNotNull(getExecutor);
    }

    public static ClientBuilder builder() {
        return ClientBuilder.create();
    }

    public long getNextRequest() {
        return nextRequest.get();
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
                return requestHandler.getJson(GET_BASE + "/uuid/" + uuid, Skin.class, GetSkinResponse::new);
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

                GenerateResponse res = requestHandler.postJson(GENERATE_BASE + "/url", body, Skin.class, GenerateResponse::new);
                handleDelayInfo(res.getDelayInfo());
                return res;
            } catch (IOException e) {
                throw new MineskinException(e);
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

                Map<String, String> data = new HashMap<>();
                options.addTo(data);
                GenerateResponse res = requestHandler.postFormDataFile(GENERATE_BASE + "/upload", "file", fileName, is, data, Skin.class, GenerateResponse::new);
                handleDelayInfo(res.getDelayInfo());
                return res;
            } catch (IOException e) {
                throw new MineskinException(e);
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

                GenerateResponse res = requestHandler.postJson(GENERATE_BASE + "/user", body, Skin.class, GenerateResponse::new);
                handleDelayInfo(res.getDelayInfo());
                return res;
            } catch (IOException e) {
                throw new MineskinException(e);
            }
        }, generateExecutor);
    }

    private void delayUntilNext() {
        if (System.currentTimeMillis() < nextRequest.get()) {
            long delay = (nextRequest.get() - System.currentTimeMillis());
            try {
                Thread.sleep(delay + 1);
            } catch (InterruptedException e) {
                throw new MineskinException("Interrupted while waiting for next request", e);
            }
        }
    }

    private void handleDelayInfo(DelayInfo delayInfo) {
        this.nextRequest.set(System.currentTimeMillis() + delayInfo.millis() + 1);
    }

}
