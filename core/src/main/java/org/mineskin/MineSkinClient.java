package org.mineskin;

public interface MineSkinClient extends QueueClient, SkinClient {

    static ClientBuilder builder() {
        return ClientBuilder.create();
    }

}
