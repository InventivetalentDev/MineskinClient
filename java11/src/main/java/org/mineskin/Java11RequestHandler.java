package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.RequestHandler;
import org.mineskin.request.RequestHandlerConstructor;
import org.mineskin.request.RequestInterceptor;
import org.mineskin.request.ResponseInterceptor;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class Java11RequestHandler extends RequestHandler {

    private final Gson gson;
    private final HttpClient httpClient;
    private final List<RequestInterceptor<HttpRequest.Builder>> requestInterceptors;
    private final List<ResponseInterceptor<HttpResponse<String>>> responseInterceptors;

    public Java11RequestHandler(String baseUrl, String userAgent, String apiKey, int timeout, Gson gson) {
        this(baseUrl, userAgent, apiKey, timeout, gson, List.of(), List.of());
    }

    private Java11RequestHandler(String baseUrl, String userAgent, String apiKey, int timeout, Gson gson,
                                 List<RequestInterceptor<HttpRequest.Builder>> requestInterceptors,
                                 List<ResponseInterceptor<HttpResponse<String>>> responseInterceptors) {
        super(baseUrl, userAgent, apiKey, timeout, gson);
        this.gson = gson;
        this.requestInterceptors = requestInterceptors;
        this.responseInterceptors = responseInterceptors;

        HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofMillis(timeout));

        if (userAgent != null) {
            clientBuilder.followRedirects(HttpClient.Redirect.NORMAL);
        }
        this.httpClient = clientBuilder.build();
    }

    /**
     * Start building a {@link Java11RequestHandler} with the given request interceptor.
     * The returned {@link Builder} implements {@link RequestHandlerConstructor} and can be passed
     * directly to {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static Builder withRequestInterceptor(RequestInterceptor<HttpRequest.Builder> interceptor) {
        return new Builder().withRequestInterceptor(interceptor);
    }

    /**
     * Start building a {@link Java11RequestHandler} with the given response interceptor.
     * The returned {@link Builder} implements {@link RequestHandlerConstructor} and can be passed
     * directly to {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static Builder withResponseInterceptor(ResponseInterceptor<HttpResponse<String>> interceptor) {
        return new Builder().withResponseInterceptor(interceptor);
    }

    private <T, R extends MineSkinResponse<T>> R wrapResponse(HttpResponse<String> response, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        for (ResponseInterceptor<HttpResponse<String>> interceptor : responseInterceptors) {
            interceptor.intercept(response);
        }
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
                throw new MineSkinRequestException(
                        wrapped.getFirstError().map(CodeAndMessage::code).orElse("request_failed"),
                        wrapped.getFirstError().map(CodeAndMessage::message).orElse("Request Failed"),
                        wrapped
                );
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

    private HttpRequest buildRequest(HttpRequest.Builder requestBuilder) {
        if (apiKey != null) {
            requestBuilder
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Accept", "application/json");
        }
        for (RequestInterceptor<HttpRequest.Builder> interceptor : requestInterceptors) {
            interceptor.intercept(requestBuilder);
        }
        return requestBuilder.build();
    }

    public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        url = this.baseUrl + url;
        MineSkinClientImpl.LOGGER.fine("GET " + url);

        HttpRequest request = buildRequest(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .header("User-Agent", this.userAgent));
        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        url = this.baseUrl + url;
        MineSkinClientImpl.LOGGER.fine("POST " + url);

        HttpRequest request = buildRequest(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(BodyPublishers.ofString(gson.toJson(data)))
                .header("Content-Type", "application/json")
                .header("User-Agent", this.userAgent));

        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url, String key, String filename, InputStream in, Map<String, String> data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        url = this.baseUrl + url;
        MineSkinClientImpl.LOGGER.fine("POST " + url);

        String boundary = "mineskin-" + System.currentTimeMillis();
        StringBuilder bodyBuilder = new StringBuilder();

        // add form fields
        for (Map.Entry<String, String> entry : data.entrySet()) {
            bodyBuilder.append("--").append(boundary).append("\r\n")
                    .append("Content-Disposition: form-data; name=\"").append(entry.getKey()).append("\"\r\n\r\n")
                    .append(entry.getValue()).append("\r\n");
        }

        // add file
        byte[] fileContent = in.readAllBytes();
        bodyBuilder.append("--").append(boundary).append("\r\n")
                .append("Content-Disposition: form-data; name=\"").append(key)
                .append("\"; filename=\"").append(filename).append("\"\r\n")
                .append("Content-Type: image/png\r\n\r\n");
        byte[] bodyStart = bodyBuilder.toString().getBytes();
        byte[] boundaryEnd = ("\r\n--" + boundary + "--\r\n").getBytes();
        byte[] bodyString = new byte[bodyStart.length + fileContent.length + boundaryEnd.length];
        System.arraycopy(bodyStart, 0, bodyString, 0, bodyStart.length);
        System.arraycopy(fileContent, 0, bodyString, bodyStart.length, fileContent.length);
        System.arraycopy(boundaryEnd, 0, bodyString, bodyStart.length + fileContent.length, boundaryEnd.length);

        HttpRequest request = buildRequest(HttpRequest.newBuilder()
                .uri(URI.create(url))
                .POST(HttpRequest.BodyPublishers.ofByteArray(bodyString))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .header("User-Agent", this.userAgent));

        HttpResponse<String> response;
        try {
            response = this.httpClient.send(request, BodyHandlers.ofString());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return wrapResponse(response, clazz, constructor);
    }

    /**
     * Builder for a {@link Java11RequestHandler} configured with request and/or response interceptors.
     * Implements {@link RequestHandlerConstructor} so it can be passed directly to
     * {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static final class Builder implements RequestHandlerConstructor {

        private final List<RequestInterceptor<HttpRequest.Builder>> requestInterceptors = new ArrayList<>();
        private final List<ResponseInterceptor<HttpResponse<String>>> responseInterceptors = new ArrayList<>();

        private Builder() {
        }

        public Builder withRequestInterceptor(RequestInterceptor<HttpRequest.Builder> interceptor) {
            this.requestInterceptors.add(interceptor);
            return this;
        }

        public Builder withResponseInterceptor(ResponseInterceptor<HttpResponse<String>> interceptor) {
            this.responseInterceptors.add(interceptor);
            return this;
        }

        @Override
        public RequestHandler construct(String baseUrl, String userAgent, String apiKey, int timeout, Gson gson) {
            return new Java11RequestHandler(baseUrl, userAgent, apiKey, timeout, gson,
                    List.copyOf(requestInterceptors), List.copyOf(responseInterceptors));
        }
    }
}
