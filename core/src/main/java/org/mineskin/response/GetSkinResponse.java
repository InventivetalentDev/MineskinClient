package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.data.ExistingSkin;
import org.mineskin.data.Skin;

import java.util.Map;

@Deprecated
public class GetSkinResponse extends MineSkinResponse<ExistingSkin> {

    public GetSkinResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson, Class<ExistingSkin> clazz) {
        super(status, headers, rawBody, gson, clazz);
    }

    public Skin getSkin() {
        return getBody();
    }

}
