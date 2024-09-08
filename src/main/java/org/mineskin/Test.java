package org.mineskin;

import org.mineskin.data.Skin;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.response.MineSkinResponse;

import java.util.UUID;
import java.util.concurrent.CompletionException;

public class Test {

    private static final MineSkinClient CLIENT = MineSkinClient.builder()
            .userAgent("aabss/v1.0")
            .build();

    public static void main(String[] args) {
        GenerateOptions options = GenerateOptions.create()
                .name("My Skin")
                .visibility(Visibility.PUBLIC);
        CLIENT.generateUser(UUID.fromString("d8351dca-c109-4ad4-b7ac-77fdd02234e0"), options)
                .thenAccept(response -> {
                    Skin skin = response.getSkin();
                    System.out.println(skin);
                })
                .exceptionally(throwable -> {
                    if (throwable instanceof CompletionException completionException) {
                        throwable = completionException.getCause();
                    }
                    if (throwable instanceof MineSkinRequestException requestException) {
                        MineSkinResponse<?> response = requestException.getResponse();
                        System.out.println(response.getMessageOrError());
                    }
                    return null;
                });

    }
}
