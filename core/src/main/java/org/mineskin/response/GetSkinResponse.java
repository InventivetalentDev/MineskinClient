package org.mineskin.response;

import com.google.gson.JsonObject;
import org.mineskin.data.Skin;

import java.util.Map;

public class GetSkinResponse extends MineSkinResponse<Skin> {
    public GetSkinResponse(int status, Map<String, String> headers, JsonObject rawBody, Skin body) {
        super(status, headers, rawBody, body);
    }
}
