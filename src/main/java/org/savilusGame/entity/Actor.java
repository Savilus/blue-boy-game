package org.savilusGame.entity;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BLOCKED;
import static org.savilusGame.config.GameEntityNameFactory.PARRY;
import static org.savilusGame.config.GameEntityNameFactory.RECEIVE_DAMAGE;
import static org.savilusGame.enums.Direction.DOWN;
import static org.savilusGame.enums.Direction.LEFT;
import static org.savilusGame.enums.Direction.RIGHT;
import static org.savilusGame.enums.Direction.UP;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import org.savilusGame.GamePanel;
import org.savilusGame.utils.SpriteCache;
import org.savilusGame.service.DepthDrawable;
import org.savilusGame.ai.Node;
import org.savilusGame.ai.PathFollower;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.projectile.Projectile;
import org.savilusGame.enums.Direction;
import org.savilusGame.service.DialogueSpeaker;
import org.savilusGame.service.DialogueStarter;
import org.savilusGame.service.Movable;
import org.savilusGame.service.ParticleSpec;
import org.savilusGame.utils.ParticleUtil;
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
public abstract class Actor implements DepthDrawable, CollisionBody, DialogueSpeaker, Movable, ParticleSpec {

  String name;
  static final int KNOCK_BACK_TIME = 5;
  final GamePanel gamePanel;
  @Getter(AccessLevel.NONE)
  final SpriteCache sprites;
  BufferedImage mainImage, mainImage2, mainImage3;
  BufferedImage up1, up2, down1, down2, left1, left2, right1, right2;
  BufferedImage attackUp1, attackUp2, attackDown1, attackDown2, attackLeft1, attackLeft2, attackRight1, attackRight2;
  BufferedImage guardUp, guardDown, guardLeft, guardRight;

  Rectangle solidArea = new Rectangle(0, 0, 45, 45);
  Rectangle attackArea = new Rectangle(0, 0, 0, 0);

  Map<String, List<String>> dialogues;
  Actor attacker;
  Direction direction = Direction.ANY;
  Direction knockBackDirection;
  Item currentLightItem;
  Item currentWeapon;
  Item currentShield;
  Projectile projectile;
  // INVENTORY
  ArrayList<Item> inventory = new ArrayList<>();

  boolean collision = false;
  int spriteNum = 1;
  boolean invincible = false;
  boolean collisionOn = false;
  boolean attacking = false;
  int worldX, worldY;
  boolean alive = true;
  boolean dying = false;
  boolean knockBack = false;
  boolean guarding = false;
  boolean transparent = false;
  boolean offBalance = false;
  boolean inRage = false;
  boolean sleep = false;
  // CHARACTER ATTRIBUTES
  int defaultSpeed;
  int speed;
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

  private static final String EVENT_MESSAGES = "eventMessages";
  private String dialogueSet = org.apache.commons.lang3.StringUtils.EMPTY;
  private static final int PARRY_WINDOW = 10;
  private static final int DELAY_SPRITE_CHANGE = -60;
  private static final int INVINCIBLE_TIME = 40;
  private static final int SHOOT_INTERVAL = 50;
  private static final int OFF_BALANCE_TIME = 60;
  private static final int SPRITE_CHANGE_TIME = 24;
  @Getter(AccessLevel.NONE)
  @Setter(AccessLevel.NONE)
  private PathFollower pathFollower;
  private int dialogueIndex = 0;
  private boolean hpBarOn = false;
  private boolean onPath;
  private boolean temporaryObject = false;

  public Actor(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    this.sprites = gamePanel.getSpriteCache();
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
        updateKnockBack();
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
      int randomNumber = ThreadLocalRandom.current().nextInt(100) + 1;

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

  public void setOnPath(boolean onPath) {
    this.onPath = onPath;
    if (!onPath) pathFollower = null;
  }

  /** Remaining tiles for the dev overlay. Painting never searches or advances the cursor. */
  public List<Node> remainingPath() {
    return alive && !dying && !sleep && onPath && pathFollower != null ? pathFollower.remaining() : List.of();
  }

  /** Some enemies steer directly instead of planning a tile route. */
  public boolean isPursuingDirectly() {
    return false;
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

  protected void searchPath(int goalCol, int goalRow) {
    if (!onPath) return;
    int startCol = Math.floorDiv(getLeftX(), TILE_SIZE);
    int startRow = Math.floorDiv(getTopY(), TILE_SIZE);
    if (startCol == goalCol && startRow == goalRow) {
      setOnPath(false);
      return;
    }
    if (pathFollower == null) pathFollower = new PathFollower(gamePanel.getPathFinder());
    Node nextNode = pathFollower.nextStep(gamePanel.getWorld().navigationGrid(), startCol, startRow, goalCol, goalRow);
    if (nextNode == null) return;
    int nextX = nextNode.column() * TILE_SIZE;
    int nextY = nextNode.row() * TILE_SIZE;

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
    resolveWorldCollisions(false);
    boolean contactPlayer = gamePanel.getCollisionDetector().checkPlayer(this);

    if (monster && contactPlayer && !gamePanel.getPlayer().invincible) {
      damagePlayer(attack);
    }
  }

  /**
   * Collision checks shared by normal movement and knockback: tiles, world objects, NPCs, monsters
   * and interactive tiles. Contact with the player (and the damage it causes) is added by
   * {@link #checkCollision()} for the non-player actors only.
   *
   * @param forPlayer whether the caller is the player (affects the object index the detector reports)
   */
  protected void resolveWorldCollisions(boolean forPlayer) {
    collisionOn = false;
    gamePanel.getCollisionDetector().checkTile(this);
    gamePanel.getCollisionDetector().checkObject(this, forPlayer);
    gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentNpc());
    gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentMonsters());
    var interactiveTiles = gamePanel.getWorld().currentInteractiveTiles();
    if (Objects.nonNull(interactiveTiles)) {
      gamePanel.getCollisionDetector().checkEntity(this, interactiveTiles);
    }
  }

