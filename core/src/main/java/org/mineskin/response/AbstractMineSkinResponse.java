package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.CodeAndMessage;

import java.util.*;

public abstract class AbstractMineSkinResponse<T> implements MineSkinResponse<T> {

    private final boolean success;
    private final int status;

    private final List<CodeAndMessage> messages;
    private final List<CodeAndMessage> errors;
    private final List<CodeAndMessage> warnings;

    private final Map<String, String> headers;
    private final String server;
    private final String breadcrumb;

    private final JsonObject rawBody;
    private final T body;

    public AbstractMineSkinResponse(
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

        this.headers = headers.entrySet().stream()
                .filter(e -> e.getKey().startsWith("mineskin-") || e.getKey().startsWith("x-mineskin-"))
                .collect(HashMap::new, (m, e) -> m.put(e.getKey().toLowerCase(), e.getValue()), HashMap::putAll);
        this.server = headers.get("mineskin-server");
        this.breadcrumb = headers.get("mineskin-breadcrumb");

        this.rawBody = rawBody;
        this.body = parseBody(rawBody, gson, primaryField, clazz);
    }

    protected T parseBody(JsonObject rawBody, Gson gson, String primaryField, Class<T> clazz) {
        return gson.fromJson(rawBody.get(primaryField), clazz);
    }

    @Override
    public boolean isSuccess() {
        return success;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public List<CodeAndMessage> getMessages() {
        return messages;
    }

    @Override
    public Optional<CodeAndMessage> getFirstMessage() {
        return messages.stream().findFirst();
    }

    @Override
    public List<CodeAndMessage> getErrors() {
        return errors;
    }

    @Override
    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    @Override
    public Optional<CodeAndMessage> getFirstError() {
        return errors.stream().findFirst();
    }

    @Override
    public Optional<CodeAndMessage> getErrorOrMessage() {
        return getFirstError().or(this::getFirstMessage);
    }

    @Override
    public List<CodeAndMessage> getWarnings() {
        return warnings;
    }

    @Override
    public Optional<CodeAndMessage> getFirstWarning() {
        return warnings.stream().findFirst();
    }

    @Override
    public String getServer() {
        return server;
    }

    @Override
    public String getBreadcrumb() {
        return breadcrumb;
    }

    @Override
    public T getBody() {
        return body;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "success=" + success +
                ", status=" + status +
                ", server='" + server + '\'' +
                ", breadcrumb='" + breadcrumb + '\'' +
                ", headers=" + headers +
                ", messages=" + messages +
                ", errors=" + errors +
                ", warnings=" + warnings +
                "}\n" +
                rawBody;
    }
}
