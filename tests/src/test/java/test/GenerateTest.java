package test;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mineskin.ApacheRequestHandler;
import org.mineskin.ImageUtil;
import org.mineskin.Java11RequestHandler;
import org.mineskin.JsoupRequestHandler;
import org.mineskin.MineSkinClient;
import org.mineskin.MineSkinClientImpl;
import org.mineskin.data.JobInfo;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinInfo;
import org.mineskin.data.Visibility;
import org.mineskin.exception.MineSkinRequestException;
import org.mineskin.request.GenerateRequest;
import org.mineskin.response.JobResponse;
import org.mineskin.response.QueueResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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
    private static final MineSkinClient JAVA11 = MineSkinClient.builder()
            .requestHandler(Java11RequestHandler::new)
            .userAgent("MineSkinClient-Java11/Tests")
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
                Arguments.of(JSOUP),
                Arguments.of(JAVA11)
        );
    }

//    @ParameterizedTest
//    @MethodSource("clients")
//    public void urlTest(MineSkinClient client) throws InterruptedException {
//        Thread.sleep(1000);
//
//        final String name = "JavaClient-Url";
//        try {
//            GenerateResponse res = client.generateUrl("https://i.imgur.com/jkhZKDX.png", GenerateOptions.create().name(name)).join();
//            System.out.println(res);
//            Skin skin = res.getSkin();
//            validateSkin(skin, name);
//        } catch (CompletionException e) {
//            if (e.getCause() instanceof MineSkinRequestException req) {
//                System.out.println(req.getResponse());
//            }
//            throw e;
//        }
//        Thread.sleep(1000);
//    }

    @ParameterizedTest
    @MethodSource("clients")
    public void singleQueueUploadTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        File file = File.createTempFile("mineskin-temp-upload-image", ".png");
        ImageIO.write(ImageUtil.randomImage(64, ThreadLocalRandom.current().nextBoolean() ? 64 : 32), "png", file);
        System.out.println("#queueTest");
        long start = System.currentTimeMillis();
        try {
            String name = "mskjva-upl-" + ThreadLocalRandom.current().nextInt(1000);
            GenerateRequest request = GenerateRequest.upload(file)
                    .visibility(Visibility.UNLISTED)
                    .name(name);
            QueueResponse res = client.queue().submit(request).join();
            System.out.println("Queue submit took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(res);
            JobResponse jobResponse = res.getBody().waitForCompletion(client).join();
            System.out.println("Job took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(jobResponse);
            SkinInfo skinInfo = jobResponse.getOrLoadSkin(client).join();
            validateSkin(skinInfo, name);
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
    public void singleQueueUrlTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        try {
            String name = "mskjva-url-" + ThreadLocalRandom.current().nextInt(1000);
            GenerateRequest request = GenerateRequest.url("https://api.mineskin.org/random-image?t=" + System.currentTimeMillis())
                    .visibility(Visibility.UNLISTED)
                    .name(name);
            QueueResponse res = client.queue().submit(request).join();
            System.out.println("Queue submit took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(res);
            JobResponse jobResponse = res.getBody().waitForCompletion(client).join();
            System.out.println("Job took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(jobResponse);
            SkinInfo skinInfo = jobResponse.getOrLoadSkin(client).join();
            validateSkin(skinInfo, name);
        } catch (CompletionException e) {
            if (e.getCause() instanceof MineSkinRequestException req) {
                System.out.println(req.getResponse());
            }
            throw e;
        }
        Thread.sleep(1000);
    }


    @Test
    public void multiQueueRenderedUploadTest() throws InterruptedException, IOException {
        MineSkinClient client = JAVA11;
        int count = 5;
        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        Map<String, JobInfo> jobs = new HashMap<>();
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(100);
                String name = "mskjva-upl-" + i + "-" + ThreadLocalRandom.current().nextInt(1000);
                BufferedImage image = ImageUtil.randomImage(64, ThreadLocalRandom.current().nextBoolean() ? 64 : 32);
                GenerateRequest request = GenerateRequest.upload(image)
                        .visibility(Visibility.UNLISTED)
                        .name(name);
                QueueResponse res = client.queue().submit(request).join();
                System.out.println("Queue submit took " + (System.currentTimeMillis() - start) + "ms");
                System.out.println(res);
                jobs.put(name, res.getBody());
            } catch (CompletionException e) {
                if (e.getCause() instanceof MineSkinRequestException req) {
                    System.out.println(req.getResponse());
                }
                throw e;
            }
        }
        int jobsPending = 1;
        while (jobsPending > 0) {
            jobsPending = 0;
            for (JobInfo jobInfo : jobs.values()) {
                JobResponse jobResponse = client.queue().get(jobInfo).join();
                if (jobResponse.getJob().status().isPending()) {
                    jobsPending++;
                }
            }
            System.out.println("Jobs pending: " + jobsPending);
            Thread.sleep(1000);
        }

        for (Map.Entry<String, JobInfo> entry : jobs.entrySet()) {
            String name = entry.getKey();
            JobInfo jobInfo = entry.getValue();
            JobResponse jobResponse = client.queue().get(jobInfo).join();
            assertTrue(jobResponse.getJob().status().isDone());
            assertTrue(jobResponse.getSkin().isPresent());
            System.out.println("Job took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(jobResponse);
            SkinInfo skinInfo = jobResponse.getOrLoadSkin(client).join();
            validateSkin(skinInfo, name);
        }


        Thread.sleep(1000);
    }

    @ParameterizedTest
    @MethodSource("clients")
    public void duplicateQueueUrlTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        long start = System.currentTimeMillis();
        try {
            String name = "mskjva-url";
            GenerateRequest request = GenerateRequest.url("https://i.imgur.com/ZC5PRM4.png")
                    .name(name);
            QueueResponse res = client.queue().submit(request).join();
            System.out.println("Queue submit took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(res);
            JobResponse jobResponse = res.getBody().waitForCompletion(client).join();
            System.out.println("Job took " + (System.currentTimeMillis() - start) + "ms");
            System.out.println(jobResponse);
            SkinInfo skinInfo = jobResponse.getOrLoadSkin(client).join();
            validateSkin(skinInfo, name);
        } catch (CompletionException e) {
            if (e.getCause() instanceof MineSkinRequestException req) {
                System.out.println(req.getResponse());
            }
            throw e;
        }
        Thread.sleep(1000);
    }

    /*
    @ParameterizedTest
    @MethodSource("clients")
    public void uploadTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        final String name = "JavaClient-Upload";
        File file = File.createTempFile("mineskin-temp-upload-image", ".png");
        ImageIO.write(ImageUtil.randomImage(64, ThreadLocalRandom.current().nextBoolean() ? 64 : 32), "png", file);
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

     */

    /*
    @ParameterizedTest
    @MethodSource("clients")
    public void uploadRenderedImageTest(MineSkinClient client) throws InterruptedException, IOException {
        Thread.sleep(1000);

        final String name = "JavaClient-Upload";
        System.out.println("#uploadRenderedImageTest");
        long start = System.currentTimeMillis();
        try {
            GenerateResponse res = client.generateUpload(ImageUtil.randomImage(64, ThreadLocalRandom.current().nextBoolean() ? 64 : 32), GenerateOptions.create().visibility(Visibility.UNLISTED).name(name)).join();
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
*/
    void validateSkin(Skin skin, String name) {
        assertNotNull(skin);
        assertNotNull(skin.texture());
        assertNotNull(skin.texture().data());
        assertNotNull(skin.texture().data().value());
        assertNotNull(skin.texture().data().signature());

        assertEquals(name, skin.name());
    }


}
