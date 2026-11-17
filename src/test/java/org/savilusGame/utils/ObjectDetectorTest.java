package org.savilusGame.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.enums.GameState.DIALOG_STATE;
import static org.savilusGame.utils.CollisionDetector.INIT_INDEX;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.items.Door;
import org.savilusGame.entity.items.Key;
import org.savilusGame.enums.Direction;
import org.savilusGame.enums.GameObject;

class ObjectDetectorTest {
  @Test
  void usingKeyOnEmptyInteriorMapDoesNotConsumeIt() {
    GamePanel panel = new GamePanel();
    panel.getWorld().switchMap(INTERIOR_MAP);

    assertFalse(new Key(panel).use(panel.getPlayer()));
    assertEquals(DIALOG_STATE, panel.getGameState());
  }

  @Test
  void doorDetectionUsesStableIdentityRatherThanDisplayName() {
    GamePanel panel = new GamePanel();
    var player = panel.getPlayer();
    player.setWorldPosition(0, 0);
    player.setDirection(Direction.RIGHT);
    Door door = new Door(panel);
    door.setName("translated door");

    assertEquals(0, ObjectDetector.detect(player, List.of(door), GameObject.DOOR));
    assertEquals(INIT_INDEX, ObjectDetector.detect(player, List.of(door), GameObject.IRON_DOOR));
    assertEquals(INIT_INDEX, ObjectDetector.detect(player, null, GameObject.DOOR));
  }
}
