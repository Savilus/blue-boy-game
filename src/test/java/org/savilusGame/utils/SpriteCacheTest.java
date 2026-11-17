package org.savilusGame.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.savilusGame.config.GameEntityNameFactory.BOY_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_DOWN2;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.items.Heart;
import org.savilusGame.entity.monster.GreenSlime;

class SpriteCacheTest {
  @Test
  void keyIncludesPathAndBothDimensions() {
    var sprites = new SpriteCache();
    var first = sprites.get(BOY_DOWN1, 24, 48);
    assertEquals(24, first.getWidth());
    assertEquals(48, first.getHeight());
    assertSame(first, sprites.get(BOY_DOWN1, 24, 48));
    assertNotSame(first, sprites.get(BOY_DOWN1, 24, 24));
    assertNotSame(first, sprites.get(BOY_DOWN1, 48, 48));
    assertNotSame(first, sprites.get(BOY_DOWN2, 24, 48));
  }

  @Test
  void evictsTheLeastRecentlyUsedSuccessfulEntry() {
    var sprites = new SpriteCache(2);
    var first = sprites.get(BOY_DOWN1, 12, 12);
    var second = sprites.get(BOY_DOWN2, 12, 12);
    assertSame(first, sprites.get(BOY_DOWN1, 12, 12));
    var error = assertThrows(IllegalStateException.class, () -> sprites.get("/missing.png", 12, 12));
    assertTrue(error.getMessage().contains("/missing.png"));
    assertThrows(IllegalStateException.class, () -> sprites.get("/config.yml", 12, 12));
    sprites.get(BOY_DOWN2, 24, 24);
    assertSame(first, sprites.get(BOY_DOWN1, 12, 12));
    assertNotSame(second, sprites.get(BOY_DOWN2, 12, 12));
    assertThrows(IllegalArgumentException.class, () -> sprites.get(BOY_DOWN1, 1, 0));
    assertThrows(IllegalArgumentException.class, () -> new SpriteCache(0));
  }

  @Test
  void hudItemsAndRespawnedMonstersShareImagesButNotGameState() {
    var panel = new GamePanel();
    assertSame(panel.getUi().getHeartFull(), new Heart(panel).getIcon());
    var first = new GreenSlime(panel);
    var second = new GreenSlime(panel);
    first.setCurrentLife(0);
    assertTrue(second.getCurrentLife() > 0);
    assertSame(first.getDown1(), second.getDown1());
    assertNotSame(first.getDown1(), first.getDown2());
  }
}
