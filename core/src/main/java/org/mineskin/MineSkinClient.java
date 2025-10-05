package org.mineskin;

import java.util.logging.Logger;

public interface MineSkinClient {

    static ClientBuilder builder() {
        return ClientBuilder.create();
    }

    /**
     * Get the queue client
     */
    QueueClient queue();

    /**
     * Get the generate client
     */
    GenerateClient generate();

    /**
     * Get the skins client
     */
    SkinsClient skins();

    /**
     * Get the client for miscellaneous endpoints
     */
    MiscClient misc();

    Logger getLogger();

}
