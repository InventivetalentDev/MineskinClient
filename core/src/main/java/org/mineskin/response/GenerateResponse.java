package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.DelayInfo;
import org.mineskin.data.GeneratedSkin;
import org.mineskin.data.Skin;

import java.util.Map;

public class GenerateResponse extends MineSkinResponse<GeneratedSkin> {

    private final DelayInfo delayInfo;

    public GenerateResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson, Class<GeneratedSkin> clazz) {
        super(status, headers, rawBody, gson, clazz);
        this.delayInfo = gson.fromJson(rawBody.get("delayInfo"), DelayInfo.class);
    }

    public DelayInfo getDelayInfo() {
        return delayInfo;
    }

    public Skin getSkin() {
        return getBody();
    }

}
