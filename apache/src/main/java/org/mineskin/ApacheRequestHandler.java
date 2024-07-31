package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.mineskin.request.RequestHandler;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApacheRequestHandler extends RequestHandler {

    private final Gson gson;

    private final HttpClient httpClient;

    public ApacheRequestHandler(
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        super(userAgent, apiKey, timeout, gson);
        this.gson = gson;

        List<Header> defaultHeaders = new ArrayList<>();
        if (apiKey != null) {
            defaultHeaders.add(new BasicHeader("Authorization", "Bearer " + apiKey));
            defaultHeaders.add(new BasicHeader("Content-Type", "application/json"));
            defaultHeaders.add(new BasicHeader("Accept", "application/json"));
        }
        this.httpClient = HttpClientBuilder.create()
                .setDefaultRequestConfig(RequestConfig.copy(RequestConfig.DEFAULT)
                        .setSocketTimeout(timeout)
                        .setConnectTimeout(timeout)
                        .setConnectionRequestTimeout(timeout)
                        .build())
                .setUserAgent(userAgent)
                .setDefaultHeaders(defaultHeaders)
                .build();
    }

    private <T> T parseResponse(HttpEntity entity, Class<T> clazz) throws Exception {
        try (InputStreamReader reader = new InputStreamReader(entity.getContent())) {
            return gson.fromJson(reader, clazz);
        }
    }

    @Override
    public <T> T getJson(String url, Class<T> clazz) throws Exception {
        HttpEntity entity = this.httpClient
                .execute(new HttpGet(url))
                .getEntity();
        return parseResponse(entity, clazz);
    }

    @Override
    public <T> T postJson(String url, JsonObject data, Class<T> clazz) throws Exception {
        HttpPost post = new HttpPost(url);
        post.setEntity(new StringEntity(gson.toJson(data)));
        HttpEntity entity = this.httpClient
                .execute(new HttpPost(url))
                .getEntity();
        return parseResponse(entity, clazz);
    }

    @Override
    public <T> T postFormDataFile(String url,
                                  String key, String filename, InputStream in,
                                  Map<String, String> data,
                                  Class<T> clazz) throws Exception {
        HttpPost post = new HttpPost(url);
        MultipartEntityBuilder multipart = MultipartEntityBuilder.create()
                .addBinaryBody(key, in, ContentType.IMAGE_PNG, filename);
        for (Map.Entry<String, String> entry : data.entrySet()) {
            multipart.addTextBody(entry.getKey(), entry.getValue());
        }
        post.setEntity(multipart.build());
        HttpEntity entity = this.httpClient
                .execute(post)
                .getEntity();
        return parseResponse(entity, clazz);
    }
}
