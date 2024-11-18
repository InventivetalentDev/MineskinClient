package org.mineskin;

public interface MineSkinClient {

    static ClientBuilder builder() {
        return ClientBuilder.create();
    }

    QueueClient queue();

    SkinsClient skins();

}
