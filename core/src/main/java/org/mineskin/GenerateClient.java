package org.mineskin;

import org.mineskin.request.GenerateRequest;
import org.mineskin.response.GenerateResponse;

import java.util.concurrent.CompletableFuture;

public interface GenerateClient {

    /**
     * Generate a skin
     * @see <a href="https://docs.mineskin.org/docs/mineskin-api/generate-a-skin">Generate a skin</a>
     */
    CompletableFuture<GenerateResponse> submitAndWait(GenerateRequest request);

}
