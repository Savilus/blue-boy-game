package org.savilusGame.entity;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BLOCKED;
import static org.savilusGame.config.GameEntityNameFactory.PARRY;
import static org.savilusGame.config.GameEntityNameFactory.RECEIVE_DAMAGE;
import static org.savilusGame.enums.Direction.DOWN;
import static org.savilusGame.enums.Direction.LEFT;
import static org.savilusGame.enums.Direction.RIGHT;
import static org.savilusGame.enums.Direction.UP;
import static org.savilusGame.enums.GameState.CUTSCENE_STATE;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.entity.projectile.Projectile;
import org.savilusGame.enums.Direction;
import org.savilusGame.service.DialogueSpeaker;
import org.savilusGame.service.DialogueStarter;
import org.savilusGame.service.Movable;
import org.savilusGame.service.ParticleSpec;
import org.savilusGame.utils.CameraUtil;
import org.savilusGame.utils.ParticleUtil;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/*
 * SOLID AREA X - WHEN WE START ON AXIS X COUNTING FROM LEFT
 * SOLID AREA Y - WHEN WE START ON AXIS Y COUNTING FROM TOP
 * WIDTH - WIDTH OF THE SOLID AREA, TILE HAS 48 PIXELS
 * HEIGHT - SAME AS WIDTH
 * */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public abstract class Actor implements CollisionBody, DialogueSpeaker, Movable, ParticleSpec {

  private static final String EVENT_MESSAGES = "eventMessages";
  private static final int PARRY_WINDOW = 10;
  private static final int DELAY_SPRITE_CHANGE = -60;
  private static final int INVINCIBLE_TIME = 40;
  private static final int SHOOT_INTERVAL = 50;
  private static final int OFF_BALANCE_TIME = 60;
  private static final int KNOCK_BACK_TIME = 5;
  private static final int SPRITE_CHANGE_TIME = 24;

  final GamePanel gamePanel;

  BufferedImage mainImage, mainImage2, mainImage3;
  BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
  BufferedImage attackUp1, attackUp2, attackDown1, attackDown2, attackLeft1, attackLeft2, attackRight1, attackRight2;
  BufferedImage guardUp, guardDown, guardLeft, guardRight;
  Rectangle solidArea = new Rectangle(0, 0, 45, 45);
  Rectangle attackArea = new Rectangle(0, 0, 0, 0);
  int solidAreaDefaultX, solidAreaDefaultY;
  Map<String, List<String>> dialogues;
  boolean collision = false;
  Actor attacker;

  // STATE
  private String dialogueSet = org.apache.commons.lang3.StringUtils.EMPTY;
  private int dialogueIndex = 0;
  Direction direction = Direction.ANY;
  int spriteNum = 1;
  boolean invincible = false;
  boolean collisionOn = false;
  boolean attacking = false;
  int worldX, worldY;
  boolean alive = true;
  boolean dying = false;
  private boolean hpBarOn = false;
  boolean onPath = false;
  boolean knockBack = false;
  Direction knockBackDirection;
  boolean guarding = false;
  boolean transparent = false;
  boolean offBalance = false;
  boolean inRage = false;
  boolean sleep = false;
  private boolean temporaryObject = false;

  // CHARACTER ATTRIBUTES
  int defaultSpeed;
  int speed;
  String name;
  int maxLife;
  int currentLife;
  int level;
  int strength;
  int attack;
  int attackValue;
  int dexterity;
  int ammo;
  int exp;
  int nextLevelExp;
  int money;
  int defense;
  int maxMana;
  int mana;
  int firstAttackMotionDuration;
  int secondAttackMotionDuration;
  int knockBackPower = 0;
  boolean boss;
  boolean monster = false;
  Item currentLightItem;
  Item currentWeapon;
  Item currentShield;
  Projectile projectile;

  // COUNTER
  int invincibleCounter = 0;
  int actionLockCounter = 0;
  int spriteCounter = 0;
  int dyingCounter = 0;
  int hpBarCounter = 0;
  int shootAvailableCounter = 0;
  int knockBackCounter = 0;
  int guardCounter = 0;
  int offBalanceCounter = 0;

  // INVENTORY
  ArrayList<Item> inventory = new ArrayList<>();

  public Actor(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    setDialogues(TextManager.getAllDialoguesForTarget(EVENT_MESSAGES));
  }

  public boolean isMonster() {
    return monster;
  }

  @Override
  public void startDialogue(DialogueSpeaker speaker, String selectedDialogue) {
    DialogueStarter.start(gamePanel, speaker, selectedDialogue);
  }

  public void update() {
    checkCollision();
    if (!sleep) {
      if (knockBack) {
        if (!collisionOn) {
          switch (knockBackDirection) {
            case UP -> worldY -= speed;
            case DOWN -> worldY += speed;
            case LEFT -> worldX -= speed;
            case RIGHT -> worldX += speed;
          }
        }
        knockBackCounter++;
        if (knockBackCounter == KNOCK_BACK_TIME || collisionOn) {
          knockBackCounter = 0;
          knockBack = false;
          speed = defaultSpeed;
        }
      } else if (attacking) {
        attacking();
      } else {
        setAction();

        // IF COLLISION IS FALSE, PLAYER CAN MOVE
        if (!collisionOn) {
          switch (getDirection()) {
            case UP -> worldY -= speed;
            case DOWN -> worldY += speed;
            case LEFT -> worldX -= speed;
            case RIGHT -> worldX += speed;
          }
        }

        spriteCounter++;
        if (spriteCounter > SPRITE_CHANGE_TIME) {
          spriteNum = spriteNum == 1 ? 2 : 1;
          spriteCounter = 0;
        }
      }

      if (invincible) {
        invincibleCounter++;
        if (invincibleCounter > INVINCIBLE_TIME) {
          invincible = false;
          invincibleCounter = 0;
        }
      }

      if (shootAvailableCounter < SHOOT_INTERVAL) {
        shootAvailableCounter++;
      }

      if (offBalance) {
        offBalanceCounter++;
        if (offBalanceCounter > OFF_BALANCE_TIME) {
          offBalance = false;
          offBalanceCounter = 0;
        }
      }
    }
  }

  public boolean inCamera() {
    return worldX + TILE_SIZE * 5 > getPlayer().worldX - getPlayer().getScreenX() &&
        worldX - TILE_SIZE < getPlayer().worldX + getPlayer().getScreenX() &&
        worldY + TILE_SIZE * 5 > getPlayer().worldY - getPlayer().getScreenY() &&
        worldY - TILE_SIZE < getPlayer().worldY + getPlayer().getScreenY();
  }

  public void draw(Graphics2D graphics2D) {
    if (inCamera()) {
      int temporaryScreenX = getScreenX();
      int temporaryScreenY = getScreenY();

      BufferedImage image = switch (getDirection()) {
        case UP -> {
          if (!attacking) {
            yield (spriteNum == 1) ? up1 : up2;
          } else {
            temporaryScreenY = getScreenY() - up1.getHeight();
            yield (spriteNum == 1) ? attackUp1 : attackUp2;
          }
        }
        case DOWN -> {
          if (!attacking) {
            yield (spriteNum == 1) ? down1 : down2;
          } else {
            yield (spriteNum == 1) ? attackDown1 : attackDown2;
          }
        }
        case LEFT -> {
          if (!attacking) {
            yield (spriteNum == 1) ? left1 : left2;
          } else {
            temporaryScreenX = getScreenX() - left1.getWidth();
            yield (spriteNum == 1) ? attackLeft1 : attackLeft2;
          }
        }
        case RIGHT -> {
          if (!attacking) {
            yield (spriteNum == 1) ? right1 : right2;
          } else {
            yield (spriteNum == 1) ? attackRight1 : attackRight2;
          }
        }
        case ANY -> this.mainImage;
      };

      if (invincible) {
        hpBarOn = true;
        hpBarCounter = 0;
        changeAlpha(graphics2D, 0.6F);
      }

      if (dying) {
        dyingAnimation(graphics2D);
      }

      graphics2D.drawImage(image, temporaryScreenX, temporaryScreenY, null);

      changeAlpha(graphics2D, 1F);
    }
  }

  /** Drop the given item as a world pickup at this actor's position. */
  public void dropItem(Item item) {
    gamePanel.getWorld().dropItem(item, worldX, worldY);
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

  public void getRandomDirection(int interval) {
    actionLockCounter++;
    if (actionLockCounter > interval) {
      int randomNumber = new Random().nextInt(100) + 1;

      if (randomNumber <= 25) direction = Direction.UP;
      else if (randomNumber <= 50) direction = Direction.DOWN;
      else if (randomNumber <= 75) direction = Direction.LEFT;
      else if (randomNumber <= 100) direction = Direction.RIGHT;

      actionLockCounter = 0;
    }
  }

  public Actor setWorldPosition(int worldX, int worldY) {
    this.worldX = worldX;
    this.worldY = worldY;
    return this;
  }

  public int getScreenX() {
    return worldX - getPlayer().worldX + getPlayer().getScreenX();
  }

  public int getScreenY() {
    return worldY - getPlayer().worldY + getPlayer().getScreenY();
  }

  public void resetCounter() {
    invincibleCounter = 0;
    actionLockCounter = 0;
    spriteCounter = 0;
    dyingCounter = 0;
    hpBarCounter = 0;
    shootAvailableCounter = 0;
    knockBackCounter = 0;
    guardCounter = 0;
    offBalanceCounter = 0;
  }

  void attacking() {
    spriteCounter++;
    if (spriteCounter <= firstAttackMotionDuration) {
      spriteNum = 1;
    } else if (spriteCounter <= secondAttackMotionDuration) {
      spriteNum = 2;

      // SAVE THE CURRENT worldX, worldY and solid area
      int currentWorldX = worldX;
      int currentWorldY = worldY;
      int currentSolidAreaWidth = solidArea.width;
      int currentSolidAreaHeight = solidArea.height;

      // adjust player's worldX/Y for the attack area
      switch (getDirection()) {
        case UP -> worldY -= attackArea.height;
        case DOWN -> worldY += attackArea.height;
        case LEFT -> worldX -= attackArea.width;
        case RIGHT -> worldX += attackArea.width;
      }
      // attackArea becomes solid area
      solidArea.width = attackArea.width;
      solidArea.height = attackArea.height;

      if (monster) {
        if (gamePanel.getCollisionDetector().checkPlayer(this)) {
          damagePlayer(attackValue);
        }
      } else {
        // check monster collision with updated worldX, worldY and solidArea
        int monsterIndex = gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentMonsters());
        getPlayer().damageMonster(this, monsterIndex, attack, currentWeapon.getKnockBackPower());

        int interactiveTileIndex = gamePanel.getCollisionDetector().checkEntity(
            this,
            Optional.ofNullable(gamePanel.getWorld().currentInteractiveTiles())
                .orElse(Collections.emptyList())
        );

        getPlayer().damageInteractiveTile(interactiveTileIndex);

        int projectileIndex = gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentProjectiles());
        getPlayer().damageProjectile(projectileIndex);
      }

      worldX = currentWorldX;
      worldY = currentWorldY;
      solidArea.width = currentSolidAreaWidth;
      solidArea.height = currentSolidAreaHeight;
    } else if (spriteCounter > secondAttackMotionDuration) {
      spriteNum = 1;
      spriteCounter = 0;
      attacking = false;
    }
  }


  int getCenterX() {
    return worldX + solidArea.x + solidArea.width / 2;
  }

  int getCenterY() {
    return worldY + solidArea.y + solidArea.height / 2;
  }

  protected void searchPath(int goalCol, int goalRow) {
    int startCol = (worldX + solidArea.x) / TILE_SIZE;
    int startRow = (worldY + solidArea.y) / TILE_SIZE;

    gamePanel.getPathFinder().setNodes(startCol, startRow, goalCol, goalRow);

    if (gamePanel.getPathFinder().search()) {
      var nextNode = gamePanel.getPathFinder().getPathList().getFirst();
      int nextX = nextNode.getCol() * TILE_SIZE;
      int nextY = nextNode.getRow() * TILE_SIZE;

      int entityLeftX = worldX + solidArea.x;
      int entityRightX = entityLeftX + solidArea.width;
      int entityTopY = worldY + solidArea.y;
      int entityBottomY = entityTopY + solidArea.height;

      if (entityLeftX >= nextX && entityRightX < nextX + TILE_SIZE) {
        direction = (entityTopY > nextY) ? UP : (entityTopY < nextY ? DOWN : direction);
      } else if (entityTopY >= nextY && entityBottomY < nextY + TILE_SIZE) {
        direction = (entityLeftX > nextX) ? LEFT : (entityLeftX < nextX ? RIGHT : direction);
      } else {
        // Prefer vertical movement, fallback to horizontal if blocked.
        Direction primaryDir = (entityTopY > nextY) ? UP : DOWN;
        Direction secondaryDir = (entityLeftX > nextX) ? LEFT : RIGHT;

        direction = primaryDir;
        checkCollision();

        if (collisionOn) {
          direction = secondaryDir;
        }
      }

      if (nextNode.getCol() == goalCol && nextNode.getRow() == goalRow) {
        onPath = false;
      }
    }
  }

  protected void moveTowardPlayer(int interval) {
    actionLockCounter++;

    if (actionLockCounter > interval) {
      Player player = gamePanel.getPlayer();

      int xDistance = getXDistance(player);
      int yDistance = getYDistance(player);

      if (xDistance > yDistance) {
        direction = (player.getCenterX() < getCenterX()) ? LEFT : RIGHT;
      } else {
        direction = (player.getCenterY() < getCenterY()) ? UP : DOWN;
      }

      actionLockCounter = 0;
    }
  }

  protected void checkCollision() {
    collisionOn = false;
    gamePanel.getCollisionDetector().checkTile(this);
    gamePanel.getCollisionDetector().checkObject(this, false);
    gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentNpc());
    gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentMonsters());
    gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentInteractiveTiles());
    boolean contactPlayer = gamePanel.getCollisionDetector().checkPlayer(this);

    if (monster && contactPlayer && !gamePanel.getPlayer().invincible) {
      damagePlayer(attack);
    }
  }

  protected void generateParticle(ParticleSpec source, int targetWorldX, int targetWorldY) {
    ParticleUtil.burst(gamePanel, targetWorldX, targetWorldY, source);
  }

  protected void facePlayer() {
    if (Objects.isNull(gamePanel.getPlayer())) return;

    switch (gamePanel.getPlayer().getDirection()) {
      case UP -> direction = DOWN;
      case DOWN -> direction = UP;
      case LEFT -> direction = Direction.RIGHT;
      case RIGHT -> direction = Direction.LEFT;
    }
  }

  protected void damagePlayer(int attack) {

    if (getPlayer() == null || getPlayer().invincible) return;

    int damage = attack - getPlayer().defense;
    Direction canGuardDirection = direction.getOpposite();
    if (getPlayer().guarding && getPlayer().direction == canGuardDirection) {
      if (getPlayer().guardCounter < PARRY_WINDOW) {
        damage = 0;
        gamePanel.playSE(PARRY);
        setKnockBack(this, getPlayer(), knockBackPower);
        offBalance = true;
        spriteCounter = DELAY_SPRITE_CHANGE;
      } else {
        damage = Math.max(damage / 3, 1);
        gamePanel.playSE(BLOCKED);
      }
    } else {
      gamePanel.playSE(RECEIVE_DAMAGE);
      damage = Math.max(damage, 1);
    }

    if (damage > 0) {
      getPlayer().transparent = true;
      setKnockBack(getPlayer(), this, knockBackPower);
    }

    getPlayer().currentLife -= damage;
    getPlayer().invincible = true;
  }

  protected int getTileDistance(Actor target) {
    return (getXDistance(target) + getYDistance(target)) / TILE_SIZE;
  }

  protected int getGoalCol(Actor target) {
    return (target.worldX + target.solidArea.x) / TILE_SIZE;
  }

  protected int getGoalRow(Actor target) {
    return (target.worldY + target.solidArea.y) / TILE_SIZE;
  }

  protected void checkIfShouldAttack(int rate, int straight, int horizontal) {
    boolean targetInRange = false;
    int xDistance = getXDistance(getPlayer());
    int yDistance = getYDistance(getPlayer());

    switch (direction) {
      case UP -> {
        if (getPlayer().getCenterY() < getCenterY() && yDistance < straight && xDistance < horizontal) {
          targetInRange = true;
        }
      }
      case DOWN -> {
        if (getPlayer().getCenterY() > getCenterY() && yDistance < straight && xDistance < horizontal) {
          targetInRange = true;
        }
      }
      case LEFT -> {
        if (getPlayer().getCenterX() < getCenterX() && xDistance < straight && yDistance < horizontal) {
          targetInRange = true;
        }
      }
      case RIGHT -> {
        if (getPlayer().getCenterX() > getCenterX() && xDistance < straight && yDistance < horizontal) {
          targetInRange = true;
        }
      }
    }
    if (targetInRange) {
      int randomChance = new Random().nextInt(rate);
      if (randomChance == 0) {
        attacking = true;
        spriteNum = 1;
        spriteCounter = 0;
        shootAvailableCounter = 0;
      }
    }
  }

  protected void checkIfShouldShoot(int rate, int shotInterval) {
    int randomNumber = new Random().nextInt(rate);
    if (randomNumber == 0 && !projectile.isAlive() && shootAvailableCounter == shotInterval) {
      projectile.set(worldX, worldY, direction, true, this);
      List<Actor> projectilesList = gamePanel.getWorld().currentProjectiles();
      projectilesList.add(projectile);
      shootAvailableCounter = 0;
    }
  }

  protected void checkIfShouldStopChasing(Actor target, int distance, int rate) {
    if (getTileDistance(target) > distance) {
      int randomChance = new Random().nextInt(rate);
      if (randomChance == 0) {
        onPath = false;
      }
    }
  }

  protected void checkIfShouldStartChasing(Actor target, int distance, int rate) {
    if (getTileDistance(target) < distance) {
      int randomChance = new Random().nextInt(rate);
      if (randomChance == 0) {
        onPath = true;
      }
    }
  }

  protected void setKnockBack(Actor target, Actor attacker, int knockBackPower) {
    this.attacker = attacker;
    target.knockBackDirection = attacker.direction;
    if (knockBackPower == 0) return;
    target.speed += knockBackPower;
    target.knockBack = true;
  }

  private void dyingAnimation(Graphics2D graphics2D) {
    dyingCounter++;
    int dyingAnimationInterval = 5;
    int totalPhases = 8;

    if (dyingCounter <= dyingAnimationInterval * totalPhases) {
      boolean visible = ((dyingCounter / dyingAnimationInterval) % 2) == 1;
      changeAlpha(graphics2D, visible ? 1F : 0F);
    } else {
      alive = false;
    }
  }

  private void changeAlpha(Graphics2D graphics2D, float alphaValue) {
    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alphaValue));
  }

  int getLeftX() {
    return worldX + solidArea.x;
  }

  int getRightX() {
    return worldX + solidArea.x + solidArea.width;
  }

  int getTopY() {
    return worldY + solidArea.y;
  }

  int getBottomY() {
    return worldY + solidArea.y + solidArea.height;
  }

  int getXDistance(Actor target) {
    return Math.abs(getCenterX() - target.getCenterX());
  }

  int getYDistance(Actor target) {
    return Math.abs(getCenterY() - target.getCenterY());
  }

  private Player getPlayer() {
    return gamePanel.getPlayer();
  }

  public void setAction() {
  }

  public void damageReaction() {
  }

  public void speak() {
  }

  public void checkDrop() {
  }

  @Override
  public void move(Direction direction) {
  }
}
