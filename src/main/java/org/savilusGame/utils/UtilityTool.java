package org.savilusGame.utils;

import java.awt.*;
import java.awt.image.BufferedImage;

import lombok.experimental.UtilityClass;

@UtilityClass
public class UtilityTool {

  public static BufferedImage scaleImage(BufferedImage originalImage, int width, int height) {

    BufferedImage scaledImage = new BufferedImage(width, height, originalImage.getType());
    Graphics2D g2 = scaledImage.createGraphics();
    try {
      g2.drawImage(originalImage, 0, 0, width, height, null);
      return scaledImage;
    } finally {
      g2.dispose();
    }
  }
}
