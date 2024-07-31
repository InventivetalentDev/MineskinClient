package org.mineskin;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Random;

public class ImageUtil {

    public static BufferedImage randomImage(int width, int height) {
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
