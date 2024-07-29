package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jsoup.Connection;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.util.Map;

public class JsoupRequestHandler extends RequestHandler {

    private final String userAgent;
    private final String apiKey;
    private final int timeout;

    private final Gson gson;

    public JsoupRequestHandler(
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        this.userAgent = userAgent;
        this.apiKey = apiKey;
        this.timeout = timeout;
        this.gson = gson;
    }

    private Connection requestBase(String url) {
        Connection connection = Jsoup.connect(url)
                .method(Connection.Method.POST)
                .userAgent(userAgent)
                .ignoreContentType(true)
                .ignoreHttpErrors(true)
                .timeout(timeout);
        if (apiKey != null) {
            connection.header("Authorization", "Bearer " + apiKey);
        }
        return connection;
    }

    public <T> T getJson(String url, Class<T> clazz) throws Exception {
        Connection.Response response = requestBase(url).execute();
        return gson.fromJson(response.body(), clazz);
    }

    public <T> T postJson(String url, JsonObject data, Class<T> clazz) throws Exception{
                Connection.Response response = requestBase(url)
                        .requestBody(data.toString())
                        .header("Content-Type", "application/json")
                        .execute();
                return gson.fromJson(response.body(), clazz);
    }

    public <T> T postFormDataFile(String url,
                                                     String key, String filename, InputStream in,
                                                     Map<String, String> data,
                                                     Class<T> clazz) throws Exception {
                Connection connection = requestBase(url);
                connection.data(data);
                connection.data(key, filename, in);
                Connection.Response response = connection.execute();
                return gson.fromJson(response.body(), clazz);
    }

}
