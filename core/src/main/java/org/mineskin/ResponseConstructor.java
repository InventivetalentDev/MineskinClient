package org.mineskin;

import com.google.gson.JsonObject;
import org.mineskin.response.MineSkinResponse;

import java.util.Map;

public interface ResponseConstructor<T, R extends MineSkinResponse<T>> {
    R construct(int status,
                Map<String, String> headers,
                JsonObject rawBody,
                T body);
}
