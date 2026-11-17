package org.savilusGame.entity;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.enums.Direction.ANY;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.item.Item;
import org.savilusGame.enums.Direction;
import org.savilusGame.service.DialogueSpeaker;
import org.savilusGame.service.DialogueStarter;
import org.savilusGame.service.Interactable;
import org.savilusGame.service.ParticleSpec;
import org.savilusGame.utils.CameraUtil;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * A world-placed object the player can overlap, bump into, look at or (sometimes) destroy:
 * doors, chests, iron doors, interactive tiles.
 * <p>Not a character: no combat stats, no AI. Interactive behaviour is opted into via
 * {@code interact()} (default: nothing); destructive behaviour via the {@code Destructible}
 * interface implemented by the tile hierarchy.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class InteractiveObject implements CollisionBody, DialogueSpeaker, Interactable, ParticleSpec {

  private static final String EVENT_MESSAGES = "eventMessages";

  final GamePanel gamePanel;

  String name;
  int worldX, worldY;
  Direction direction = ANY;
  Rectangle solidArea = new Rectangle(0, 0, 45, 45);
  int solidAreaDefaultX, solidAreaDefaultY;
  boolean collision = false;
  boolean invincible = false;
  int invincibleCounter = 0;
  protected int invincibleTime = 40;
  boolean opened = false;
  private boolean temporaryObject = false;
  int currentLife = 1;

  BufferedImage mainImage, mainImage2;

  private Map<String, List<String>> dialogues;
  private String dialogueSet = org.apache.commons.lang3.StringUtils.EMPTY;
  private int dialogueIndex = 0;

  /** Loot handed out by chests; {@code null} for other objects. */
  @Setter(AccessLevel.NONE)
  Item loot;

  protected InteractiveObject(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    this.setDialogues(TextManager.getAllDialoguesForTarget(EVENT_MESSAGES));
  }

  @Override
  public void startDialogue(DialogueSpeaker speaker, String dialogueSet) {
    DialogueStarter.start(gamePanel, speaker, dialogueSet);
  }

  @Override
  public void interact() {
    // Nothing to do by default; interactive subclasses override.
  }

  public void update() {
    if (invincible) {
      invincibleCounter++;
      if (invincibleCounter > invincibleTime) {
        invincible = false;
        invincibleCounter = 0;
      }
    }
  }

  public void draw(Graphics2D graphics2D) {
    if (CameraUtil.isOnScreen(gamePanel, worldX, worldY) && mainImage != null) {
      graphics2D.drawImage(mainImage, CameraUtil.screenX(gamePanel, worldX),
          CameraUtil.screenY(gamePanel, worldY), null);
    }
  }

  public InteractiveObject setWorldPosition(int worldX, int worldY) {
    this.worldX = worldX;
    this.worldY = worldY;
    return this;
  }

  public int getSpeed() {
    return 0;
  }

  public Direction getKnockBackDirection() {
    return direction;
  }

  public boolean isKnockBack() {
    return false;
  }

  public boolean isCollision() {
    return collision;
  }

  public void setCollisionOn(boolean collisionOn) {
    this.collision = collisionOn;
  }

  public Color getParticleColor() {
    return Color.WHITE;
  }

  public int getParticleSize() {
    return 0;
  }

  public int getParticleSpeed() {
    return 0;
  }

  public int getParticleMaxLife() {
    return 0;
  }

  public String getName() {
    return name;
  }
}
