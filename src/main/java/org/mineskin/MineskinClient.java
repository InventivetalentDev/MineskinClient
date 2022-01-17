package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mineskin.data.MineskinException;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;

import javax.imageio.ImageIO;
import java.awt.*;
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

    private final Executor requestExecutor;
    private final String userAgent;
    private final String apiKey;

    private final JsonParser jsonParser = new JsonParser();
    private final Gson gson = new Gson();

    private long nextRequest = 0;

    @Deprecated
    public MineskinClient() {
        this.requestExecutor = Executors.newSingleThreadExecutor();
        this.userAgent = "MineSkin-JavaClient";
        this.apiKey = null;
    }

    @Deprecated
    public MineskinClient(Executor requestExecutor) {
        this.requestExecutor = checkNotNull(requestExecutor);
        this.userAgent = "MineSkin-JavaClient";
        this.apiKey = null;
    }

    public MineskinClient(String userAgent) {
        this.requestExecutor = Executors.newSingleThreadExecutor();
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = null;
    }

    public MineskinClient(String userAgent, String apiKey) {
        this.requestExecutor = Executors.newSingleThreadExecutor();
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = apiKey;
    }

    public MineskinClient(Executor requestExecutor, String userAgent, String apiKey) {
        this.requestExecutor = checkNotNull(requestExecutor);
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = apiKey;
    }

    public MineskinClient(Executor requestExecutor, String userAgent) {
        this.requestExecutor = checkNotNull(requestExecutor);
        this.userAgent = checkNotNull(userAgent);
        this.apiKey = null;
    }

    public long getNextRequest() {
        return nextRequest;
    }

    /////

    private Connection generateRequest(String endpoint) {
        Connection connection = Jsoup.connect(GENERATE_BASE + endpoint)
                .method(Connection.Method.POST)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(30000);
        if (apiKey != null) {
            connection.header("Authorization", "Bearer " + apiKey);
        }
        return connection;
    }

    private Connection getRequest(String endpoint) {
        return Jsoup.connect(GET_BASE + endpoint)
                .method(Connection.Method.GET)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(5000);
    }


    public CompletableFuture<Skin> getId(long id) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = getRequest("/id/" + id);
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }

    public CompletableFuture<Skin> getUuid(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Connection connection = getRequest("/uuid/" + uuid);
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
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
                    Thread.sleep(delay + 1000);
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
        }, requestExecutor);
    }

    public CompletableFuture<Skin> generateUpload(InputStream is) {
        return generateUpload(is, SkinOptions.none());
    }


    public CompletableFuture<Skin> generateUpload(InputStream is, SkinOptions options) {
        checkNotNull(is);
        checkNotNull(options);
        return CompletableFuture.supplyAsync(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    Thread.sleep(delay + 1000);
                }

                Connection connection = generateRequest("/upload")
                        // It really doesn't like setting a content-type header here for some reason
                        .data("file", options.getName(), is);
                options.addAsData(connection);
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
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
        return generateUpload(new FileInputStream(file));
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
                    Thread.sleep(delay + 1000);
                }

                JsonObject body = options.toJson();
                body.addProperty("user", uuid.toString());
                Connection connection = generateRequest("/user")
                        .header("Content-Type", "application/json")
                        .requestBody(body.toString());
                return handleResponse(connection.execute().body());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, requestExecutor);
    }

    Skin handleResponse(String body) throws MineskinException, JsonParseException {
        JsonObject jsonObject = gson.fromJson(body, JsonObject.class);
        if (jsonObject.has("error")) {
            throw new MineskinException(jsonObject.get("error").getAsString());
        }

        Skin skin = gson.fromJson(jsonObject, Skin.class);
        this.nextRequest = System.currentTimeMillis() + ((long) ((skin.nextRequest + 10) * 1000L));
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
        requestExecutor.execute(() -> {
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
        requestExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 1000);
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
        requestExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 1000);
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
        requestExecutor.execute(() -> {
            try {
                if (System.currentTimeMillis() < nextRequest) {
                    long delay = (nextRequest - System.currentTimeMillis());
                    callback.waiting(delay);
                    Thread.sleep(delay + 1000);
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
            this.nextRequest = System.currentTimeMillis() + ((long) ((skin.nextRequest + 10) * 1000L));
            callback.done(skin);
        } catch (JsonParseException e) {
            callback.parseException(e, body);
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

}
