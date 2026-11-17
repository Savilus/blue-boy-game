package org.savilusGame.entity.interactiveObjects;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BIGROCK;
import static org.savilusGame.config.GameEntityNameFactory.DOOR_OPEN;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.enums.Direction;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class BigRock extends Actor {

  private static final String BIG_ROCK_DIALOGUES_KEY = "bigRock";
  private static final String USE = "use";
  private static final int LINKED_OBJECTS_DISTANCE = 12;

  /** The metal plate this rock is currently linked to, if any. */
  private InteractiveTile linkedEntity;

  public InteractiveTile getLinkedEntity() {
    return linkedEntity;
  }

  public BigRock(GamePanel gamePanel) {
    super(gamePanel);

    name = GameObject.BIG_ROCK.getName();
    direction = Direction.DOWN;
    speed = 4;
    getImage();
    setDialogues(TextManager.getItemTexts(BIG_ROCK_DIALOGUES_KEY));

    solidArea = new Rectangle();
    solidArea.x = 2;
    solidArea.y = 6;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    solidArea.width = 44;
    solidArea.height = 40;
  }

  private void detectPlate() {
    var currentMap = gamePanel.getWorld().currentInteractiveTiles();

    List<InteractiveTile> plateList = currentMap.stream()
        .filter(Objects::nonNull)
        .filter(tile -> StringUtils.equals(GameObject.METAL_PLATE.getName(), tile.getName()))
        .toList();

    List<BigRock> rockList = gamePanel.getWorld().currentNpc().stream()
        .filter(BigRock.class::isInstance)
        .map(BigRock.class::cast)
        .toList();

    for (InteractiveTile plate : plateList) {
      int distance = Math.max(Math.abs(worldX - plate.getWorldX()), Math.abs(worldY - plate.getWorldY()));

      if (distance < LINKED_OBJECTS_DISTANCE) {
        if (Objects.isNull(linkedEntity)) {
          linkedEntity = plate;
          gamePanel.playSE(UNLOCK);
        }
      } else if (linkedEntity == plate) {
        linkedEntity = null;
      }
    }

    long linkedRocks = rockList.stream()
        .filter(rock -> Objects.nonNull(rock.getLinkedEntity()))
        .count();

    if (linkedRocks == rockList.size()) {
      var objects = gamePanel.getWorld().currentObjects();
      for (int doorQuantity = 0; doorQuantity < objects.size(); doorQuantity++) {
        var object = objects.get(doorQuantity);
        if (Objects.nonNull(object) &&
            GameObject.IRON_DOOR.getName().equals(object.getName())) {

          objects.remove(doorQuantity);
          gamePanel.playSE(DOOR_OPEN);
          break;
        }
      }
    }
  }

  private void getImage() {
    up1 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(BIGROCK, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public void move(Direction direction) {
    this.direction = direction;
    checkCollision();

    if (!collisionOn) {
      switch (direction) {
        case UP:
          worldY -= speed;
          break;
        case DOWN:
          worldY += speed;
          break;
        case LEFT:
          worldX -= speed;
          break;
        case RIGHT:
          worldX += speed;
          break;
      }
    }
    detectPlate();
  }

  @Override
  public void speak() {
    startDialogue(this, USE);
  }

  @Override
  public void update() {
  }

}

