package org.mineskin;

import org.mineskin.response.SkinResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SkinClient {

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    CompletableFuture<SkinResponse> getSkin(UUID uuid);

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    CompletableFuture<SkinResponse> getSkin(String uuid);

}
