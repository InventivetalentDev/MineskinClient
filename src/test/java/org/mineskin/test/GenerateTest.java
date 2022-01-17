package org.mineskin.test;

import org.junit.Test;
import org.mineskin.MineskinClient;
import org.mineskin.SkinOptions;
import org.mineskin.data.Skin;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GenerateTest {

	private final MineskinClient client = new MineskinClient("MineskinJavaClient-Test");

	@Test(timeout = 40000L)
	public void urlTest() throws InterruptedException {
		Thread.sleep(7000);

		final String name = "JavaClient-Url";
		Skin skin = this.client.generateUrl("https://i.imgur.com/jkhZKDX.png", SkinOptions.name(name)).join();
		validateSkin(skin, name);
		Thread.sleep(1000);
	}

	@Test(timeout = 40000L)
	public void uploadTest() throws InterruptedException, IOException {
		Thread.sleep(7000);

		final String name = "JavaClient-Upload";
		File file = File.createTempFile("mineskin-temp-upload-image", ".png");
		ImageIO.write(randomImage(64, 32), "png", file);
		Skin skin = this.client.generateUpload(file, SkinOptions.name(name)).join();
		validateSkin(skin, name);
		Thread.sleep(1000);
	}

	@Test(timeout = 40000L)
	public void uploadRenderedImageTest() throws InterruptedException, IOException {
		Thread.sleep(7000);

		final String name = "JavaClient-Upload";
		Skin skin = this.client.generateUpload(randomImage(64, 32), SkinOptions.name(name)).join();
		validateSkin(skin, name);
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
