package org.mineskin;

import org.mineskin.data.CodeAndMessage;
import org.mineskin.data.JobInfo;
import org.mineskin.data.Skin;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.MineSkinResponse;

import java.io.File;
import java.util.Optional;
import java.util.concurrent.CompletionException;

public class Example {

    private static final MineSkinClient CLIENT = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MyMineSkinApp/v1.0") // TODO: update this with your own user agent
            .apiKey("your-api-key") // TODO: update this with your own API key (https://account.mineskin.org/keys)
            /*
             Uncomment this if you're on a paid plan with higher concurrency limits
            .generateQueueOptions(new QueueOptions(Executors.newSingleThreadScheduledExecutor(), 200, 5concurrency))
             */
            .build();

    public static void main(String[] args) {
        File file = new File("skin.png");
        GenerateRequest request = GenerateRequest.upload(file)
                .name("My Skin")
                .visibility(Visibility.PUBLIC);
        // submit queue request
        CLIENT.queue().submit(request)
                .thenCompose(queueResponse -> {
                    JobInfo job = queueResponse.getJob();
                    // wait for job completion
                    return job.waitForCompletion(CLIENT);
                })
                .thenCompose(jobResponse -> {
                    // get skin from job or load it from the API
                    return jobResponse.getOrLoadSkin(CLIENT);
                })
                .thenAccept(skinInfo -> {
                    // do stuff with the skin
                    System.out.println(skinInfo);
                    System.out.println(skinInfo.texture().data().value());
                    System.out.println(skinInfo.texture().data().signature());
                })
                .exceptionally(throwable -> {
                    throwable.printStackTrace();
                    if (throwable instanceof CompletionException completionException) {
                        throwable = completionException.getCause();
                    }

                    if (throwable instanceof MineSkinRequestException requestException) {
                        // get error details
                        MineSkinResponse<?> response = requestException.getResponse();
                        Optional<CodeAndMessage> detailsOptional = response.getErrorOrMessage();
                        detailsOptional.ifPresent(details -> {
                            System.out.println(details.code() + ": " + details.message());
                        });
                    }
                    return null;
                });

        CLIENT.skins().get("skinuuid")
                .thenAccept(response -> {
                    // get existing skin
                    Skin skin = response.getSkin();
                    System.out.println(skin);
                });
    }

}
