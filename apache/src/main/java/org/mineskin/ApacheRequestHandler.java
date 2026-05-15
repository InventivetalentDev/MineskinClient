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
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicHeader;
import org.mineskin.data.CodeAndMessage;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.exception.MineskinException;
import org.mineskin.request.RequestHandler;
import org.mineskin.request.RequestHandlerConstructor;
import org.mineskin.request.RequestInterceptor;
import org.mineskin.request.ResponseInterceptor;
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
    private final List<RequestInterceptor<HttpUriRequest>> requestInterceptors;
    private final List<ResponseInterceptor<HttpResponse>> responseInterceptors;

    public ApacheRequestHandler(
            String baseUrl,
            String userAgent, String apiKey,
            int timeout,
            Gson gson) {
        this(baseUrl, userAgent, apiKey, timeout, gson, List.of(), List.of());
    }

    private ApacheRequestHandler(
            String baseUrl,
            String userAgent, String apiKey,
            int timeout,
            Gson gson,
            List<RequestInterceptor<HttpUriRequest>> requestInterceptors,
            List<ResponseInterceptor<HttpResponse>> responseInterceptors) {
        super(baseUrl, userAgent, apiKey, timeout, gson);
        this.gson = gson;
        this.requestInterceptors = requestInterceptors;
        this.responseInterceptors = responseInterceptors;

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

    /**
     * Start building an {@link ApacheRequestHandler} with the given request interceptor.
     * The returned {@link Builder} implements {@link RequestHandlerConstructor} and can be passed
     * directly to {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static Builder withRequestInterceptor(RequestInterceptor<HttpUriRequest> interceptor) {
        return new Builder().withRequestInterceptor(interceptor);
    }

    /**
     * Start building an {@link ApacheRequestHandler} with the given response interceptor.
     * <p>
     * Note: reading the entity body via {@code response.getEntity().getContent()} inside an
     * interceptor will consume the stream and break internal JSON parsing. Status code and
     * headers are always safe to inspect.
     */
    public static Builder withResponseInterceptor(ResponseInterceptor<HttpResponse> interceptor) {
        return new Builder().withResponseInterceptor(interceptor);
    }

    private HttpResponse execute(HttpUriRequest request) throws IOException {
        for (RequestInterceptor<HttpUriRequest> interceptor : requestInterceptors) {
            interceptor.intercept(request);
        }
        return this.httpClient.execute(request);
    }

    private <T, R extends MineSkinResponse<T>> R wrapResponse(HttpResponse response, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        for (ResponseInterceptor<HttpResponse> interceptor : responseInterceptors) {
            interceptor.intercept(response);
        }
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
        url = this.baseUrl + url;
        MineSkinClientImpl.LOGGER.fine("GET " + url);
        HttpResponse response = execute(new HttpGet(url));
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postJson(String url, JsonObject data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        url = this.baseUrl + url;
        MineSkinClientImpl.LOGGER.fine("POST " + url);
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", ContentType.APPLICATION_JSON.getMimeType());
        StringEntity entity = new StringEntity(gson.toJson(data), ContentType.APPLICATION_JSON);
        post.setEntity(entity);
        HttpResponse response = execute(post);
        return wrapResponse(response, clazz, constructor);
    }

    @Override
    public <T, R extends MineSkinResponse<T>> R postFormDataFile(String url, String key, String filename, InputStream in, Map<String, String> data, Class<T> clazz, ResponseConstructor<T, R> constructor) throws IOException {
        url = this.baseUrl + url;
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
        HttpResponse response = execute(post);
        return wrapResponse(response, clazz, constructor);
    }

    /**
     * Builder for an {@link ApacheRequestHandler} configured with request and/or response interceptors.
     * Implements {@link RequestHandlerConstructor} so it can be passed directly to
     * {@link ClientBuilder#requestHandler(RequestHandlerConstructor)}.
     */
    public static final class Builder implements RequestHandlerConstructor {

        private final List<RequestInterceptor<HttpUriRequest>> requestInterceptors = new ArrayList<>();
        private final List<ResponseInterceptor<HttpResponse>> responseInterceptors = new ArrayList<>();

        private Builder() {
        }

        public Builder withRequestInterceptor(RequestInterceptor<HttpUriRequest> interceptor) {
            this.requestInterceptors.add(interceptor);
            return this;
        }

        public Builder withResponseInterceptor(ResponseInterceptor<HttpResponse> interceptor) {
            this.responseInterceptors.add(interceptor);
            return this;
        }

        @Override
        public RequestHandler construct(String baseUrl, String userAgent, String apiKey, int timeout, Gson gson) {
            return new ApacheRequestHandler(baseUrl, userAgent, apiKey, timeout, gson,
                    List.copyOf(requestInterceptors), List.copyOf(responseInterceptors));
        }
    }

}
