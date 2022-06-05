# MineskinClient

Client for [api.mineskin.org](https://mineskin.org)

Can be used to generate valid texture data from skin image files.  
You can also use [mineskin.org](https://mineskin.org) to directly generate skin data from images.

The API requires official Minecraft accounts to upload the skin textures.  
If you own a Minecraft account you don't actively use and want to contibute to the API's speed,
please [add your account here]([https://inventivetalent.org/contact](https://mineskin.org/account))!

```java
MineSkinClient client = new MineSkinClient("MyUserAgent");
client.generateUrl("https://image.url", SkinOptions.name("some cool skin")).thenAccept(skin -> {
    ...
})
```  


```xml
<dependency>
    <groupId>org.mineskin</groupId>
    <artifactId>java-client</artifactId>
    <version>1.2.0-SNAPSHOT</version>
</dependency>
```
```xml
<repositories>
    <repository>
        <id>inventive-repo</id>
        <url>https://repo.inventivetalent.org/repository/public/</url>
    </repository>
</repositories>
```
