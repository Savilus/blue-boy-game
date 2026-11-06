package org.savilusGame.entity.item;

import java.awt.*;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.CollisionBody;
import org.savilusGame.enums.Direction;
import org.savilusGame.utils.CameraUtil;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

/**
 * World representation of an item lying on the ground (dropped loot or seeded pickups).
 * <p>Never solid; only overlapped by the player to collect. The item itself stays shared data.
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ItemDrop implements CollisionBody {

  final GamePanel gamePanel;
  final Item item;
  final Rectangle solidArea = new Rectangle(0, 0, 45, 45);
  final int solidAreaDefaultX;
  final int solidAreaDefaultY;
  final Direction direction = Direction.ANY;
  int worldX;
  int worldY;
  private boolean temporaryObject = false;

  public ItemDrop(GamePanel gamePanel, Item item) {
    this.gamePanel = gamePanel;
    this.item = item;
    this.solidAreaDefaultX = this.solidArea.x;
    this.solidAreaDefaultY = this.solidArea.y;
  }

  public void setTemporaryObject(boolean temporaryObject) {
    this.temporaryObject = temporaryObject;
  }

  public ItemDrop setWorldPosition(int worldX, int worldY) {
    this.worldX = worldX;
    this.worldY = worldY;
    return this;
  }

  public String getName() {
    return item.getName();
  }

  public void draw(Graphics2D graphics2D) {
    if (CameraUtil.isOnScreen(gamePanel, worldX, worldY)) {
      graphics2D.drawImage(item.getIcon(), CameraUtil.screenX(gamePanel, worldX),
          CameraUtil.screenY(gamePanel, worldY), null);
    }
  }

  @Override
  public int getSpeed() {
    return 0;
  }

  @Override
  public boolean isKnockBack() {
    return false;
  }

  @Override
  public Direction getKnockBackDirection() {
    return direction;
  }

  @Override
  public boolean isCollision() {
    return false;
  }

  @Override
  public void setCollisionOn(boolean collisionOn) {
    // Items are never solid: movement collision resolution deliberately ignores them.
  }
}
