package org.mineskin.request;

import com.google.gson.Gson;

public interface RequestHandlerConstructor {
    RequestHandler construct(String userAgent, String apiKey, int timeout, Gson gson);
}
