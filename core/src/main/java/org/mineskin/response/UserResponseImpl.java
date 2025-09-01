package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.User;
import org.mineskin.data.UserInfo;

import java.util.Map;

public class UserResponseImpl extends AbstractMineSkinResponse<UserInfo> implements UserResponse {

    public UserResponseImpl(int status, Map<String, String> headers, JsonObject rawBody, Gson gson, Class<UserInfo> clazz) {
        super(status, headers, rawBody, gson, "skin", clazz);
    }

    @Override
    protected UserInfo parseBody(JsonObject rawBody, Gson gson, String primaryField, Class<UserInfo> clazz) {
        return gson.fromJson(rawBody, clazz);
    }

    @Override
    public User getUser() {
        return getBody();
    }
}
