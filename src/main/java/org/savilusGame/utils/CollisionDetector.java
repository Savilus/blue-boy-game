package org.savilusGame.utils;

import static org.savilusGame.GamePanel.TILE_SIZE;

import java.util.List;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.enums.Direction;
import org.savilusGame.service.CollisionService;

import lombok.RequiredArgsConstructor;

/** Projects motion without changing either participant's local hitbox. */
@RequiredArgsConstructor
public class CollisionDetector implements CollisionService {
  private final GamePanel gamePanel;

  @Override
  public void checkTile(CollisionBody entity) {
    Direction movement = movementOf(entity);
    if (movement == Direction.ANY) return;
    var area = entity.getSolidArea();
    int x = projectedX(entity, movement) + area.x;
    int y = projectedY(entity, movement) + area.y;
    int left = Math.floorDiv(x, TILE_SIZE);
    int right = Math.floorDiv(x + area.width, TILE_SIZE);
    int top = Math.floorDiv(y, TILE_SIZE);
    int bottom = Math.floorDiv(y + area.height, TILE_SIZE);
    String currentMap = gamePanel.getWorld().getCurrentMap();
    int[][] map = gamePanel.getTileManager().getGameMaps().get(currentMap);
    if (map == null) throw new IllegalStateException("No tile map loaded for current map: " + currentMap);
    boolean blocked = switch (movement) {
      case UP -> isSolidTile(map, left, top) || isSolidTile(map, right, top);
      case DOWN -> isSolidTile(map, left, bottom) || isSolidTile(map, right, bottom);
      case LEFT -> isSolidTile(map, left, top) || isSolidTile(map, left, bottom);
      case RIGHT -> isSolidTile(map, right, top) || isSolidTile(map, right, bottom);
      case ANY -> false;
    };
    if (blocked) entity.setCollisionOn(true);
  }

  @Override
  public int checkObject(CollisionBody entity, boolean player) {
    var objects = gamePanel.getWorld().currentObjects();
    if (objects == null || objects.isEmpty()) return INIT_INDEX;
    Direction movement = movementOf(entity);
    int x = projectedX(entity, movement);
    int y = projectedY(entity, movement);
    int index = INIT_INDEX;
    for (int i = 0; i < objects.size(); i++) {
      var object = objects.get(i);
      if (object != null && object != entity && intersects(entity, x, y, object)) {
        if (object.isCollision()) entity.setCollisionOn(true);
        if (player) index = i;
      }
    }
    return index;
  }

  @Override
  public int checkEntity(CollisionBody entity, List<? extends CollisionBody> targets) {
    int index = overlapIndex(entity, targets);
    if (index != INIT_INDEX) entity.setCollisionOn(true);
    return index;
  }

  @Override
  public boolean checkPlayer(CollisionBody entity) {
    Direction movement = movementOf(entity);
    var player = gamePanel.getPlayer();
    if (player == entity || !intersects(entity, projectedX(entity, movement), projectedY(entity, movement), player)) {
      return false;
    }
    entity.setCollisionOn(true);
    return true;
  }

  @Override
  public int checkItems(CollisionBody entity, List<ItemDrop> items) {
    return overlapIndex(entity, items); // pickups do not block movement
  }

  private static int overlapIndex(CollisionBody entity, List<? extends CollisionBody> targets) {
    if (targets == null || targets.isEmpty()) return INIT_INDEX;
    Direction movement = movementOf(entity);
    int x = projectedX(entity, movement);
    int y = projectedY(entity, movement);
    // The original forward scan selected the last overlap (e.g. two drops on one tile).
    for (int index = targets.size() - 1; index >= 0; index--) {
      var target = targets.get(index);
      if (target != null && target != entity && intersects(entity, x, y, target)) return index;
    }
    return INIT_INDEX;
  }

  private static boolean intersects(CollisionBody entity, int x, int y, CollisionBody target) {
    var area = target.getSolidArea();
    return entity.getSolidArea().intersects(target.getWorldX() - x + area.x, target.getWorldY() - y + area.y,
        area.width, area.height);
  }

  private static Direction movementOf(CollisionBody entity) {
    return entity.isKnockBack() ? entity.getKnockBackDirection() : entity.getDirection();
  }

  private static int projectedX(CollisionBody entity, Direction movement) {
    return entity.getWorldX() + switch (movement) {
      case LEFT -> -entity.getSpeed();
      case RIGHT -> entity.getSpeed();
      default -> 0;
    };
  }

  private static int projectedY(CollisionBody entity, Direction movement) {
    return entity.getWorldY() + switch (movement) {
      case UP -> -entity.getSpeed();
      case DOWN -> entity.getSpeed();
      default -> 0;
    };
  }

  private boolean isSolidTile(int[][] map, int column, int row) {
    if (column < 0 || column >= map.length || row < 0 || row >= map[column].length) return true;
    int number = map[column][row];
    var tiles = gamePanel.getTileManager().getTile();
    if (number < 0 || number >= tiles.length || tiles[number] == null) {
      throw new IllegalStateException("Invalid tile number " + number + " at col=" + column + ", row=" + row);
    }
    return tiles[number].collision();
  }
}
