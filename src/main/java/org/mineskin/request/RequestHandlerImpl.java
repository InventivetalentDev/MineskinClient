package org.mineskin.request;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class RequestHandlerImpl extends RequestHandler {

    private final Gson gson;
    private final HttpClient httpClient;

    public RequestHandlerImpl(
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        super(userAgent, apiKey, timeout, gson);
        this.gson = gson;

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(timeout));

        if (userAgent != null) {
            clientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
        }
        this.httpClient = clientBuilder.build();
    }

    private <T, R extends MineSkinResponse<T>> R wrapResponse(HttpResponse<String> response, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        String rawBody = response.body();
        try {
            JsonObject jsonBody = gson.fromJson(rawBody, JsonObject.class);
            R wrapped = constructor.construct(
                    response.statusCode(),
                    lowercaseHeaders(response.headers().map()),
                    jsonBody,
                    gson, clazz
            );
            if (!wrapped.isSuccess()) {
                throw new MineSkinRequestException(wrapped.getError().orElse("Request Failed"), wrapped);
            }
            return wrapped;
        } catch (JsonParseException e) {
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Failed to parse response body: " + rawBody, e);
            throw new MineskinException("Failed to parse response", e);
        }
    }

    private Map<String, String> lowercaseHeaders(Map<String, java.util.List<String>> headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().toLowerCase(),
                        entry -> String.join(", ", entry.getValue())
                ));
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", this.userAgent)
                .build();

        HttpResponse<String> response = null;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString(gson.toJson(data)))
                .header("Content-Type", "application/json")
                .header("User-Agent", this.userAgent)
                .build();

        HttpResponse<String> response = null;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url, String key, String filename, InputStream in, Map<String, String> data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {

        String boundary = "mineskin-" + System.currentTimeMillis();
        StringBuilder formBody = new StringBuilder();

        for (Map.Entry<String, String> entry : data.entrySet()) {
            formBody.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n")
                    .append(entry.getValue()).append("\r\n");
        }

        formBody.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(key).append("\"; filename=\"").append(filename).append("\"\r\n")
                .append("Content-Type: ").append(Files.probeContentType(Files.createTempFile(null, null))).append("\r\n\r\n");

        byte[] fileBytes = in.readAllBytes();
        formBody.append(new String(fileBytes))
                .append("\r\n--").append(boundary).append("--\r\n");

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString(formBody.toString()))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("User-Agent", this.userAgent)
                .build();

        HttpResponse<String> response = null;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }
}
