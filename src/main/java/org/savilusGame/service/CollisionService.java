package org.savilusGame.service;

import java.util.List;

import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.item.ItemDrop;

/**
 * Abstraction over tile/object/entity/item/player collision detection.
 * <p>Returned index of {@link #INIT_INDEX} means "no collision".
 */
public interface CollisionService {

  int INIT_INDEX = 999;

  /** Resolve tile (map) collision for {@code entity} and set its {@code collisionOn} flag. */
  void checkTile(CollisionBody entity);

  /**
   * Resolve collisions against the objects of the current map.
   *
   * @param entity the moving entity
   * @param player {@code true} when the caller wants the index of the colliding object (player use-case)
   * @return index of the colliding object, or {@link #INIT_INDEX}
   */
  int checkObject(CollisionBody entity, boolean player);

  /**
   * Resolve collisions against a list of entities (NPCs, monsters, interactive tiles, projectiles...).
   *
   * @param entity the moving entity
   * @param target the candidate entities to test against (may be {@code null})
   * @return index of the first colliding entity, or {@link #INIT_INDEX}
   */
  int checkEntity(CollisionBody entity, List<? extends CollisionBody> target);

  /**
   * Resolve collision between {@code entity} and the player.
   *
   * @return {@code true} when the entity overlaps the player
   */
  boolean checkPlayer(CollisionBody entity);

  /**
   * Overlap check against the dropped items of the current map. Items are never solid;
   * unlike {@link #checkObject(CollisionBody, boolean)} this never flags movement collision.
   *
   * @return index of the first overlapping item, or {@link #INIT_INDEX}
   */
  int checkItems(CollisionBody entity, List<ItemDrop> items);
}
