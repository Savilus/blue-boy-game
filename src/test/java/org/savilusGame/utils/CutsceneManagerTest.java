package org.savilusGame.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.monster.GreenSlime;
import org.savilusGame.entity.monster.SkeletonLord;

/**
 * The boss is addressed by its type. Display names are data (they can be translated), so neither a
 * renamed boss nor a minion that happens to share the boss name may change which actor the cutscene
 * drives.
 */
class CutsceneManagerTest {

  @Test
  void theBossIsFoundByTypeNotByDisplayName() {
    GamePanel panel = new GamePanel();
    var boss = new SkeletonLord(panel);
    boss.setName("Kosciej");
    var impostor = new GreenSlime(panel);
    impostor.setName("Skeleton Lord");

    panel.getWorld().currentMonsters().add(impostor);
    panel.getWorld().currentMonsters().add(boss);

    assertEquals(boss, new CutsceneManager(panel).skeletonLord().orElseThrow());
  }

  @Test
  void aMapWithoutTheBossHasNoBoss() {
    GamePanel panel = new GamePanel();
    panel.getWorld().currentMonsters().add(new GreenSlime(panel));

    assertTrue(new CutsceneManager(panel).skeletonLord().isEmpty());
  }
}
