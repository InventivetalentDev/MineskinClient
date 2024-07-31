package org.mineskin;

import com.google.gson.Gson;

public interface RequestHandlerConstructor {
    RequestHandler construct(String userAgent, String apiKey, int timeout, Gson gson);
}
