package org.savilusGame.data;

import java.util.List;
import java.util.Map;

import org.savilusGame.enums.Direction;

/**
 * Versioned, locale-independent snapshot. Map keys are the existing resource paths;
 * item and object ids are {@code GameObject} enum names.
 */
public record SaveGame(
    int schemaVersion,
    String currentMap,
    PlayerState player,
    boolean skeletonLordDefeated,
    Map<String, MapState> maps
) {
  public static final int SCHEMA_VERSION = 1;

  public record PlayerState(
      int worldX, int worldY, Direction direction,
      int level, int maxLife, int life, int mana, int maxMana, int strength,
      int dexterity, int exp, int nextLevelExp, int money,
      List<ItemState> inventory, int weaponSlot, int shieldSlot, int lightSlot
  ) {}

  public record ItemState(String id, int amount) {}

  public record ObjectState(String id, int worldX, int worldY, boolean opened, String lootId) {}

  public record DropState(String id, int amount, int worldX, int worldY, boolean temporary) {}

  public enum TileKind { DRY_TREE, TRUNK, WALL, METAL_PLATE }

  public record TileState(TileKind kind, int col, int row, int life, boolean invincible) {}

  public record EventPosition(int col, int row) {}

  public record MapState(
      List<ObjectState> objects,
      List<DropState> drops,
      List<TileState> interactiveTiles,
      List<EventPosition> completedEvents
  ) {}
}
