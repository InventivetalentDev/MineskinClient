package org.mineskin;

import org.mineskin.data.Skin;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.response.MineSkinResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.concurrent.CompletionException;

public class Example {

    private static final MineSkinClient CLIENT = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MyMineSkinApp/v1.0")
            .apiKey("your-api-key")
            .build();

    public static void main(String[] args) throws FileNotFoundException {
        GenerateOptions options = GenerateOptions.create()
                .name("My Skin")
                .visibility(Visibility.PUBLIC);
        File file = new File("skin.png");
        CLIENT.generateUpload(file, options)
                .thenAccept(response -> {
                    // get generated skin
                    Skin skin = response.getSkin();
                    System.out.println(skin);
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    if (throwable instanceof CompletionException completionException) {
                        throwable = completionException.getCause();
                    }

                    if (throwable instanceof MineSkinRequestException requestException) {
                        // get error details
                        MineSkinResponse response = requestException.getResponse();
                        System.out.println(response.getMessageOrError());
                    }
                    return null;
                });

        CLIENT.getSkinByUuid("skinuuid")
                .thenAccept(response -> {
                    // get existing skin
                    Skin skin = response.getSkin();
                    System.out.println(skin);
                });
    }

}
