package org.mineskin;

import org.mineskin.response.UserResponse;

import java.util.concurrent.CompletableFuture;

public interface MiscClient {
    /**
     * Get the current user
     * @see <a href="https://docs.mineskin.org/docs/mineskin-api/get-the-current-user">Get the current user</a>
     */
    CompletableFuture<UserResponse> getUser();
}
