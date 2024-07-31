package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.DelayInfo;
import org.mineskin.data.Skin;

import java.util.Map;

public class GenerateResponse extends MineSkinResponse<Skin> {

    private final DelayInfo delayInfo;

    public GenerateResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson, Class<Skin> clazz) {
        super(status, headers, rawBody, gson, clazz);
        this.delayInfo = gson.fromJson(rawBody.get("delayInfo"), DelayInfo.class);
    }

    public DelayInfo getDelayInfo() {
        return delayInfo;
    }

}
