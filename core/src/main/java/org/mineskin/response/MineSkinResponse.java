package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.CodeAndMessage;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MineSkinResponse<T> {

    private final boolean success;
    private final int status;

    private final List<CodeAndMessage> messages;
    private final List<CodeAndMessage> errors;
    private final List<CodeAndMessage> warnings;

    private final String server;
    private final String breadcrumb;

    private final JsonObject rawBody;
    private final T body;

    public MineSkinResponse(
            int status,
            Map<String, String> headers,
            JsonObject rawBody,
            Gson gson,
            String primaryField, Class<T> clazz
    ) {
        if (rawBody.has("success")) {
            this.success = rawBody.get("success").getAsBoolean();
        } else {
            this.success = status == 200;
        }
        this.status = status;
        this.messages = rawBody.has("messages") ? gson.fromJson(rawBody.get("messages"), CodeAndMessage.LIST_TYPE_TOKEN.getType()) : Collections.emptyList();
        this.warnings = rawBody.has("warnings") ? gson.fromJson(rawBody.get("warnings"), CodeAndMessage.LIST_TYPE_TOKEN.getType()) : Collections.emptyList();
        this.errors = rawBody.has("errors") ? gson.fromJson(rawBody.get("errors"), CodeAndMessage.LIST_TYPE_TOKEN.getType()) : Collections.emptyList();

        this.server = headers.get("mineskin-server");
        this.breadcrumb = headers.get("mineskin-breadcrumb");

        this.rawBody = rawBody;
        this.body = parseBody(rawBody, gson, primaryField, clazz);
    }

    protected T parseBody(JsonObject rawBody, Gson gson, String primaryField, Class<T> clazz) {
        return gson.fromJson(rawBody.get(primaryField), clazz);
    }

    public boolean isSuccess() {
        return success;
    }

    public int getStatus() {
        return status;
    }

    public List<CodeAndMessage> getMessages() {
        return messages;
    }

    public List<CodeAndMessage> getErrors() {
        return errors;
    }

    public List<CodeAndMessage> getWarnings() {
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
