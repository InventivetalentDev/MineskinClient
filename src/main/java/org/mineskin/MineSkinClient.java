package org.mineskin;

import org.mineskin.response.GenerateResponse;
import org.mineskin.response.GetSkinResponse;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface MineSkinClient {

    static ClientBuilder builder() {
        return ClientBuilder.create();
    }

    /**
     * @return the next request timestamp in milliseconds
     */
    long getNextRequest();

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    CompletableFuture<GetSkinResponse> getSkinByUuid(UUID uuid);

    /**
     * Get an existing skin by UUID (Note: not the player's UUID)
     */
    CompletableFuture<GetSkinResponse> getSkinByUuid(String uuid);

    /**
     * Generates skin data from an URL
     */
    CompletableFuture<GenerateResponse> generateUrl(String url);

    /**
     * Generates skin data from an URL with custom options
     */
    CompletableFuture<GenerateResponse> generateUrl(String url, GenerateOptions options);

    /**
     * Generates skin data by uploading an image (with default options)
     */
    CompletableFuture<GenerateResponse> generateUpload(InputStream is);

    /**
     * Generates skin data by uploading an image with custom options
     */
    CompletableFuture<GenerateResponse> generateUpload(InputStream is, GenerateOptions options);

    /**
     * Uploads and generates skin data by uploading an image (with default options)
     */
    CompletableFuture<GenerateResponse> generateUpload(InputStream is, String fileName);

    /**
     * Uploads and generates skin data by uploading an image with custom options
     */
    CompletableFuture<GenerateResponse> generateUpload(InputStream is, GenerateOptions options, String fileName);

    /**
     * Uploads and generates skin data from a local file (with default options)
     */
    CompletableFuture<GenerateResponse> generateUpload(File file) throws FileNotFoundException;

    /**
     * Uploads and generates skin data from a local file with custom options
     */
    CompletableFuture<GenerateResponse> generateUpload(File file, GenerateOptions options) throws FileNotFoundException;

    /**
     * Uploads and generates skin data from a RenderedImage object (with default options)
     */
    CompletableFuture<GenerateResponse> generateUpload(RenderedImage image) throws IOException;

    /**
     * Uploads and generates skin data from a RenderedImage object with custom options
     */
    CompletableFuture<GenerateResponse> generateUpload(RenderedImage image, GenerateOptions options) throws IOException;

    /**
     * Loads skin data from an existing player
     */
    CompletableFuture<GenerateResponse> generateUser(UUID uuid);


    /**
     * Loads skin data from an existing player with custom options
     */
    CompletableFuture<GenerateResponse> generateUser(UUID uuid, GenerateOptions options);

}
