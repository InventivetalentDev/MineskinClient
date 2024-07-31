package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.response.MineSkinResponse;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

public abstract class RequestHandler {

    public abstract <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    public abstract <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException;

    public abstract <T, R extends MineSkinResponse<T>> R postFormDataFile(String url,
                                                                          String key, String filename, InputStream in,
                                                                          Map<String, String> data,
                                                                          Class<T> clazz,ResponseConstructor<T, R> constructor) throws IOException;

}