package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class JsoupRequestHandler extends RequestHandler {

    private final String userAgent;
    private final String apiKey;
    private final int timeout;
    private final List<RequestInterceptor<Connection>> requestInterceptors;
    private final List<ResponseInterceptor<Connection.Response>> responseInterceptors;

    public JsoupRequestHandler(
            String baseUrl,
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        this(baseUrl, userAgent, apiKey, timeout, gson, List.of(), List.of());
    }

    private JsoupRequestHandler(
            String baseUrl,
            String userAgent, String apiKey,
            int timeout,
            Gson gson,
            List<RequestInterceptor<Connection>> requestInterceptors,
            List<ResponseInterceptor<Connection.Response>> responseInterceptors) {
        super(baseUrl, userAgent, apiKey, timeout, gson);
        this.userAgent = userAgent;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.requestInterceptors = requestInterceptors;
        this.responseInterceptors = responseInterceptors;
    }

    /**
     * Start building a {@link JsoupRequestHandler} with the given request interceptor.
     * The returned {@link Builder} implements {@link RequestHandlerConstructor} and can be passed
     * directly to {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static Builder withRequestInterceptor(RequestInterceptor<Connection> interceptor) {
        return new Builder().withRequestInterceptor(interceptor);
    }

    /**
     * Start building a {@link JsoupRequestHandler} with the given response interceptor.
     * The returned {@link Builder} implements {@link RequestHandlerConstructor} and can be passed
     * directly to {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static Builder withResponseInterceptor(ResponseInterceptor<Connection.Response> interceptor) {
        return new Builder().withResponseInterceptor(interceptor);
    }

    private Connection requestBase(Connection.Method method, String url) {
        url = this.baseUrl + url;
        MineSkinClientImpl.LOGGER.log(Level.FINE, method + " " + url);
        Connection connection = Jsoup.connect(url)
                .method(method)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(timeout);
        if (apiKey != null) {
            connection.header("Authorization", "Bearer " + apiKey);
        }
        return connection;
    }

    private Connection.Response execute(Connection connection) throws IOException {
        for (RequestInterceptor<Connection> interceptor : requestInterceptors) {
            interceptor.intercept(connection);
        }
        return connection.execute();
    }

    private <T, R extends MineSkinResponse<T>> R wrapResponse(Connection.Response response, Class<T> clazz, ResponseConstructor<T, R> constructor) {
        for (ResponseInterceptor<Connection.Response> interceptor : responseInterceptors) {
            interceptor.intercept(response);
        }
        try {
            JsonObject jsonBody = parseJsonBody(response);
            R wrapped = constructor.construct(
                    response.statusCode(),
                    lowercaseHeaders(response.headers()),
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
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Failed to parse response body: " + response.body(), e);
            throw new MineskinException("Failed to parse response", e);
        }
    }

    private JsonObject parseJsonBody(Connection.Response response) {
        JsonElement parsed;
        try {
            parsed = gson.fromJson(response.body(), JsonElement.class);
        } catch (JsonParseException e) {
            return syntheticErrorBody(response, "invalid_json", e.getMessage());
        }
        if (parsed != null && parsed.isJsonObject()) return parsed.getAsJsonObject();
        String snippet = response.body() == null ? "null" : response.body();
        if (snippet.length() > 200) snippet = snippet.substring(0, 200) + "...";
        return syntheticErrorBody(response, "non_json_response", "HTTP " + response.statusCode() + ": " + snippet);
    }

    private JsonObject syntheticErrorBody(Connection.Response response, String code, String message) {
        JsonObject error = new JsonObject();
        error.add("code", new JsonPrimitive(code));
        error.add("message", new JsonPrimitive(message == null ? "" : message));
        JsonArray errors = new JsonArray();
        errors.add(error);
        JsonObject body = new JsonObject();
        body.add("success", new JsonPrimitive(false));
        body.add("errors", errors);
        return body;
    }

    private Map<String, String> lowercaseHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        Connection.Response response = execute(requestBase(Connection.Method.GET, url));
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        Connection connection = requestBase(Connection.Method.POST, url)
                .requestBody(data.toString())
                .header("Content-Type", "application/json");
        Connection.Response response = execute(connection);
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url,
                                                                 String key, String filename, InputStream in,
                                                                 Map<String, String> data,
                                                                 Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        Connection connection = requestBase(Connection.Method.POST, url)
                .header("Content-Type", "multipart/form-data");
        connection.data(key, filename, in);
        connection.data(data);
        Connection.Response response = execute(connection);
        return wrapResponse(response, clazz, constructor);
    }

    /**
     * Builder for a {@link JsoupRequestHandler} configured with request and/or response interceptors.
     * Implements {@link RequestHandlerConstructor} so it can be passed directly to
     * {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static final class Builder implements RequestHandlerConstructor {

        private final List<RequestInterceptor<Connection>> requestInterceptors = new ArrayList<>();
        private final List<ResponseInterceptor<Connection.Response>> responseInterceptors = new ArrayList<>();

        private Builder() {
        }

        public Builder withRequestInterceptor(RequestInterceptor<Connection> interceptor) {
            this.requestInterceptors.add(interceptor);
            return this;
        }

        public Builder withResponseInterceptor(ResponseInterceptor<Connection.Response> interceptor) {
            this.responseInterceptors.add(interceptor);
            return this;
        }

        @Override
        public RequestHandler construct(String baseUrl, String userAgent, String apiKey, int timeout, Gson gson) {
            return new JsoupRequestHandler(baseUrl, userAgent, apiKey, timeout, gson,
                    List.copyOf(requestInterceptors), List.copyOf(responseInterceptors));
        }
    }

}
