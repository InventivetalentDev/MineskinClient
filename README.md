# MineskinClient

Client for [api.mineskin.org](https://mineskin.org)

Can be used to generate valid texture data from skin image files.  
You can also use [mineskin.org](https://mineskin.org) to directly generate skin data from images.

The API requires official Minecraft accounts to upload the skin textures.  
If you own a Minecraft account you don't actively use and want to contibute to the API's speed,
please [add your account here](https://mineskin.org/account)!

```java
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
        File file = new File("skin.jpg");
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
```  


```xml
<depencies>
    <dependency>
        <groupId>org.mineskin</groupId>
        <artifactId>java-client</artifactId>
        <version>2.1.1-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>org.mineskin</groupId>
        <artifactId>java-client-jsoup</artifactId>
        <version>2.1.1-SNAPSHOT</version>
    </dependency>
<!-- alternatively use apache httpcommons -->
<!--    <dependency>-->
<!--        <groupId>org.mineskin</groupId>-->
<!--        <artifactId>java-client-apache</artifactId>-->
<!--        <version>2.1.1-SNAPSHOT</version>-->
<!--    </dependency>-->
<!-- ... or java 11 HttpRequest -->
<!--    <dependency>-->
<!--        <groupId>org.mineskin</groupId>-->
<!--        <artifactId>java-client-java11</artifactId>-->
<!--        <version>2.1.1-SNAPSHOT</version>-->
<!--    </dependency>-->
</depencies>
```
```xml
<repositories>
    <repository>
        <id>inventive-repo</id>
        <url>https://repo.inventivetalent.org/repository/public/</url>
    </repository>
</repositories>
```
