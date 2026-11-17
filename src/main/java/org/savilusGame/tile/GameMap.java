package org.savilusGame.tile;

import static org.savilusGame.GamePanel.TILE_SIZE;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.WeakHashMap;
import java.util.Map;

import org.savilusGame.GamePanel;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameMap {

  static int MINIMAP_DIAMETER = 200;
  static String CLOSE_MAP_KEY = "closeMapText";
  static String UI_MESSAGES = "uiMessages";

  GamePanel gamePanel;
  TileManager tileManager;
  // Each loaded layout has one image. A replaced layout (dev reload) can be collected.
  Map<int[][], BufferedImage> mapImages = new WeakHashMap<>();
  @Getter @Setter @NonFinal boolean miniMapOn = false;

  public GameMap(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    this.tileManager = gamePanel.getTileManager();
  }

  public void drawFullMapScreen(Graphics2D g2) {
    int size = 500;
    int x = gamePanel.getScreenWidth() / 2 - size / 2;
    int y = gamePanel.getScreenHeight() / 2 - size / 2;
    drawMap(g2, x, y, size, size, false, true, 1F);

    g2.setFont(gamePanel.getUi().getFonts().getLabel());
    g2.setColor(Color.WHITE);
    g2.drawString(TextManager.getUiText(UI_MESSAGES, CLOSE_MAP_KEY), 750, 550);
  }


  public void drawMiniMap(Graphics2D g2) {
    if (!miniMapOn) return;
    int x = gamePanel.getScreenWidth() - MINIMAP_DIAMETER - 15;
    int y = 15;
    drawMap(g2, x, y, MINIMAP_DIAMETER, MINIMAP_DIAMETER, true, false, 0.8F);
  }


  BufferedImage currentMapImage() {
    int[][] layout = tileManager.getGameMaps().get(gamePanel.getWorld().getCurrentMap());
    BufferedImage image = mapImages.get(layout);
    if (image != null) return image;

    image = new BufferedImage(layout.length * TILE_SIZE, layout[0].length * TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
    var graphics = image.createGraphics();
    try {
      for (int column = 0; column < layout.length; column++) {
        for (int row = 0; row < layout[column].length; row++) {
          graphics.drawImage(tileManager.getTile()[layout[column][row]].image(), column * TILE_SIZE, row * TILE_SIZE, null);
        }
      }
    } finally {
      graphics.dispose();
    }
    mapImages.put(layout, image);
    return image;
  }

  private void drawMap(Graphics2D g2, int x, int y, int width, int height, boolean isCircle, boolean withBackground, float alpha) {
    if (withBackground) {
      g2.setColor(Color.BLACK);
      g2.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
    }

    Shape originalClip = g2.getClip();
    if (isCircle) {
      Ellipse2D circle = new Ellipse2D.Float(x, y, width, height);
      g2.setClip(circle);
    }

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    g2.drawImage(currentMapImage(), x, y, width, height, null);

    double scale = (double) (TILE_SIZE * gamePanel.getMaxWorldCol()) / width;
    int playerX = (int) (x + gamePanel.getPlayer().getWorldX() / scale);
    int playerY = (int) (y + gamePanel.getPlayer().getWorldY() / scale);
    int playerSize = TILE_SIZE / 3;
    g2.drawImage(gamePanel.getPlayer().getDown1(), playerX - 6, playerY - 6, playerSize, playerSize, null);

    g2.setClip(originalClip);
    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1F));

    if (isCircle) {
      g2.setColor(Color.WHITE);
      g2.setStroke(new BasicStroke(3));
      g2.draw(new Ellipse2D.Float(x, y, width, height));
    }
  }
}
