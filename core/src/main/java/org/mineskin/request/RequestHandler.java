package org.mineskin.request;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.response.ResponseConstructor;
import org.mineskin.response.MineSkinResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class RequestHandler {

    protected final Gson gson;

    public RequestHandler(String userAgent, String apiKey, int timeout, Gson gson) {
        this.gson = gson;
    }

    public abstract <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    public abstract <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    public abstract <T, R extends MineSkinResponse<T>> R postFormDataFile(String url,
                                                                          String key, String filename, InputStream in,
                                                                          Map<String, String> data,
                                                                          Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

}
