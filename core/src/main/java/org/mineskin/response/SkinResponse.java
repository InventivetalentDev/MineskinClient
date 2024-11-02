package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.SkinInfo;

import java.util.Map;

public class SkinResponse extends MineSkinResponse<SkinInfo> {

    public SkinResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson) {
        super(status, headers, rawBody, gson, "skin", SkinInfo.class);
    }

}
