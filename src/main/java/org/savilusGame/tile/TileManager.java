package org.savilusGame.tile;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BLACK_TAIL;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_FIRST_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.EARTH;
import static org.savilusGame.config.GameEntityNameFactory.FLOOR;
import static org.savilusGame.config.GameEntityNameFactory.GRASS0;
import static org.savilusGame.config.GameEntityNameFactory.GRASS1;
import static org.savilusGame.config.GameEntityNameFactory.HUT;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;
import static org.savilusGame.config.GameEntityNameFactory.ROAD0;
import static org.savilusGame.config.GameEntityNameFactory.ROAD1;
import static org.savilusGame.config.GameEntityNameFactory.ROAD10;
import static org.savilusGame.config.GameEntityNameFactory.ROAD11;
import static org.savilusGame.config.GameEntityNameFactory.ROAD12;
import static org.savilusGame.config.GameEntityNameFactory.ROAD2;
import static org.savilusGame.config.GameEntityNameFactory.ROAD3;
import static org.savilusGame.config.GameEntityNameFactory.ROAD4;
import static org.savilusGame.config.GameEntityNameFactory.ROAD5;
import static org.savilusGame.config.GameEntityNameFactory.ROAD6;
import static org.savilusGame.config.GameEntityNameFactory.ROAD7;
import static org.savilusGame.config.GameEntityNameFactory.ROAD8;
import static org.savilusGame.config.GameEntityNameFactory.ROAD9;
import static org.savilusGame.config.GameEntityNameFactory.STAIRS_DOWN;
import static org.savilusGame.config.GameEntityNameFactory.STAIRS_UP;
import static org.savilusGame.config.GameEntityNameFactory.TABLE;
import static org.savilusGame.config.GameEntityNameFactory.TREE;
import static org.savilusGame.config.GameEntityNameFactory.WALL;
import static org.savilusGame.config.GameEntityNameFactory.WATER0;
import static org.savilusGame.config.GameEntityNameFactory.WATER1;
import static org.savilusGame.config.GameEntityNameFactory.WATER10;
import static org.savilusGame.config.GameEntityNameFactory.WATER11;
import static org.savilusGame.config.GameEntityNameFactory.WATER12;
import static org.savilusGame.config.GameEntityNameFactory.WATER13;
import static org.savilusGame.config.GameEntityNameFactory.WATER2;
import static org.savilusGame.config.GameEntityNameFactory.WATER3;
import static org.savilusGame.config.GameEntityNameFactory.WATER4;
import static org.savilusGame.config.GameEntityNameFactory.WATER5;
import static org.savilusGame.config.GameEntityNameFactory.WATER6;
import static org.savilusGame.config.GameEntityNameFactory.WATER7;
import static org.savilusGame.config.GameEntityNameFactory.WATER8;
import static org.savilusGame.config.GameEntityNameFactory.WATER9;

import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.imageio.ImageIO;

import org.savilusGame.GamePanel;
import org.savilusGame.utils.UtilityTool;

