package org.mineskin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.RequestHandler;
import org.mineskin.response.MineSkinResponse;
import org.mineskin.response.ResponseConstructor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    private <T, R extends MineSkinResponse<T>> R wrapResponse(HttpResponse response, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        String rawBody = null;
        try {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))) {
                rawBody = reader.lines().collect(Collectors.joining("\n"));
            }

            JsonObject jsonBody = gson.fromJson(rawBody, JsonObject.class);
            R wrapped = constructor.construct(
                    response.getStatusLine().getStatusCode(),
                    lowercaseHeaders(response.getAllHeaders()),
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

    private Map<String, String> lowercaseHeaders(Header[] headers) {
        return Stream.of(headers)
                .collect(Collectors.toMap(
                        header -> header.getName().toLowerCase(),
                        Header::getValue
                ));
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R getJson(String url, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        MineSkinClientImpl.LOGGER.fine("GET " + url);
        HttpResponse response = this.httpClient.execute(new HttpGet(url));
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        MineSkinClientImpl.LOGGER.fine("POST " + url);
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        StringEntity entity = new StringEntity(gson.toJson(data), ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        HttpResponse response = this.httpClient.execute(post);
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url, String key, String filename, InputStream in, Map<String, String> data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        MineSkinClientImpl.LOGGER.fine("POST " + url);
        HttpPost post = new HttpPost(url);
        MultipartEntityBuilder multipart = MultipartEntityBuilder.create()
                .setBoundary("mineskin-" + System.currentTimeMillis())
                .addBinaryBody(key, in, ContentType.IMAGE_PNG, filename);
        for (Map.Entry<String, String> entry : data.entrySet()) {
            multipart.addTextBody(entry.getKey(), entry.getValue());
        }
        HttpEntity entity = multipart.build();
        post.setEntity(entity);
        HttpResponse response = this.httpClient.execute(post);
        return wrapResponse(response, clazz, constructor);
    }

}
