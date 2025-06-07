# MineskinClient

Client for [api.mineskin.org](https://mineskin.org)

Can be used to generate valid texture data from skin image files.  
You can also use [mineskin.org](https://mineskin.org) to directly generate skin data from images.

**Important:** This is a client for [MineSkin V2](https://docs.mineskin.org/docs/guides/migrating-to-v2/) - for V1 see the [2.x branch](https://github.com/InventivetalentDev/MineskinClient/tree/2.x) and use versions < 3.0.0. 

The API requires official Minecraft accounts to upload the skin textures.  
If you own a Minecraft account you don't actively use and want to contibute to the API's speed,
please [add your account here](https://account.mineskin.org)!

```java
public class Example {

    private static final MineSkinClient CLIENT = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MyMineSkinApp/v1.0") // TODO: update this with your own user agent
            .apiKey("your-api-key") // TODO: update this with your own API key (https://account.mineskin.org/keys)
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
```  


```xml

<dependencies>
    <dependency>
        <groupId>org.mineskin</groupId>
        <artifactId>java-client</artifactId>
        <version>3.0.6-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.mineskin</groupId>
        <artifactId>java-client-jsoup</artifactId>
        <version>3.0.6-SNAPSHOT</version>
    </dependency>
    <!-- alternatively use apache httpcommons -->
    <!--    <dependency>-->
    <!--        <groupId>org.mineskin</groupId>-->
    <!--        <artifactId>java-client-apache</artifactId>-->
    <!--        <version>3.0.6-SNAPSHOT</version>-->
    <!--    </dependency>-->
    <!-- ... or java 11 HttpRequest -->
    <!--    <dependency>-->
    <!--        <groupId>org.mineskin</groupId>-->
    <!--        <artifactId>java-client-java11</artifactId>-->
    <!--        <version>3.0.6-SNAPSHOT</version>-->
    <!--    </dependency>-->
</dependencies>
```
```xml
<repositories>
    <repository>
        <id>inventive-repo</id>
        <url>https://repo.inventivetalent.org/repository/public/</url>
    </repository>
</repositories>
```
