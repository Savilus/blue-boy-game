package org.savilusGame.entity.interactiveObjects;

import static org.junit.jupiter.api.Assertions.*;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DOOR_OPEN;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;
import static org.savilusGame.enums.Direction.LEFT;
import static org.savilusGame.enums.Direction.RIGHT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.interactiveTile.MetalPlate;
import org.savilusGame.entity.items.IronDoor;

class BigRockTest {
  @Test
  void onlyTheLastLinkedRockOpensTheDoor() {
    List<String> sounds = new ArrayList<>();
    var panel = new GamePanel() {
      @Override public void playSE(String id) { sounds.add(id); }
    };
    var world = panel.getWorld();
    var map = world.getCurrentMap();
    for (var column : panel.getTileManager().getGameMaps().get(map)) Arrays.fill(column, 10);
    var leftPlate = new MetalPlate(panel, 7, 5);
    var rightPlate = new MetalPlate(panel, 11, 5);
    world.setInteractiveTiles(Map.of(map, Arrays.asList(leftPlate, null, rightPlate)));
    var first = new BigRock(panel);
    var second = new BigRock(panel);
    first.setWorldPosition(7 * TILE_SIZE - 12, 5 * TILE_SIZE);
    second.setWorldPosition(11 * TILE_SIZE - 12, 5 * TILE_SIZE);
    world.setNpcs(Map.of(map, Arrays.asList(first, null, second)));
    var door = new IronDoor(panel);
    world.currentObjects().add(door);

    first.move(RIGHT);
    assertSame(leftPlate, first.getLinkedEntity());
    first.move(LEFT);
    assertNull(first.getLinkedEntity());
    second.move(RIGHT);
    assertSame(rightPlate, second.getLinkedEntity());
    assertEquals(List.of(door), world.currentObjects());
    first.move(RIGHT);
    assertTrue(world.currentObjects().isEmpty());
    assertEquals(List.of(UNLOCK, UNLOCK, UNLOCK, DOOR_OPEN), sounds);
  }
}
