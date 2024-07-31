package test;

import org.junit.Before;
import org.junit.Test;
import org.mineskin.GenerateOptions;
import org.mineskin.ImageUtil;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.GeneratedSkin;
import org.mineskin.data.Skin;
import org.mineskin.data.Visibility;
import org.mineskin.response.GenerateResponse;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;

import static org.junit.Assert.*;

public class GenerateTest {

    private static final MineSkinClient CLIENT = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MineSkinClient/Tests")
            .build();

    static {
        MineSkinClientImpl.LOGGER.setLevel(Level.ALL);
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        MineSkinClientImpl.LOGGER.addHandler(handler);
    }

    @Before
    public void before() throws InterruptedException {
        Thread.sleep(1000);
    }

    @Test(timeout = 40_000L)
    public void urlTest() throws InterruptedException {
        Thread.sleep(1000);

        final String name = "JavaClient-Url";
        GenerateResponse res = CLIENT.generateUrl("https://i.imgur.com/jkhZKDX.png", GenerateOptions.create().name(name)).join();
        System.out.println(res);
        Skin skin = res.getSkin();
        validateSkin(skin, name);
        Thread.sleep(1000);
    }

    @Test(timeout = 40_000L)
    public void uploadTest() throws InterruptedException, IOException {
        Thread.sleep(1000);

        final String name = "JavaClient-Upload";
        File file = File.createTempFile("mineskin-temp-upload-image", ".png");
        ImageIO.write(ImageUtil.randomImage(64, 32), "png", file);
        System.out.println("#uploadTest");
        long start = System.currentTimeMillis();
        GenerateResponse res = CLIENT.generateUpload(file, GenerateOptions.create().visibility(Visibility.UNLISTED).name(name)).join();
        System.out.println("Upload took " + (System.currentTimeMillis() - start) + "ms");
        System.out.println(res);
        Skin skin = res.getSkin();
        validateSkin(skin, name);
        Thread.sleep(1000);
    }

    @Test(timeout = 40_000L)
    public void uploadRenderedImageTest() throws InterruptedException, IOException {
        Thread.sleep(1000);

        final String name = "JavaClient-Upload";
        System.out.println("#uploadRenderedImageTest");
        long start = System.currentTimeMillis();
        GenerateResponse res =  CLIENT.generateUpload(ImageUtil.randomImage(64, 32), GenerateOptions.create().visibility(Visibility.UNLISTED).name(name)).join();
        System.out.println("Upload took " + (System.currentTimeMillis() - start) + "ms");
        System.out.println(res);
        Skin skin = res.getSkin();
        validateSkin(skin, name);
        Thread.sleep(1000);
    }
//
//    @Test()
//    public void multiUploadTest() throws InterruptedException, IOException {
//        for (int i = 0; i < 50; i++) {
//            try {
//                uploadTest();
//                uploadRenderedImageTest();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

    void validateSkin(Skin skin, String name) {
        assertNotNull(skin);
        assertTrue(skin instanceof GeneratedSkin);
        assertNotNull(skin.data());
        assertNotNull(skin.data().texture());

        assertEquals(name, skin.name());
    }

}
