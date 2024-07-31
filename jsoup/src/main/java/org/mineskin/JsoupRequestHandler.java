package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.RequestHandler;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class JsoupRequestHandler extends RequestHandler {

    private final String userAgent;
    private final String apiKey;
    private final int timeout;

    public JsoupRequestHandler(
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        super(userAgent, apiKey, timeout, gson);
        this.userAgent = userAgent;
        this.apiKey = apiKey;
        this.timeout = timeout;
    }

    private Connection requestBase(Connection.Method method, String url) {
        MineSkinClient.LOGGER.log(Level.FINE, method + " " + url);
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

    private <T, R extends MineSkinResponse<T>> R wrapResponse(Connection.Response response, Class<T> clazz, ResponseConstructor<T, R> constructor) {
        try {
            JsonObject jsonBody = gson.fromJson(response.body(), JsonObject.class);
            R wrapped = constructor.construct(
                    response.statusCode(),
                    lowercaseHeaders(response.headers()),
                    jsonBody,
                    gson, clazz
            );
            if (!wrapped.isSuccess()) {
                throw new MineSkinRequestException(wrapped.getError().orElse("Request Failed"), wrapped);
            }
            return wrapped;
        } catch (JsonParseException e) {
            MineSkinClient.LOGGER.log(Level.WARNING, "Failed to parse response body: " + response.body(), e);
            throw new MineskinException("Failed to parse response", e);
        }
    }

    private Map<String, String> lowercaseHeaders(Map<String, String> headers) {
        return headers.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().toLowerCase(), Map.Entry::getValue));
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        Connection.Response response = requestBase(Connection.Method.GET, url).execute();
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        Connection.Response response = requestBase(Connection.Method.POST, url)
                .requestBody(data.toString())
                .header("Content-Type", "application/json")
                .execute();
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
        Connection.Response response = connection.execute();
        return wrapResponse(response, clazz, constructor);
    }

}
