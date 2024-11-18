package org.mineskin;

import org.mineskin.response.SkinResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SkinsClient {

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    CompletableFuture<SkinResponse> get(UUID uuid);

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    CompletableFuture<SkinResponse> get(String uuid);

}
