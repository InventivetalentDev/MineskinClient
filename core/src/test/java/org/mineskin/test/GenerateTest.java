package org.mineskin.test;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class GenerateTest {

//    private final MineSkinClient client = new MineSkinClient(Executors.newFixedThreadPool(2), "MineskinJavaClient-Test","3373adbd32c5621ab2175e6ec637f6a2754639f00903dbbc46870c87c826efa6");
//
//    @Before
//    public void before() throws InterruptedException {
//        Thread.sleep(5000);
//    }
//
//    @Test(timeout = 40_000L)
//    public void urlTest() throws InterruptedException {
//        Thread.sleep(1000);
//
//        final String name = "JavaClient-Url";
//        System.out.println("#urlTest");
//        Skin skin = this.client.generateUrl("https://i.imgur.com/jkhZKDX.png", GenerateOptions.name(name)).join();
//        validateSkin(skin, name);
//        Thread.sleep(1000);
//    }
//
//    @Test(timeout = 40_000L)
//    public void uploadTest() throws InterruptedException, IOException {
//        Thread.sleep(1000);
//
//        final String name = "JavaClient-Upload";
//        File file = File.createTempFile("mineskin-temp-upload-image", ".png");
//        ImageIO.write(randomImage(64, 32), "png", file);
//        System.out.println("#uploadTest");
//        long start = System.currentTimeMillis();
//        Skin skin = this.client.generateUpload(file, GenerateOptions.name(name)).join();
//        System.out.println("Upload took " + (System.currentTimeMillis() - start) + "ms");
//        validateSkin(skin, name);
//        Thread.sleep(1000);
//    }
//
//    @Test(timeout = 40_000L)
//    public void uploadRenderedImageTest() throws InterruptedException, IOException {
//        Thread.sleep(1000);
//
//        final String name = "JavaClient-Upload";
//        System.out.println("#uploadRenderedImageTest");
//        long start = System.currentTimeMillis();
//        Skin skin = this.client.generateUpload(randomImage(64, 32), GenerateOptions.name(name)).join();
//        System.out.println("Upload took " + (System.currentTimeMillis() - start) + "ms");
//        validateSkin(skin, name);
//        Thread.sleep(1000);
//    }
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

//    void validateSkin(Skin skin, String name) {
//        assertNotNull(skin);
//        assertNotNull(skin.data);
//        assertNotNull(skin.data.texture);
//
//        assertEquals(name, skin.name);
//    }

    BufferedImage randomImage(int width, int height) {
        Random random = new Random();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                float r = random.nextFloat();
                float g = random.nextFloat();
                float b = random.nextFloat();
                image.setRGB(x, y, new Color(r, g, b).getRGB());
            }
        }
        return image;
    }

}
