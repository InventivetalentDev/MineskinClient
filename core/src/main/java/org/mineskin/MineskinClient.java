package org.mineskin;

import com.google.gson.Gson;
import org.mineskin.data.MineskinException;
import org.mineskin.data.Skin;
import org.mineskin.response.GetSkinResponse;

import java.io.IOException;
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

    private final Gson gson = new Gson();

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

//    public CompletableFuture<Skin> generateUrl(String url) {
//        return generateUrl(url, GenerateOptions.none());
//    }
//
//    /**
//     * Generates skin data from an URL
//     */
//    public CompletableFuture<Skin> generateUrl(String url, GenerateOptions options) {
//        checkNotNull(url);
//        checkNotNull(options);
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                if (System.currentTimeMillis() < nextRequest) {
//                    long delay = (nextRequest - System.currentTimeMillis());
//                    Thread.sleep(delay + 10);
//                }
//
//                JsonObject body = options.toJson();
//                body.addProperty("url", url);
//                Connection connection = generateRequest("/url")
//                        .header("Content-Type", "application/json")
//                        .requestBody(body.toString());
//                return handleResponse(connection.execute().body());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }, generateExecutor);
//    }
//
//    public CompletableFuture<Skin> generateUpload(InputStream is) {
//        return generateUpload(is, GenerateOptions.none(), null);
//    }
//
//    public CompletableFuture<Skin> generateUpload(InputStream is, GenerateOptions options) {
//        return generateUpload(is, options, options.getName() + ".png");
//    }
//
//    public CompletableFuture<Skin> generateUpload(InputStream is, String name) {
//        return generateUpload(is, GenerateOptions.none(), name);
//    }
//
//    public CompletableFuture<Skin> generateUpload(InputStream is, GenerateOptions options, String name) {
//        checkNotNull(is);
//        checkNotNull(options);
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                if (System.currentTimeMillis() < nextRequest) {
//                    long delay = (nextRequest - System.currentTimeMillis());
//                    Thread.sleep(delay + 10);
//                }
//
//                Connection connection = generateRequest("/upload")
//                        // It really doesn't like setting a content-type header here for some reason
//                        .data("file", name, is);
//                options.addAsData(connection);
//                return handleResponse(connection.execute().body());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }, generateExecutor);
//    }
//
//    /**
//     * Uploads and generates skin data from a local file (with default options)
//     */
//    public CompletableFuture<Skin> generateUpload(File file) throws FileNotFoundException {
//        return generateUpload(file, GenerateOptions.none());
//    }
//
//    public CompletableFuture<Skin> generateUpload(File file, GenerateOptions options) throws FileNotFoundException {
//        checkNotNull(file);
//        checkNotNull(options);
//        return generateUpload(new FileInputStream(file), options, file.getName());
//    }
//
//    /**
//     * Uploads and generates skin data from a RenderedImage object (with default options)
//     */
//    public CompletableFuture<Skin> generateUpload(RenderedImage image) throws IOException {
//        return generateUpload(image, GenerateOptions.none());
//    }
//
//    public CompletableFuture<Skin> generateUpload(RenderedImage image, GenerateOptions options) throws IOException {
//        checkNotNull(image);
//        checkNotNull(options);
//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        ImageIO.write(image, "png", baos);
//        return generateUpload(new ByteArrayInputStream(baos.toByteArray()), options);
//    }
//
//    public CompletableFuture<Skin> generateUser(UUID uuid) {
//        return generateUser(uuid, GenerateOptions.none());
//    }
//
//    /**
//     * Loads skin data from an existing player
//     */
//    public CompletableFuture<Skin> generateUser(UUID uuid, GenerateOptions options) {
//        checkNotNull(uuid);
//        checkNotNull(options);
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                if (System.currentTimeMillis() < nextRequest) {
//                    long delay = (nextRequest - System.currentTimeMillis());
//                    Thread.sleep(delay + 10);
//                }
//
//                JsonObject body = options.toJson();
//                body.addProperty("uuid", uuid.toString());
//                Connection connection = generateRequest("/user")
//                        .header("Content-Type", "application/json")
//                        .requestBody(body.toString());
//                return handleResponse(connection.execute().body());
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }, generateExecutor);
//    }
//
//    Skin handleResponse(String body) throws MineskinException, JsonParseException {
//        JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
//        if (jsonObject.has("error")) {
//            throw new MineskinException(jsonObject.get("error").getAsString());
//        }
//
//        Skin skin = gson.fromJson(jsonObject, Skin.class);
//        this.nextRequest = System.currentTimeMillis() + ((long) (skin.delayInfo.millis + (this.apiKey == null ? 10_000 : 100)));
//        return skin;
//    }

}
