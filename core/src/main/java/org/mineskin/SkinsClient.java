package org.mineskin;

import org.mineskin.response.SkinResponse;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface SkinsClient {

    /**
     * Get an existing skin by UUID (Note: not a player UUID)
     * @see <a href="https://docs.mineskin.org/docs/mineskin-api/get-a-skin-by-uuid">Get a skin by UUID</a>
     */
    CompletableFuture<SkinResponse> get(UUID uuid);

    /**
     * Get an existing skin by UUID (Note: not a player UUID)
     * @see <a href="https://docs.mineskin.org/docs/mineskin-api/get-a-skin-by-uuid">Get a skin by UUID</a>
     */
    CompletableFuture<SkinResponse> get(String uuid);

}
