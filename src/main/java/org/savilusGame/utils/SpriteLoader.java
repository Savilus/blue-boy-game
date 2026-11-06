package org.savilusGame.utils;

import java.awt.image.BufferedImage;
import java.util.Objects;

import javax.imageio.ImageIO;


import io.vavr.control.Try;
import lombok.experimental.UtilityClass;

/** Loads and scales game sprites from the classpath. */
@UtilityClass
public class SpriteLoader {

  /**
   * Load the image at {@code imagePath} (classpath resource), scaled to {@code width} x {@code height}.
   *
   * @throws RuntimeException when the image cannot be read (fail fast, as before)
   */
  public static BufferedImage load(String imagePath, int width, int height) {
    return Try.of(() -> ImageIO.read(Objects.requireNonNull(SpriteLoader.class.getResourceAsStream(imagePath))))
        .map(image -> UtilityTool.scaleImage(image, width, height))
        .getOrElseThrow(() -> new RuntimeException("Failed to load image: " + imagePath));
  }
}
