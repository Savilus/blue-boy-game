package org.savilusGame.tile;

import static org.savilusGame.GamePanel.TILE_SIZE;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameMap {

  static int MINIMAP_DIAMETER = 200;
  static String CLOSE_MAP_KEY = "closeMapText";
  static String UI_MESSAGES = "uiMessages";

  GamePanel gamePanel;
  TileManager tileManager;
  Map<String, BufferedImage> worldGameMap = new HashMap<>();
  @NonFinal boolean miniMapOn = false;

  public GameMap(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    this.tileManager = gamePanel.getTileManager();
    createWorldMap();
  }

  public void createWorldMap() {
    int cols = gamePanel.getMaxWorldCol();
    int rows = gamePanel.getMaxWorldRow();

    int width = TILE_SIZE * cols;
    int height = TILE_SIZE * rows;

    var worldMapImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    var graphics2D = worldMapImage.createGraphics();

    int[][] tileMap = tileManager.getGameMaps().get(gamePanel.getWorld().getCurrentMap());

    for (int row = 0; row < rows; row++) {
      for (int col = 0; col < cols; col++) {
        int tileNum = tileMap[col][row];
        var tile = tileManager.getTile()[tileNum];
        if (Objects.nonNull(tile)) {
          int x = TILE_SIZE * col;
          int y = TILE_SIZE * row;
          graphics2D.drawImage(tile.image(), x, y, null);
        }
      }
    }

    graphics2D.dispose();
    worldGameMap.put(gamePanel.getWorld().getCurrentMap(), worldMapImage);
  }


  public void drawFullMapScreen(Graphics2D g2) {
    int size = 500;
    int x = gamePanel.getScreenWidth() / 2 - size / 2;
    int y = gamePanel.getScreenHeight() / 2 - size / 2;
    drawMap(g2, x, y, size, size, false, true, 1F);

    g2.setFont(gamePanel.getUi().getMaruMonica().deriveFont(32F));
    g2.setColor(Color.WHITE);
    g2.drawString(TextManager.getUiText(UI_MESSAGES, CLOSE_MAP_KEY), 750, 550);
  }


  public void drawMiniMap(Graphics2D g2) {
    if (!miniMapOn) return;
    int x = gamePanel.getScreenWidth() - MINIMAP_DIAMETER - 15;
    int y = 15;
    drawMap(g2, x, y, MINIMAP_DIAMETER, MINIMAP_DIAMETER, true, false, 0.8F);
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
    g2.drawImage(worldGameMap.get(gamePanel.getWorld().getCurrentMap()), x, y, width, height, null);

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
