package org.savilusGame.entity.interactiveObjects;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BIGROCK;
import static org.savilusGame.config.GameEntityNameFactory.DOOR_OPEN;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;

import java.awt.*;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
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

  public BigRock(GamePanel gamePanel) {
    super(gamePanel);

    name = TextManager.getItemName(GameObject.BIG_ROCK.getTextKey());
    direction = Direction.DOWN;
    speed = 4;
    getImage();
    setDialogues(TextManager.getItemTexts(BIG_ROCK_DIALOGUES_KEY));

    solidArea = new Rectangle();
    solidArea.x = 2;
    solidArea.y = 6;
    solidArea.width = 44;
    solidArea.height = 40;
  }

  public InteractiveTile getLinkedEntity() {
    return linkedEntity;
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

  private void detectPlate() {
    for (var plate : gamePanel.getWorld().currentInteractiveTiles()) {
      if (plate == null || plate.getGameObject() != GameObject.METAL_PLATE) continue;
      int distance = Math.max(Math.abs(worldX - plate.getWorldX()), Math.abs(worldY - plate.getWorldY()));

      if (distance < LINKED_OBJECTS_DISTANCE) {
        if (linkedEntity == null) {
          linkedEntity = plate;
          gamePanel.playSE(UNLOCK);
        }
      } else if (linkedEntity == plate) {
        linkedEntity = null;
      }
    }

    for (var npc : gamePanel.getWorld().currentNpc()) {
      if (npc instanceof BigRock rock && rock.linkedEntity == null) return;
    }
    var objects = gamePanel.getWorld().currentObjects();
    for (int index = 0; index < objects.size(); index++) {
      var object = objects.get(index);
      if (object != null && object.getGameObject() == GameObject.IRON_DOOR) {
        objects.remove(index);
        gamePanel.playSE(DOOR_OPEN);
        return;
      }
    }
  }

  private void getImage() {
    up1 = down1 = left1 = right1 = sprites.get(BIGROCK, TILE_SIZE, TILE_SIZE);
    up2 = down2 = left2 = right2 = up1;
  }

}
