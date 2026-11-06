package org.savilusGame.entity;

import java.awt.Rectangle;

import org.savilusGame.enums.Direction;

/**
 * Structural contract for anything the tile/entity collision code can test.
 * <p>Implemented by characters, world (interactive) objects and dropped items;
 * decouples {@code CollisionDetector} from the concrete entity hierarchy.
 * The mutable solid area is intentionally exposed: collision resolution reuses
 * the rectangle instance and resets it to the defaults afterwards.
 */
public interface CollisionBody {

  Rectangle getSolidArea();

  int getSolidAreaDefaultX();

  int getSolidAreaDefaultY();

  int getWorldX();

  int getWorldY();

  int getSpeed();

  Direction getDirection();

  boolean isKnockBack();

  Direction getKnockBackDirection();

  /** Whether this body is treated as solid for movement collision. */
  boolean isCollision();

  void setCollisionOn(boolean collisionOn);

  /** Runtime-spawned (e.g. dropped loot, cutscene props): cleared when the game is reset. */
  boolean isTemporaryObject();

  void setTemporaryObject(boolean temporaryObject);
}
