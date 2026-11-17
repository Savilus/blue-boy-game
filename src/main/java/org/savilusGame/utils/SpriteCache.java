package org.savilusGame.utils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Objects;

import javax.imageio.ImageIO;

/** Session-owned LRU of scaled classpath sprites. Images are shared read-only: animation swaps
 * references, never paints into the sprite. Both input (equipment) and the game loop may load. */
public final class SpriteCache {
  private static final int DEFAULT_CAPACITY = 256;

  private final LinkedHashMap<Key, BufferedImage> images = new LinkedHashMap<>(16, 0.75f, true);
  private final int capacity;

  public SpriteCache() {
    this(DEFAULT_CAPACITY);
  }

  SpriteCache(int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("Non-positive sprite cache capacity: " + capacity);
    this.capacity = capacity;
  }

  public synchronized BufferedImage get(String path, int width, int height) {
    if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid sprite dimensions: " + width + "x" + height);
    var key = new Key(Objects.requireNonNull(path), width, height);
    BufferedImage image = images.get(key);
    if (image == null) {
      image = load(key);
      if (images.size() == capacity) images.pollFirstEntry();
      images.put(key, image);
    }
    return image;
  }

  private static BufferedImage load(Key key) {
    try (var stream = SpriteCache.class.getResourceAsStream(key.path())) {
      if (stream == null) throw new IllegalStateException("Missing sprite: " + key.path());
      var original = ImageIO.read(stream);
      if (original == null) throw new IllegalStateException("Not an image: " + key.path());
      return UtilityTool.scaleImage(original, key.width(), key.height());
    } catch (IOException error) {
      throw new IllegalStateException("Cannot read sprite: " + key.path(), error);
    }
  }

  private record Key(String path, int width, int height) {}
}