  /**
   * One knockback tick, shared by every actor so the player and the monsters cannot drift apart:
   * drift one step in the hit direction unless blocked, and end the knockback after
   * {@link #KNOCK_BACK_TIME} ticks or on the first collision, restoring the normal speed.
   * <p>The caller resolves collisions first, because the player and the other actors do that
   * differently ({@link #resolveWorldCollisions(boolean)}).
   */
  protected void updateKnockBack() {
    if (!knockBack) {
      return;
    }
    if (!collisionOn) {
      switch (knockBackDirection) {
        case UP -> worldY -= speed;
        case DOWN -> worldY += speed;
        case LEFT -> worldX -= speed;
        case RIGHT -> worldX += speed;
      }
    }
    knockBackCounter++;
    if (knockBackCounter >= KNOCK_BACK_TIME || collisionOn) {
      knockBackCounter = 0;
      knockBack = false;
      speed = defaultSpeed;
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
    return Math.floorDiv(target.getLeftX(), TILE_SIZE);
  }

  protected int getGoalRow(Actor target) {
    return Math.floorDiv(target.getTopY(), TILE_SIZE);
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
      int randomChance = ThreadLocalRandom.current().nextInt(rate);
      if (randomChance == 0) {
        attacking = true;
        spriteNum = 1;
        spriteCounter = 0;
        shootAvailableCounter = 0;
      }
    }
  }

  protected void checkIfShouldShoot(int rate, int shotInterval) {
    int randomNumber = ThreadLocalRandom.current().nextInt(rate);
    if (randomNumber == 0 && !projectile.isAlive() && shootAvailableCounter == shotInterval) {
      projectile.set(worldX, worldY, direction, true, this);
      List<Actor> projectilesList = gamePanel.getWorld().currentProjectiles();
      projectilesList.add(projectile);
      shootAvailableCounter = 0;
    }
  }

  protected void checkIfShouldStopChasing(Actor target, int distance, int rate) {
    if (getTileDistance(target) > distance) {
      int randomChance = ThreadLocalRandom.current().nextInt(rate);
      if (randomChance == 0) {
        setOnPath(false);
      }
    }
  }

  protected void checkIfShouldStartChasing(Actor target, int distance, int rate) {
    if (getTileDistance(target) < distance) {
      int randomChance = ThreadLocalRandom.current().nextInt(rate);
      if (randomChance == 0) {
        setOnPath(true);
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

  void attacking() {
    spriteCounter++;
    if (spriteCounter <= firstAttackMotionDuration) {
      spriteNum = 1;
    } else if (spriteCounter <= secondAttackMotionDuration) {
      spriteNum = 2;

      resolveStrike();
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

  /** Project the attack for collision callbacks, never resize the shared body rectangle. */
  private void resolveStrike() {
    int originalX = worldX;
    int originalY = worldY;
    Rectangle body = solidArea;
    attackArea.setLocation(body.x, body.y);
    solidArea = attackArea;
    try {
      switch (direction) {
        case UP -> worldY -= attackArea.height;
        case DOWN -> worldY += attackArea.height;
        case LEFT -> worldX -= attackArea.width;
        case RIGHT -> worldX += attackArea.width;
      }
      if (monster) {
        if (gamePanel.checkPlayer(this)) damagePlayer(attackValue);
      } else {
        var world = gamePanel.getWorld();
        int hit = gamePanel.checkEntity(this, world.currentMonsters());
        getPlayer().damageMonster(this, hit, attack, currentWeapon.getKnockBackPower());
        getPlayer().damageInteractiveTile(gamePanel.checkEntity(this, world.currentInteractiveTiles()));
        getPlayer().damageProjectile(gamePanel.checkEntity(this, world.currentProjectiles()));
      }
    } finally {
      worldX = originalX;
      worldY = originalY;
      solidArea = body;
    }
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

  private Player getPlayer() {
    return gamePanel.getPlayer();
  }
}
