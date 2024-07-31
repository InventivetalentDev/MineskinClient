package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MineSkinResponse<T> {

    private final boolean success;
    private final int status;

    private final String message;
    private final String error;
    private final List<String> warnings;

    private final String server;
    private final String breadcrumb;

    private final JsonObject rawBody;
    private final T body;

    public MineSkinResponse(
            int status,
            Map<String, String> headers,
            JsonObject rawBody,
            Gson gson, Class<T> clazz
    ) {
        if (rawBody.has("success")) {
            this.success = rawBody.get("success").getAsBoolean();
        } else {
            this.success = status == 200;
        }
        this.status = status;
        this.message = rawBody.has("message") ? rawBody.get("message").getAsString() : null;
        this.error = rawBody.has("error") ? rawBody.get("error").getAsString() : null;
        this.warnings = rawBody.has("warnings") ? gson.fromJson(rawBody.get("warnings"), List.class) : Collections.emptyList();


        this.server = headers.get("x-mineskin-server");
        this.breadcrumb = headers.get("x-mineskin-breadcrumb");

        this.rawBody = rawBody;
        this.body = gson.fromJson(rawBody, clazz);
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

    public List<String> getWarnings() {
        return warnings;
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
