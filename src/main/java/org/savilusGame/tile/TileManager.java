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
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import org.savilusGame.GamePanel;

import lombok.Getter;

@Getter
public class TileManager {
  private static final Color CHASE_PATH_COLOR = new Color(255, 0, 0, 70);

  private final GamePanel gamePanel;
  private final Tile[] tile;
  private final Map<String, int[][]> gameMaps = new ConcurrentHashMap<>();
  private volatile boolean pathVisible;

  public TileManager(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    tile = new Tile[50];

    getTileImage();
    loadMap(INTERIOR_MAP);
    loadMap(MAIN_MAP);
    loadMap(DUNGEON_FIRST_FLOR);
    loadMap(DUNGEON_SECOND_FLOR);
  }


  /** Publish the entire layout at once. A dev reload also changes the identity used by the
   * navigation and map-image caches; a failed reload never publishes a partial map. */
  public void loadMap(String mapPath) {
    int columns = gamePanel.getMaxWorldCol();
    int rows = gamePanel.getMaxWorldRow();
    int[][] layout = new int[columns][rows];
    var stream = getClass().getResourceAsStream(mapPath);
    if (stream == null) throw new IllegalArgumentException("Missing tile map: " + mapPath);
    try (var reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      for (int row = 0; row < rows; row++) {
        String line = reader.readLine();
        String[] numbers = line == null ? new String[0] : line.strip().split("\\s+");
        if (numbers.length != columns) throw new IllegalStateException("Invalid row " + row + " in " + mapPath);
        for (int column = 0; column < columns; column++) {
          int number = Integer.parseInt(numbers[column]);
          if (number < 0 || number >= tile.length || tile[number] == null) {
            throw new IllegalStateException("Unknown tile " + number + " at " + column + "," + row + " in " + mapPath);
          }
          layout[column][row] = number;
        }
      }
    } catch (IOException | NumberFormatException error) {
      throw new IllegalStateException("Cannot load tile map: " + mapPath, error);
    }
    gameMaps.put(mapPath, layout);
  }

  public void draw(Graphics2D graphics) {
    int[][] map = gameMaps.get(gamePanel.getWorld().getCurrentMap());
    int cameraX = gamePanel.cameraX();
    int cameraY = gamePanel.cameraY();
    int firstColumn = Math.max(0, Math.floorDiv(cameraX, TILE_SIZE));
    int firstRow = Math.max(0, Math.floorDiv(cameraY, TILE_SIZE));
    int endColumn = Math.min(map.length, Math.ceilDiv(cameraX + gamePanel.getScreenWidth(), TILE_SIZE));
    int endRow = Math.min(map[0].length, Math.ceilDiv(cameraY + gamePanel.getScreenHeight(), TILE_SIZE));
    for (int row = firstRow; row < endRow; row++) {
      for (int column = firstColumn; column < endColumn; column++) {
        graphics.drawImage(tile[map[column][row]].image(), column * TILE_SIZE - cameraX, row * TILE_SIZE - cameraY, null);
      }
    }
    if (pathVisible) drawChasePaths(graphics, cameraX, cameraY);
  }

  public void togglePathVisibility() {
    pathVisible = !pathVisible;
  }

  private void drawChasePaths(Graphics2D graphics, int cameraX, int cameraY) {
    Color original = graphics.getColor();
    graphics.setColor(CHASE_PATH_COLOR);
    for (var monster : gamePanel.getWorld().currentMonsters()) {
      if (monster == null) continue;
      if (monster.isPursuingDirectly()) {
        var player = gamePanel.getPlayer();
        graphics.drawLine(monster.getScreenX() + (int) monster.getSolidArea().getCenterX(),
            monster.getScreenY() + (int) monster.getSolidArea().getCenterY(),
            player.getScreenX() + (int) player.getSolidArea().getCenterX(),
            player.getScreenY() + (int) player.getSolidArea().getCenterY());
      }
      for (var cell : monster.remainingPath()) {
        graphics.fillRect(cell.column() * TILE_SIZE - cameraX, cell.row() * TILE_SIZE - cameraY, TILE_SIZE, TILE_SIZE);
      }
    }
    graphics.setColor(original);
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
    tile[index] = new Tile(gamePanel.getSpriteCache().get(imageName, TILE_SIZE, TILE_SIZE), collision);
  }
}
