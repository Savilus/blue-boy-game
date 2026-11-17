package org.savilusGame.world;

import static org.junit.jupiter.api.Assertions.*;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.interactiveTile.DestructibleWall;
import org.savilusGame.entity.interactiveTile.Trunk;

class WorldNavigationTest {
  @Test
  void mapObstaclesAndReloadsPublishNewSnapshotsWithoutChangingOldRoutes() {
    var panel = new GamePanel();
    var world = panel.getWorld();
    var base = world.navigationGrid();
    int column = panel.getPlayer().getWorldX() / GamePanel.TILE_SIZE;
    int row = panel.getPlayer().getWorldY() / GamePanel.TILE_SIZE;
    assertTrue(base.isWalkable(column, row));
    assertFalse(base.isWalkable(-1, row));
    assertFalse(base.isWalkable(column, panel.getMaxWorldRow()));
    assertSame(base, world.navigationGrid());

    var wall = new DestructibleWall(panel, column, row);
    world.setInteractiveTiles(Map.of(MAIN_MAP, List.of(wall, wall)));
    var blocked = world.navigationGrid();
    assertFalse(blocked.isWalkable(column, row));
    world.replaceInteractiveTile(MAIN_MAP, 0, null);
    assertFalse(world.navigationGrid().isWalkable(column, row), "the second wall is still there");
    world.replaceInteractiveTile(MAIN_MAP, 1, new Trunk(panel, column, row));
    var cleared = world.navigationGrid();
    assertTrue(cleared.isWalkable(column, row));
    assertFalse(blocked.isWalkable(column, row));
    assertThrows(UnsupportedOperationException.class, () -> world.currentInteractiveTiles().clear());

    panel.getTileManager().loadMap(MAIN_MAP);
    assertNotSame(cleared, world.navigationGrid());
    world.switchMap(INTERIOR_MAP);
    assertFalse(world.navigationGrid().isWalkable(0, 0));
    world.switchMap(MAIN_MAP);
    assertTrue(world.navigationGrid().isWalkable(column, row));
    world.switchMap("missing-terrain");
    assertThrows(IllegalStateException.class, world::navigationGrid);
  }
}
