package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.data.DelayInfo;
import org.mineskin.data.MineskinException;
import org.mineskin.data.Skin;
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

import static com.google.common.base.Preconditions.checkNotNull;

public class MineskinClient {

    private static final String API_BASE = "https://api.mineskin.org";
    private static final String GENERATE_BASE = API_BASE + "/generate";
    private static final String GET_BASE = API_BASE + "/get";

    private static final String ID_FORMAT = "https://api.mineskin.org/get/id/%s";
    private static final String URL_FORMAT = "https://api.mineskin.org/generate/url?url=%s&%s";
    private static final String UPLOAD_FORMAT = "https://api.mineskin.org/generate/upload?%s";
    private static final String USER_FORMAT = "https://api.mineskin.org/generate/user/%s?%s";

    private final Executor generateExecutor;
    private final Executor getExecutor;

    private final RequestHandler requestHandler;

    private long nextRequest = 0;


//    public MineskinClient(Executor generateExecutor, String userAgent, String apiKey) {
//        this.generateExecutor = checkNotNull(generateExecutor);
//        this.userAgent = checkNotNull(userAgent);
//        this.apiKey = apiKey;
//    }

    public MineskinClient(RequestHandler requestHandler, Executor generateExecutor, Executor getExecutor) {
        this.requestHandler = checkNotNull(requestHandler);
        this.generateExecutor = checkNotNull(generateExecutor);
        this.getExecutor = checkNotNull(getExecutor);
    }

    public long getNextRequest() {
        return nextRequest;
    }

    /////

    public CompletableFuture<GetSkinResponse> getSkinByUuid(UUID uuid) {
        return getSkinByUuid(uuid.toString());
    }

    public CompletableFuture<GetSkinResponse> getSkinByUuid(String uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requestHandler.getJson(GET_BASE + "/uuid/" + uuid, Skin.class, GetSkinResponse::new);
            } catch (IOException e) {
                throw new MineskinException(e);
            }
        }, getExecutor);
    }

    public CompletableFuture<GenerateResponse> generateUrl(String url) {
        return generateUrl(url, GenerateOptions.create());
    }

    /**
     * Generates skin data from an URL
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


    public CompletableFuture<GenerateResponse> generateUpload(InputStream is) {
        return generateUpload(is, GenerateOptions.create(), null);
    }

    public CompletableFuture<GenerateResponse> generateUpload(InputStream is, GenerateOptions options) {
        return generateUpload(is, options, options.getName() + ".png");
    }

    public CompletableFuture<GenerateResponse> generateUpload(InputStream is, String name) {
        return generateUpload(is, GenerateOptions.create(), name);
    }

    public CompletableFuture<GenerateResponse> generateUpload(InputStream is, GenerateOptions options, String fileName) {
        checkNotNull(is);
        checkNotNull(options);
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

    public CompletableFuture<GenerateResponse> generateUpload(RenderedImage image, GenerateOptions options) throws IOException {
        checkNotNull(image);
        checkNotNull(options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return generateUpload(new ByteArrayInputStream(baos.toByteArray()), options);
    }

    public CompletableFuture<GenerateResponse> generateUser(UUID uuid) {
        return generateUser(uuid, GenerateOptions.create());
    }


    /**
     * Loads skin data from an existing player
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

    void delayUntilNext() {
        if (System.currentTimeMillis() < nextRequest) {
            long delay = (nextRequest - System.currentTimeMillis());
            try {
                Thread.sleep(delay + 1);
            } catch (InterruptedException e) {
                throw new MineskinException("Interrupted while waiting for next request", e);
            }
        }
    }

    void handleDelayInfo(DelayInfo delayInfo) {
        this.nextRequest = System.currentTimeMillis() + delayInfo.millis + 1;
    }

}