import io.vavr.control.Try;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class TileManager {
  private final GamePanel gamePanel;
  private final Tile[] tile;
  private final Map<String, int[][]> gameMaps = new HashMap<>();
  boolean drawPath = true;

  public TileManager(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    tile = new Tile[50];

    getTileImage();
    loadMap(INTERIOR_MAP);
    loadMap(MAIN_MAP);
    loadMap(DUNGEON_FIRST_FLOR);
    loadMap(DUNGEON_SECOND_FLOR);
  }


  public void loadMap(String mapPath) {
    if (gameMaps.containsKey(mapPath)) {
      return;
    }
    gameMaps.put(mapPath, new int[gamePanel.getMaxWorldCol()][gamePanel.getMaxWorldRow()]);

    Try.withResources(() -> getClass().getResourceAsStream(mapPath))
        .of(is -> {
          try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            for (int row = 0; row < gamePanel.getMaxWorldRow(); row++) {
              String line = br.readLine();
              if (Objects.isNull(line)) break;

              String[] numbers = line.trim().split("\\s+");
              for (int col = 0; col < Math.min(numbers.length, gamePanel.getMaxWorldCol()); col++) {
                gameMaps.get(mapPath)[col][row] = Integer.parseInt(numbers[col]);
              }
            }
          }
          return mapPath;
        })
        .onFailure(error -> log.error("Error while loading tile map '{}': {}", mapPath, error.getMessage()));
  }

  public void draw(Graphics2D g2) {
    int worldCols = gamePanel.getMaxWorldCol();
    int worldRows = gamePanel.getMaxWorldRow();
    int[][] map = gameMaps.get(gamePanel.getWorld().getCurrentMap());

    int playerWorldX = gamePanel.getPlayer().getWorldX();
    int playerWorldY = gamePanel.getPlayer().getWorldY();
    int playerScreenX = gamePanel.getPlayer().getScreenX();
    int playerScreenY = gamePanel.getPlayer().getScreenY();

    int visibleMinX = playerWorldX - playerScreenX - TILE_SIZE;
    int visibleMaxX = playerWorldX + playerScreenX + TILE_SIZE;
    int visibleMinY = playerWorldY - playerScreenY - TILE_SIZE;
    int visibleMaxY = playerWorldY + playerScreenY + TILE_SIZE;

    for (int row = 0; row < worldRows; row++) {
      for (int col = 0; col < worldCols; col++) {
        int tileNum = map[col][row];

        int worldX = col * TILE_SIZE;
        int worldY = row * TILE_SIZE;

        if (worldX >= visibleMinX && worldX <= visibleMaxX &&
            worldY >= visibleMinY && worldY <= visibleMaxY) {

          int screenX = worldX - playerWorldX + playerScreenX;
          int screenY = worldY - playerWorldY + playerScreenY;

          if (Objects.nonNull(tile[tileNum])) {
            g2.drawImage(tile[tileNum].image(), screenX, screenY, null);
          }
        }
      }
    }

// ENABLE IF YOU WANT TO SEE A* ALGORITHM PATH FOR NPC/MONSTER
//    if (drawPath) {
//      g2.setColor(new Color(255, 0, 0, 70));
//      for (var pathNode : gamePanel.pathFinder.getPathList()) {
//        int worldX = pathNode.getCol() * TILE_SIZE;
//        int worldY = pathNode.getRow() * TILE_SIZE;
//        int screenX = worldX - playerWorldX + playerScreenX;
//        int screenY = worldY - playerWorldY + playerScreenY;
//
//        g2.fillRect(screenX, screenY, TILE_SIZE, TILE_SIZE);
//      }
//    }
  }


  private void getTileImage() {
    setUp(49, BLACK_TAIL, true);
    setUp(10, GRASS0, false);
    setUp(11, GRASS1, false);
    setUp(12, WATER0, true);
    setUp(13, WATER1, true);
    setUp(14, WATER2, true);
    setUp(15, WATER3, true);
    setUp(16, WATER4, true);
    setUp(17, WATER5, true);
    setUp(18, WATER6, true);
    setUp(19, WATER7, true);
    setUp(20, WATER8, true);
    setUp(21, WATER9, true);
    setUp(22, WATER10, true);
    setUp(23, WATER11, true);
    setUp(24, WATER12, true);
    setUp(25, WATER13, true);
    setUp(26, ROAD0, false);
    setUp(27, ROAD1, false);
    setUp(28, ROAD2, false);
    setUp(29, ROAD3, false);
    setUp(30, ROAD4, false);
    setUp(31, ROAD5, false);
    setUp(32, ROAD6, false);
    setUp(33, ROAD7, false);
    setUp(34, ROAD8, false);
    setUp(35, ROAD9, false);
    setUp(36, ROAD10, false);
    setUp(37, ROAD11, false);
    setUp(38, ROAD12, false);
    setUp(39, EARTH, false);
    setUp(40, WALL, true);
    setUp(41, TREE, true);
    setUp(42, HUT, false);
    setUp(43, FLOOR, false);
    setUp(44, TABLE, true);
    setUp(45, STAIRS_DOWN, false);
    setUp(46, STAIRS_UP, false);
  }

  private void setUp(int index, String imageName, boolean collision) {
    if (index <= 9) {
      return;
    }
    Try.run(() -> {
      tile[index] = new Tile(
          UtilityTool.scaleImage(ImageIO.read(Objects.requireNonNull(getClass().getResourceAsStream(imageName))),
              TILE_SIZE, TILE_SIZE), collision
      );
    }).onFailure(error -> log.error("Error while setting up tile: {}", error.getMessage()));
  }
}
