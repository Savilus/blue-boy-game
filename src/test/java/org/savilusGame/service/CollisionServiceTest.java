package org.savilusGame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.entity.items.RedPotion;
import org.savilusGame.enums.Direction;

/**
 * Verifies the collision service behavior used by player and monsters.
 * <p>Runs fully headless; construction of {@link GamePanel} performs no display/audio interaction.
 * Must be run with the game working directory as CWD because legacy text/config
 * loaders resolve paths relative to it (a known debt item tracked in the review).
 */
class CollisionServiceTest {

  /** Minimal concrete actor for collision checks (all hooks have safe defaults). */
  static class TestEntity extends Actor {
    TestEntity(GamePanel gamePanel, int worldX, int worldY, Direction direction, int speed) {
      super(gamePanel);
      this.worldX = worldX;
      this.worldY = worldY;
      this.direction = direction;
      this.speed = speed;
      this.defaultSpeed = speed;
    }
  }

  GamePanel panel;
  CollisionService collision;

  @BeforeEach
  void setUp() {
    panel = new GamePanel();
    collision = panel; // GamePanel implements CollisionService since Phase 0
  }

  @Test
  @DisplayName("checkEntity returns INIT_INDEX for null and empty target lists")
  void checkEntityHandlesNullAndEmpty() {
    TestEntity entity = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    assertEquals(CollisionService.INIT_INDEX, collision.checkEntity(entity, null));
    assertEquals(CollisionService.INIT_INDEX, collision.checkEntity(entity, List.of()));
    assertFalse(entity.isCollisionOn());
  }

  @Test
  @DisplayName("checkEntity detects overlap in movement direction and flags collision")
  void checkEntityDetectsOverlap() {
    TestEntity mover = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    TestEntity blocker = new TestEntity(panel, 0, 30, Direction.ANY, 4);
    int index = collision.checkEntity(mover, List.of(blocker));
    assertEquals(0, index);
    assertTrue(mover.isCollisionOn());
  }

  @Test
  @DisplayName("checkEntity does not flag entities far away in movement direction")
  void checkEntityIgnoresDistantTargets() {
    TestEntity mover = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    TestEntity distant = new TestEntity(panel, 9600, 9600, Direction.ANY, 4);
    int index = collision.checkEntity(mover, List.of(distant));
    assertEquals(CollisionService.INIT_INDEX, index);
    assertFalse(mover.isCollisionOn());
  }

  @Test
  @DisplayName("checkPlayer returns true for an entity overlapping the player start position")
  void checkPlayerDetectsOverlap() {
    var player = panel.getPlayer();
    TestEntity entity = new TestEntity(panel, player.getWorldX(), player.getWorldY(), Direction.DOWN, 4);
    assertTrue(collision.checkPlayer(entity));
    assertTrue(entity.isCollisionOn());
  }

  @Test
  @DisplayName("checkObject returns INIT_INDEX when the current map has no objects")
  void checkObjectReturnsInitIndexWithoutObjects() {
    TestEntity entity = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    assertEquals(CollisionService.INIT_INDEX, collision.checkObject(entity, false));
  }

  @Test
  @DisplayName("checkTile flags collision when moving into a solid tile")
  void checkTileFlagsSolidTile() {
    // Find any solid tile in the currently loaded map and approach it from above.
    var tiles = panel.getTileManager().getTile();
    int[][] map = panel.getTileManager().getGameMaps().get(panel.getWorld().getCurrentMap());
    for (int row = map[0].length - 1; row >= 1; row--) {
      for (int col = 0; col < map.length; col++) {
        int tileNum = map[col][row];
        if (tileNum >= 0 && tileNum < tiles.length && tiles[tileNum] != null && tiles[tileNum].collision()) {
          TestEntity entity = new TestEntity(panel, col * 48, (row - 1) * 48, Direction.DOWN, 48);
          entity.setWorldPosition(col * 48, (row - 1) * 48);
          collision.checkTile(entity);
          assertTrue(entity.isCollisionOn(),
              "Expected collision when moving into solid tile at col=" + col + ", row=" + row);
          return;
        }
      }
    }
    throw new IllegalStateException("No solid tile found in current map; cannot verify tile collision");
  }

