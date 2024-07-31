package test;

import org.junit.Before;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mineskin.ApacheRequestHandler;
import org.mineskin.GenerateOptions;
import org.mineskin.ImageUtil;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.GeneratedSkin;
import org.mineskin.data.Skin;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.response.GenerateResponse;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class GenerateTest {

    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    private static final MineSkinClient APACHE = MineSkinClient.builder()
            .requestHandler(ApacheRequestHandler::new)
            .userAgent("MineSkinClient-Apache/Tests")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .generateExecutor(EXECUTOR)
            .build();
    private static final MineSkinClient JSOUP = MineSkinClient.builder()
            .requestHandler(JsoupRequestHandler::new)
            .userAgent("MineSkinClient-Jsoup/Tests")
            .apiKey(System.getenv("MINESKIN_API_KEY"))
            .generateExecutor(EXECUTOR)
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

    private static Stream<Arguments> clients() {
        return Stream.of(
                Arguments.of(APACHE),
                Arguments.of(JSOUP)
        );
    }

    @ParameterizedTest
    @MethodSource("clients")
    public void urlTest(MineSkinClient client) throws InterruptedException {
        Thread.sleep(1000);

        final String name = "JavaClient-Url";
        try {
            GenerateResponse res = client.generateUrl("https://i.imgur.com/jkhZKDX.png", GenerateOptions.create().name(name)).join();
            System.out.println(res);
            Skin skin = res.getSkin();
            validateSkin(skin, name);
        } catch (CompletionException e) {
            if (e.getCause() instanceof MineSkinRequestException req) {
                System.out.println(req.getResponse());
            }
            throw e;
        }
        Thread.sleep(1000);
    }

    @ParameterizedTest
    @MethodSource("clients")
    public void uploadTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        final String name = "JavaClient-Upload";
        File file = File.createTempFile("mineskin-temp-upload-image", ".png");
        ImageIO.write(ImageUtil.randomImage(64, 32), "png", file);
        System.out.println("#uploadTest");
        long start = System.currentTimeMillis();
        try {
            GenerateResponse res = client.generateUpload(file, GenerateOptions.create().visibility(Visibility.UNLISTED).name(name)).join();
            System.out.println("Upload took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(res);
            Skin skin = res.getSkin();
            validateSkin(skin, name);
        } catch (CompletionException e) {
            if (e.getCause() instanceof MineSkinRequestException req) {
                System.out.println(req.getResponse());
            }
            throw e;
        }
        Thread.sleep(1000);
    }

    @ParameterizedTest
    @MethodSource("clients")
    public void uploadRenderedImageTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        final String name = "JavaClient-Upload";
        System.out.println("#uploadRenderedImageTest");
        long start = System.currentTimeMillis();
        try {
            GenerateResponse res = client.generateUpload(ImageUtil.randomImage(64, 32), GenerateOptions.create().visibility(Visibility.UNLISTED).name(name)).join();
            System.out.println("Upload took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(res);
            Skin skin = res.getSkin();
            validateSkin(skin, name);
        } catch (CompletionException e) {
            if (e.getCause() instanceof MineSkinRequestException req) {
                System.out.println(req.getResponse());
            }
            throw e;
        }
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
