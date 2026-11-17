package org.savilusGame.tile;

import static org.junit.jupiter.api.Assertions.*;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;

class TileRenderingTest {
  private final GamePanel panel = new GamePanel();
  private final TileManager tiles = panel.getTileManager();

  @Test
  void cameraVisitsOnlyTheWindowIncludingBothPartiallyVisibleEdges() {
    int cameraX = 12 * TILE_SIZE + 1;
    int cameraY = 16 * TILE_SIZE - 1;
    var map = tiles.getGameMaps().get(MAIN_MAP);
    // Accessing even a single cell of this off-screen column would throw.
    map[40] = null;
    BufferedImage image = drawAt(cameraX, cameraY);
    for (int x : new int[]{0, TILE_SIZE - 1, image.getWidth() - 1}) {
      for (int y : new int[]{0, 1, image.getHeight() - 1}) {
        int worldX = x + cameraX;
        int worldY = y + cameraY;
        var tile = tiles.getTile()[map[worldX / TILE_SIZE][worldY / TILE_SIZE]];
        assertEquals(tile.image().getRGB(worldX % TILE_SIZE, worldY % TILE_SIZE), image.getRGB(x, y));
      }
    }
  }

  @Test
  void windowClampsAtBothMapBoundaries() {
    BufferedImage topLeft = drawAt(-1, -1);
    assertEquals(0, topLeft.getRGB(0, 0));
    int[][] map = tiles.getGameMaps().get(MAIN_MAP);
    assertEquals(tiles.getTile()[map[0][0]].image().getRGB(0, 0), topLeft.getRGB(1, 1));
    int lastColumn = map.length - 1;
    int lastRow = map[0].length - 1;
    BufferedImage bottomRight = drawAt(lastColumn * TILE_SIZE, lastRow * TILE_SIZE);
    assertEquals(0, bottomRight.getRGB(TILE_SIZE, TILE_SIZE));
    assertEquals(tiles.getTile()[map[lastColumn][lastRow]].image().getRGB(0, 0), bottomRight.getRGB(0, 0));
  }

  @Test
  void minimapAndFullMapReuseOneImagePerLayoutButDoNotReuseAReloadedOne() {
    GameMap maps = panel.getGameMap();
    BufferedImage outside = maps.currentMapImage();
    panel.getWorld().switchMap(INTERIOR_MAP);
    BufferedImage inside = maps.currentMapImage();
    assertNotSame(outside, inside);
    panel.getWorld().switchMap(MAIN_MAP);
    assertSame(outside, maps.currentMapImage());
    tiles.loadMap(MAIN_MAP);
    assertNotSame(outside, maps.currentMapImage());
    panel.getWorld().switchMap(INTERIOR_MAP);
    assertSame(inside, maps.currentMapImage());
  }

  private BufferedImage drawAt(int cameraX, int cameraY) {
    var player = panel.getPlayer();
    player.setWorldPosition(cameraX + player.getScreenX(), cameraY + player.getScreenY());
    var image = new BufferedImage(panel.getScreenWidth(), panel.getScreenHeight(), BufferedImage.TYPE_INT_ARGB);
    var graphics = image.createGraphics();
    try {
      tiles.draw(graphics);
    } finally {
      graphics.dispose();
    }
    return image;
  }
}
