package org.mineskin.request;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.exception.UnexpectedResponseException;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.logging.Level;

public abstract class RequestHandler {

    protected final Gson gson;
    protected final String baseUrl;
    protected final String userAgent;
    protected final String apiKey;

    public RequestHandler(String baseUrl, String userAgent, String apiKey, int timeout, Gson gson) {
        this.baseUrl = baseUrl;
        this.userAgent = userAgent;
        this.apiKey = apiKey;
        this.gson = gson;
    }

    public String getApiKey() {
        return apiKey;
    }

    public abstract <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    public abstract <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    public abstract <T, R extends MineSkinResponse<T>> R postFormDataFile(String url,
                                                                                  String key, String filename, InputStream in,
                                                                                  Map<String, String> data,
                                                                                  Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    /**
     * Parse a raw response into a {@link MineSkinResponse}.
     *
     * @param headers response headers with lowercase keys
     * @throws UnexpectedResponseException if the body isn't a JSON object (e.g. a Cloudflare error page)
     * @throws MineSkinRequestException    if the API reports the request as failed
     */
    protected <T, R extends MineSkinResponse<T>> R handleResponse(int status, Map<String, String> headers, @Nullable String rawBody,
                                                                  Class<T> clazz, ResponseConstructor<T, R> constructor) {
        if (!looksLikeJsonObject(rawBody)) {
            throw logUnexpected(UnexpectedResponseException.from(status, headers, rawBody), rawBody);
        }
        JsonObject jsonBody;
        try {
            jsonBody = gson.fromJson(rawBody, JsonObject.class);
        } catch (JsonParseException e) {
            throw logUnexpected(UnexpectedResponseException.from(status, headers, rawBody, e), rawBody);
        }
        R wrapped;
        try {
            wrapped = constructor.construct(status, headers, jsonBody, gson, clazz);
        } catch (JsonParseException e) {
            MineSkinClientImpl.LOGGER.log(Level.WARNING, "Failed to parse response body: " + rawBody, e);
            throw new MineskinException("Failed to parse response", e);
        }
        if (!wrapped.isSuccess()) {
            throw new MineSkinRequestException(
                    wrapped.getFirstError().map(CodeAndMessage::code).orElse("request_failed"),
                    wrapped.getFirstError().map(CodeAndMessage::message).orElse("Request Failed"),
                    wrapped
            );
        }
        return wrapped;
    }

    private static boolean looksLikeJsonObject(@Nullable String body) {
        if (body == null) return false;
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (!Character.isWhitespace(c)) {
                return c == '{';
            }
        }
        return false;
    }

    private static UnexpectedResponseException logUnexpected(UnexpectedResponseException e, @Nullable String rawBody) {
        MineSkinClientImpl.LOGGER.log(Level.FINE, "Unexpected response body: " + rawBody);
        return e;
    }

}
