package org.mineskin;

public interface MineSkinClient extends LegacyClient, QueueClient {

    static ClientBuilder builder() {
        return ClientBuilder.create();
    }


}
