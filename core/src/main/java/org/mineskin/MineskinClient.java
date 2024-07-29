package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mineskin.data.MineskinException;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;

import javax.imageio.ImageIO;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

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

    private final String userAgent;
    private final String apiKey;

    private final Gson gson = new Gson();

    private final RequestHandler requestHandler;

    private long nextRequest = 0;


    public MineskinClient(Executor generateExecutor, String userAgent, String apiKey) {
        this.generateExecutor = checkNotNull(generateExecutor);
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = apiKey;
    }

    public MineskinClient(RequestHandler requestHandler) {
        this.requestHandler = checkNotNull(requestHandler);
    }

    public long getNextRequest() {
        return nextRequest;
    }

    /////


    public CompletableFuture<Skin> getSkinByUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return requestHandler.getJson(GET_BASE + "/uuid/" + uuid, Skin.class);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, getExecutor);
    }

    public CompletableFuture<Skin> generateUrl(String url) {
        return generateUrl(url, SkinOptions.none());
    }

    /**
     * Generates skin data from an URL
     */
    public CompletableFuture<Skin> generateUrl(String url, SkinOptions options) {
        checkNotNull(url);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 10);
                }

                JsonObject body = options.toJson();
                body.addProperty("url", url);
                Connection connection = generateRequest("/url")
                        .header("Content-Type", "application/json")
                        .requestBody(body.toString());
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, generateExecutor);
    }

    public CompletableFuture<Skin> generateUpload(InputStream is) {
        return generateUpload(is, SkinOptions.none(), null);
    }

    public CompletableFuture<Skin> generateUpload(InputStream is, SkinOptions options) {
        return generateUpload(is, options, options.getName() + ".png");
    }

    public CompletableFuture<Skin> generateUpload(InputStream is, String name) {
        return generateUpload(is, SkinOptions.none(), name);
    }

    public CompletableFuture<Skin> generateUpload(InputStream is, SkinOptions options, String name) {
        checkNotNull(is);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 10);
                }

                Connection connection = generateRequest("/upload")
                        // It really doesn't like setting a content-type header here for some reason
                        .data("file", name, is);
                options.addAsData(connection);
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, generateExecutor);
    }

    /**
     * Uploads and generates skin data from a local file (with default options)
     */
    public CompletableFuture<Skin> generateUpload(File file) throws FileNotFoundException {
        return generateUpload(file, SkinOptions.none());
    }

    public CompletableFuture<Skin> generateUpload(File file, SkinOptions options) throws FileNotFoundException {
        checkNotNull(file);
        checkNotNull(options);
        return generateUpload(new FileInputStream(file), options, file.getName());
    }

    /**
     * Uploads and generates skin data from a RenderedImage object (with default options)
     */
    public CompletableFuture<Skin> generateUpload(RenderedImage image) throws IOException {
        return generateUpload(image, SkinOptions.none());
    }

    public CompletableFuture<Skin> generateUpload(RenderedImage image, SkinOptions options) throws IOException {
        checkNotNull(image);
        checkNotNull(options);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return generateUpload(new ByteArrayInputStream(baos.toByteArray()), options);
    }

    public CompletableFuture<Skin> generateUser(UUID uuid) {
        return generateUser(uuid, SkinOptions.none());
    }

    /**
     * Loads skin data from an existing player
     */
    public CompletableFuture<Skin> generateUser(UUID uuid, SkinOptions options) {
        checkNotNull(uuid);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 10);
                }

                JsonObject body = options.toJson();
                body.addProperty("uuid", uuid.toString());
                Connection connection = generateRequest("/user")
                        .header("Content-Type", "application/json")
                        .requestBody(body.toString());
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, generateExecutor);
    }

    Skin handleResponse(String body) throws MineskinException, JsonParseException {
        JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
        if (jsonObject.has("error")) {
            throw new MineskinException(jsonObject.get("error").getAsString());
        }

        Skin skin = gson.fromJson(jsonObject, Skin.class);
        this.nextRequest = System.currentTimeMillis() + ((long) (skin.delayInfo.millis + (this.apiKey == null ? 10_000 : 100)));
        return skin;
    }

    ///// SkinCallback stuff below


    /*
     * ID
     */

    /**
     * Gets data for an existing Skin
     *
     * @param id       Skin-Id
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void getSkin(int id, SkinCallback callback) {
        checkNotNull(callback);
        generateExecutor.execute(() -> {
            try {
                Connection connection = Jsoup
                        .connect(String.format(ID_FORMAT, id))
                        .userAgent(userAgent)
                        .method(Connection.Method.GET)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(10000);
                String body = connection.execute().body();
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /*
     * URL
     */

    /**
     * Generates skin data from an URL (with default options)
     *
     * @param url      URL
     * @param callback {@link SkinCallback}
     * @see #generateUrl(String, SkinOptions, SkinCallback)
     */
    @Deprecated
    public void generateUrl(String url, SkinCallback callback) {
        generateUrl(url, SkinOptions.none(), callback);
    }

    /**
     * Generates skin data from an URL
     *
     * @param url      URL
     * @param options  {@link SkinOptions}
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUrl(String url, SkinOptions options, SkinCallback callback) {
        checkNotNull(url);
        checkNotNull(options);
        checkNotNull(callback);
        generateExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 10);
                }

                callback.uploading();

                Connection connection = Jsoup
                        .connect(String.format(URL_FORMAT, url, options.toUrlParam()))
                        .userAgent(userAgent)
                        .method(Connection.Method.POST)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(40000);
                if (apiKey != null) {
                    connection.header("Authorization", "Bearer " + apiKey);
                }
                String body = connection.execute().body();
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /*
     * Upload
     */

    /**
     * Uploads and generates skin data from a local file (with default options)
     *
     * @param file     File to upload
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUpload(File file, SkinCallback callback) {
        generateUpload(file, SkinOptions.none(), callback);
    }

    /**
     * Uploads and generates skin data from a local file
     *
     * @param file     File to upload
     * @param options  {@link SkinOptions}
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUpload(File file, SkinOptions options, SkinCallback callback) {
        checkNotNull(file);
        checkNotNull(options);
        checkNotNull(callback);
        generateExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 10);
                }

                callback.uploading();

                Connection connection = Jsoup
                        .connect(String.format(UPLOAD_FORMAT, options.toUrlParam()))
                        .userAgent(userAgent)
                        .method(Connection.Method.POST)
                        .data("file", file.getName(), new FileInputStream(file))
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(40000);
                if (apiKey != null) {
                    connection.header("Authorization", "Bearer " + apiKey);
                }
                String body = connection.execute().body();
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    /*
     * User
     */

    /**
     * Loads skin data from an existing player (with default options)
     *
     * @param uuid     {@link UUID} of the player
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUser(UUID uuid, SkinCallback callback) {
        generateUser(uuid, SkinOptions.none(), callback);
    }

    /**
     * Loads skin data from an existing player
     *
     * @param uuid     {@link UUID} of the player
     * @param options  {@link SkinOptions}
     * @param callback {@link SkinCallback}
     */
    @Deprecated
    public void generateUser(UUID uuid, SkinOptions options, SkinCallback callback) {
        checkNotNull(uuid);
        checkNotNull(options);
        checkNotNull(callback);
        generateExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 10);
                }

                callback.uploading();

                Connection connection = Jsoup
                        .connect(String.format(USER_FORMAT, uuid.toString(), options.toUrlParam()))
                        .userAgent(userAgent)
                        .method(Connection.Method.GET)
                        .ignoreContentType(true)
                        .ignoreHttpErrors(true)
                        .timeout(40000);
                if (apiKey != null) {
                    connection.header("Authorization", "Bearer " + apiKey);
                }
                String body = connection.execute().body();
                handleResponse(body, callback);
            } catch (Exception e) {
                callback.exception(e);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        });
    }

    @Deprecated
    void handleResponse(String body, SkinCallback callback) {
        try {
            JsonObject jsonObject = jsonParser.parse(body).getAsJsonObject();
            if (jsonObject.has("error")) {
                callback.error(jsonObject.get("error").getAsString());
                return;
            }

            Skin skin = gson.fromJson(jsonObject, Skin.class);
            this.nextRequest = System.currentTimeMillis() + ((long) (skin.delayInfo.millis + (this.apiKey == null ? 10_000 : 0)));
            callback.done(skin);
        } catch (JsonParseException e) {
            callback.parseException(e, body);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
