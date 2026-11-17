package org.savilusGame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.awt.image.BufferedImage;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;

/**
 * Contract of {@link Player#getImage()}: all eight walking sprites are loaded, sized to the tile
 * and distinct from each other. The duplicate {@code BOY_UP1} assignment removed here was silent
 * because a wrong or missing assignment leaves a field null or repeats another direction's frame.
 */
class PlayerSpriteTest {

  private Player player;

  @BeforeEach
  void setUp() {
    player = new GamePanel().getPlayer();
    player.getImage();
  }

  @Test
  @DisplayName("every walking sprite is loaded and has the tile size")
  void everyWalkingSpriteIsLoaded() {
    List<BufferedImage> sprites = List.of(
        player.getUp1(), player.getUp2(),
        player.getDown1(), player.getDown2(),
        player.getLeft1(), player.getLeft2(),
        player.getRight1(), player.getRight2());

    for (BufferedImage sprite : sprites) {
      assertNotNull(sprite, "a walking sprite is missing");
      assertEquals(GamePanel.TILE_SIZE, sprite.getWidth());
      assertEquals(GamePanel.TILE_SIZE, sprite.getHeight());
    }
  }

  @Test
  @DisplayName("each direction and frame uses its own image")
  void eachDirectionAndFrameUsesItsOwnImage() {
    List<Long> checksums = List.of(
        checksum(player.getUp1()), checksum(player.getUp2()),
        checksum(player.getDown1()), checksum(player.getDown2()),
        checksum(player.getLeft1()), checksum(player.getLeft2()),
        checksum(player.getRight1()), checksum(player.getRight2()));

    assertEquals(8L, checksums.stream().distinct().count(),
        "two walking sprites point at the same image, so one direction is rendered as another");
  }

  private static long checksum(BufferedImage image) {
    long sum = 17;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        sum = sum * 31 + image.getRGB(x, y);
      }
    }
    return sum;
  }
}
