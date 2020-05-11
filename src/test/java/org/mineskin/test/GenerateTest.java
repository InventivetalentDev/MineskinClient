package org.mineskin.test;

import org.junit.Test;
import org.mineskin.MineskinClient;
import org.mineskin.SkinOptions;
import org.mineskin.data.Skin;
import org.mineskin.data.SkinCallback;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class GenerateTest {

	private final MineskinClient client = new MineskinClient();

	@Test(timeout = 90000L)
	public void urlTest() throws InterruptedException {
		Thread.sleep(7000);

		CountDownLatch latch = new CountDownLatch(1);
		final String name = "JavaClient-Test-Url";
		this.client.generateUrl("https://i.imgur.com/0Fna2GH.png", SkinOptions.name(name), new SkinCallback() {

			@Override
			public void exception(Exception exception) {
				fail(exception.getMessage());
				latch.countDown();
			}

			@Override
			public void error(String errorMessage) {
				fail(errorMessage);
				latch.countDown();
			}

			@Override
			public void waiting(long delay) {
				System.out.println("Waiting " + delay);
			}

			@Override
			public void done(Skin skin) {
				validateSkin(skin, name);

				latch.countDown();
			}
		});
		latch.await(10000, TimeUnit.SECONDS);
		Thread.sleep(1000);
	}

	@Test(timeout = 90000L)
	public void uploadTest() throws InterruptedException, IOException {
		Thread.sleep(7000);

		CountDownLatch latch = new CountDownLatch(1);
		final String name = "JavaClient-Test-Upload-" + System.currentTimeMillis();
		File file = File.createTempFile("mineskin-temp-upload-image", ".png");
		ImageIO.write(randomImage(64, 32), "png", file);
		this.client.generateUpload(file, SkinOptions.name(name), new SkinCallback() {

			@Override
			public void exception(Exception exception) {
				fail(exception.getMessage());
				latch.countDown();
			}

			@Override
			public void error(String errorMessage) {
				fail(errorMessage);
				latch.countDown();
			}

			@Override
			public void waiting(long delay) {
				System.out.println("Waiting " + delay);
			}

			@Override
			public void done(Skin skin) {
				validateSkin(skin, name);

				latch.countDown();
			}
		});
		latch.await(10000, TimeUnit.SECONDS);
		Thread.sleep(1000);
	}

	void validateSkin(Skin skin, String name) {
		assertNotNull(skin);
		assertNotNull(skin.data);
		assertNotNull(skin.data.texture);

		assertEquals(name, skin.name);
	}

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
