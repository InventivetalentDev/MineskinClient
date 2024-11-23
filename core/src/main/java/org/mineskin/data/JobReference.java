package org.mineskin.data;

import org.mineskin.MineSkinClient;
import org.mineskin.response.SkinResponse;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface JobReference {
    JobInfo getJob();

    Optional<SkinInfo> getSkin();

    default CompletableFuture<SkinInfo> getOrLoadSkin(MineSkinClient client) {
        if (this.getSkin().isPresent()) {
            return CompletableFuture.completedFuture(this.getSkin().get());
        } else {
            return getJob().getSkin(client).thenApply(SkinResponse::getSkin);
        }
    }
}
