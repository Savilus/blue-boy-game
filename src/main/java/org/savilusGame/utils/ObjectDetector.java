package org.savilusGame.utils;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.utils.CollisionDetector.INIT_INDEX;

import java.util.List;
import java.util.Objects;

import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.InteractiveObject;

/** Front-facing tile detection for interactions (e.g. the key opening a door). */
public final class ObjectDetector {

  private ObjectDetector() {
  }

  /**
   * Index of the object standing in the tile in front of {@code user}, or {@link CollisionDetector#INIT_INDEX}.
   * Mirrors the legacy semantics: the probe point is the user's leading edge, offset by its speed.
   */
  public static int detect(CollisionBody user, List<InteractiveObject> objects, String targetName) {
    int index = INIT_INDEX;
    if (Objects.isNull(objects) || objects.isEmpty()) {
      return index;
    }

    int solidLeftX = user.getWorldX() + user.getSolidArea().x;
    int solidTopY = user.getWorldY() + user.getSolidArea().y;
    int solidRightX = solidLeftX + user.getSolidArea().width;
    int solidBottomY = solidTopY + user.getSolidArea().height;
    int userSpeed = user.getSpeed();

    int nextWorldX = solidLeftX;
    int nextWorldY = solidTopY;
    switch (user.getDirection()) {
      case UP -> nextWorldY = solidTopY - userSpeed;
      case DOWN -> nextWorldY = solidBottomY + userSpeed;
      case LEFT -> nextWorldX = solidLeftX - userSpeed;
      case RIGHT -> nextWorldX = solidRightX + userSpeed;
      case ANY -> {
        return index;
      }
    }

    int col = nextWorldX / TILE_SIZE;
    int row = nextWorldY / TILE_SIZE;

    for (int objectIndex = 0; objectIndex < objects.size(); objectIndex++) {
      var gameObject = objects.get(objectIndex);
      if (Objects.nonNull(gameObject)) {
        int objectCol = (gameObject.getWorldX() + gameObject.getSolidArea().x) / TILE_SIZE;
        int objectRow = (gameObject.getWorldY() + gameObject.getSolidArea().y) / TILE_SIZE;
        if (objectCol == col && objectRow == row && targetName.equals(gameObject.getName())) {
          index = objectIndex;
        }
      }
    }

    return index;
  }
}
