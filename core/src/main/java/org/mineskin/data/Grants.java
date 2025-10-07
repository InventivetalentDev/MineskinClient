package org.mineskin.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Optional;

public class Grants {

    private final JsonObject raw;

    public Grants(JsonObject raw) {
        this.raw = raw;
    }

    public Optional<JsonElement> getRaw(String key) {
        if (raw.has(key) && !raw.get(key).isJsonNull()) {
            return Optional.of(raw.get(key));
        }
        return Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        return getRaw(key).map(JsonElement::getAsBoolean);
    }

    public Optional<Integer> getInt(String key) {
        return getRaw(key).map(JsonElement::getAsInt);
    }

    public Optional<Double> getDouble(String key) {
        return getRaw(key).map(JsonElement::getAsDouble);
    }

    public Optional<String> getString(String key) {
        return getRaw(key).map(JsonElement::getAsString);
    }

    public Optional<Integer> perMinute() {
        return getInt("per_minute");
    }

    public Optional<Integer> concurrency() {
        return getInt("concurrency");
    }

    @Override
    public String toString() {
        return "Grants" + raw;
    }
}
