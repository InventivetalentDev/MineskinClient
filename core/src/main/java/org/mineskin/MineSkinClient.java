package org.mineskin;

public interface MineSkinClient {

    static ClientBuilder builder() {
        return ClientBuilder.create();
    }

    /**
     * Get the queue client
     */
    QueueClient queue();

    /**
     * Get the skins client
     */
    SkinsClient skins();

}