  @Test
  @DisplayName("checkTile checks the leading top-right corner when moving right")
  void checkTileChecksLeadingTopRightCorner() {
    int[][] map = panel.getTileManager().getGameMaps().get(panel.getWorld().getCurrentMap());
    var tiles = panel.getTileManager().getTile();
    int solidTile = IntStream.range(0, tiles.length)
        .filter(index -> tiles[index] != null && tiles[index].collision())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No solid tile available for collision test"));
    int emptyTile = IntStream.range(0, tiles.length)
        .filter(index -> tiles[index] != null && !tiles[index].collision())
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No non-solid tile available for collision test"));

    int leadingCol = 10;
    int topRow = 10;
    int previousLeadingTop = map[leadingCol][topRow];
    int previousLeadingBottom = map[leadingCol][topRow + 1];
    int previousTrailingTop = map[leadingCol - 1][topRow];
    map[leadingCol][topRow] = solidTile;
    map[leadingCol][topRow + 1] = emptyTile;
    map[leadingCol - 1][topRow] = emptyTile;

    try {
      TestEntity entity = new TestEntity(
          panel,
          leadingCol * 48 - 14,
          topRow * 48 + 30,
          Direction.RIGHT,
          4
      );
      entity.setSolidArea(new Rectangle(0, 0, 10, 30));

      collision.checkTile(entity);

      assertTrue(entity.isCollisionOn(),
          "A solid tile at the leading top-right corner must block rightward movement");
    } finally {
      map[leadingCol][topRow] = previousLeadingTop;
      map[leadingCol][topRow + 1] = previousLeadingBottom;
      map[leadingCol - 1][topRow] = previousTrailingTop;
    }
  }

  @Test
  @DisplayName("checkTile blocks movement beyond the map boundary")
  void checkTileBlocksMapBoundary() {
    int mapWidth = panel.getTileManager().getGameMaps().get(panel.getWorld().getCurrentMap()).length;
    TestEntity entity = new TestEntity(panel, mapWidth * 48 - 13, 10 * 48, Direction.RIGHT, 4);
    entity.setSolidArea(new Rectangle(0, 0, 10, 30));

    collision.checkTile(entity);

    assertTrue(entity.isCollisionOn(), "Out-of-bounds map cells must be treated as solid");
  }

  @Test
  @DisplayName("checkTile blocks movement just beyond the left map boundary")
  void checkTileBlocksNegativeBoundary() {
    TestEntity entity = new TestEntity(panel, 0, 10 * 48, Direction.LEFT, 1);
    entity.setSolidArea(new Rectangle(0, 0, 10, 30));

    collision.checkTile(entity);

    assertTrue(entity.isCollisionOn(), "Negative world coordinates must not truncate to column zero");
  }

  @Test
  @DisplayName("checkItems returns INIT_INDEX for null and empty item lists")
  void checkItemsHandlesNullAndEmpty() {
    TestEntity entity = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    assertEquals(CollisionService.INIT_INDEX, collision.checkItems(entity, null));
    assertEquals(CollisionService.INIT_INDEX, collision.checkItems(entity, List.of()));
    assertFalse(entity.isCollisionOn());
  }

  @Test
  @DisplayName("checkItems reports an overlapping item without flagging movement collision")
  void checkItemsDetectsOverlapWithoutBlocking() {
    TestEntity mover = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    ItemDrop drop = new ItemDrop(panel, new RedPotion(panel)).setWorldPosition(0, 30);

    int index = collision.checkItems(mover, List.of(drop));

    assertEquals(0, index);
    assertFalse(mover.isCollisionOn(), "dropped items must never block movement");
  }

  @Test
  @DisplayName("checkItems ignores drops far away in movement direction")
  void checkItemsIgnoresDistantDrops() {
    TestEntity mover = new TestEntity(panel, 0, 0, Direction.DOWN, 4);
    ItemDrop drop = new ItemDrop(panel, new RedPotion(panel)).setWorldPosition(9600, 9600);

    int index = collision.checkItems(mover, List.of(drop));

    assertEquals(CollisionService.INIT_INDEX, index);
    assertFalse(mover.isCollisionOn());
  }
}
