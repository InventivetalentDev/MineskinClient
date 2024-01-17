# MineskinClient

Client for [api.mineskin.org](https://mineskin.org)

Can be used to generate valid texture data from skin image files.  
You can also use [mineskin.org](https://mineskin.org) to directly generate skin data from images.

The API requires official Minecraft accounts to upload the skin textures.  
If you own a Minecraft account you don't actively use and want to contibute to the API's speed,
please [add your account here](https://mineskin.org/account)!

```java
MineskinClient client = new MineskinClient("MyUserAgent");
client.generateUrl("https://image.url", SkinOptions.name("some cool skin")).thenAccept(skin -> {
    ...
})
client.getId(1337l).thenAccept(skin -> {
    System.out.println(skin.data.texture.value);
    System.out.println(skin.data.texture.signature);
});
```  

Maven:
```xml
<dependency>
    <groupId>org.mineskin</groupId>
    <artifactId>java-client</artifactId>
    <version>1.2.4-SNAPSHOT</version>
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
Gradle:
```gradle
repositories {
    maven {
        name = "inventive-repo"
        url = "https://repo.inventivetalent.org/repository/public/"
    }
}

dependencies {
    implementation 'org.mineskin:java-client:1.2.4-SNAPSHOT'
}
```
