package org.mineskin;

import com.google.gson.JsonObject;

import java.io.InputStream;
import java.util.Map;

public abstract class RequestHandler {

    public abstract <T> T getJson(String url, Class<T> clazz) throws Exception;

    public abstract <T> T postJson(String url, JsonObject data, Class<T> clazz) throws Exception;

    public abstract <T> T postFormDataFile(String url,
                                                              String key, String filename, InputStream in,
                                                              Map<String, String> data,
                                                              Class<T> clazz) throws Exception;

}
