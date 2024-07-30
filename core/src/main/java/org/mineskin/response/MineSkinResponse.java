package org.mineskin.response;

import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Optional;

public class MineSkinResponse<T> {

    private final boolean success;
    private final int status;

    private final String message;
    private final String error;

    private final String server;
    private final String breadcrumb;

    private final JsonObject rawBody;
    private final T body;

    public MineSkinResponse(
            boolean success,
            int status,
            String message,
            String error,
            String server,
            String breadcrumb,
            JsonObject rawBody,
            T body) {
        this.success = success;
        this.status = status;
        this.message = message;
        this.error = error;
        this.server = server;
        this.breadcrumb = breadcrumb;
        this.rawBody = rawBody;
        this.body = body;
    }

    public MineSkinResponse(
            int status,
            Map<String, String> headers,
            JsonObject rawBody,
            T body
    ) {
        if (rawBody.has("success")) {
            this.success = rawBody.get("success").getAsBoolean();
        } else {
            this.success = status == 200;
        }
        this.status = status;
        this.message = rawBody.has("message") ? rawBody.get("message").getAsString() : null;
        this.error = rawBody.has("error") ? rawBody.get("error").getAsString() : null;


        this.server = headers.get("x-mineskin-server");
        this.breadcrumb = headers.get("x-mineskin-breadcrumb");

        this.rawBody = rawBody;
        this.body = body;
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    public Optional<String> getError() {
        return Optional.ofNullable(error);
    }

    public String getMessageOrError() {
        return success ? message : error;
    }

    public String getServer() {
        return server;
    }

    public String getBreadcrumb() {
        return breadcrumb;
    }

    public T getBody() {
        return body;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()+ "{" +
                "success=" + success +
                ", status=" + status +
                ", server='" + server + '\'' +
                ", breadcrumb='" + breadcrumb + '\'' +
                "}\n" +
                rawBody;
    }
}
