package org.savilusGame.world;

import static org.savilusGame.GamePanel.TILE_SIZE;

import java.util.BitSet;
import java.util.List;

import org.savilusGame.ai.NavigationGrid;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.tile.Tile;

/** Active map's passability. Terrain reloads publish a new layout, and placement, destruction
 * and save restoration publish a new obstacle list. Neither is scanned on ordinary chase ticks. */
final class WorldNavigation {
  private int[][] layout;
  private BitSet terrainWalls;
  private List<InteractiveTile> obstacles;
  private NavigationGrid grid;

  NavigationGrid grid(int[][] terrain, Tile[] tiles, List<InteractiveTile> interactiveTiles) {
    if (layout == terrain && obstacles == interactiveTiles) return grid;
    int columns = terrain.length;
    int rows = terrain[0].length;
    if (layout != terrain) {
      terrainWalls = new BitSet(columns * rows);
      for (int column = 0; column < columns; column++) {
        for (int row = 0; row < rows; row++) {
          if (tiles[terrain[column][row]].collision()) terrainWalls.set(row * columns + column);
        }
      }
      layout = terrain;
    }
    var blocked = (BitSet) terrainWalls.clone();
    for (InteractiveTile tile : interactiveTiles) {
      if (tile == null || !tile.isDestructible()) continue;
      int column = Math.floorDiv(tile.getWorldX(), TILE_SIZE);
      int row = Math.floorDiv(tile.getWorldY(), TILE_SIZE);
      if (column >= 0 && column < columns && row >= 0 && row < rows) blocked.set(row * columns + column);
    }
    obstacles = interactiveTiles;
    grid = (column, row) -> column >= 0 && column < columns && row >= 0 && row < rows
        && !blocked.get(row * columns + column);
    return grid;
  }
}
