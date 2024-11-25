package org.mineskin.response;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.mineskin.MineSkinClient;
import org.mineskin.data.JobInfo;
import org.mineskin.data.SkinInfo;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class JobResponse extends MineSkinResponse<JobInfo> {

    private SkinInfo skin;

    public JobResponse(int status, Map<String, String> headers, JsonObject rawBody, Gson gson, Class<JobInfo> clazz) {
        super(status, headers, rawBody, gson, "job", clazz);
        this.skin = gson.fromJson(rawBody.get("skin"), SkinInfo.class);
    }

    public JobInfo getJob() {
        return getBody();
    }

    public Optional<SkinInfo> getSkin() {
        return Optional.ofNullable(skin);
    }

    public CompletableFuture<SkinInfo> getOrLoadSkin(MineSkinClient client) {
        if (this.skin != null) {
            return CompletableFuture.completedFuture(this.skin);
        } else {
            return getJob().getSkin(client).thenApply(skin -> {
                this.skin = skin.getSkin();
                return this.skin;
            });
        }
    }

}
