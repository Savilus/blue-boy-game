package org.savilusGame.entity;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_UP1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_ATTACK_UP2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_UP1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_AXE_ATTACK_UP2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_GUARD_DOWN;
import static org.savilusGame.config.GameEntityNameFactory.BOY_GUARD_LEFT;
import static org.savilusGame.config.GameEntityNameFactory.BOY_GUARD_RIGHT;
import static org.savilusGame.config.GameEntityNameFactory.BOY_GUARD_UP;
import static org.savilusGame.config.GameEntityNameFactory.BOY_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_UP1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_UP2;
import static org.savilusGame.config.GameEntityNameFactory.COIN;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_SOUND;
import static org.savilusGame.config.GameEntityNameFactory.GAME_OVER;
import static org.savilusGame.config.GameEntityNameFactory.HIT_MONSTER;
import static org.savilusGame.config.GameEntityNameFactory.LEVEL_UP;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_DOWN_1;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_DOWN_2;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_LEFT_1;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_LEFT_2;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_RIGHT_1;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_RIGHT_2;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_UP_1;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE_UP_2;
import static org.savilusGame.config.GameEntityNameFactory.RECEIVE_DAMAGE;
import static org.savilusGame.config.GameEntityNameFactory.SWING_WEAPON;
import static org.savilusGame.enums.Direction.DOWN;
import static org.savilusGame.enums.Direction.LEFT;
import static org.savilusGame.enums.Direction.RIGHT;
import static org.savilusGame.enums.Direction.UP;
import static org.savilusGame.enums.GameState.DIALOG_STATE;
import static org.savilusGame.enums.GameState.GAME_OVER_STATE;
import static org.savilusGame.utils.CollisionDetector.INIT_INDEX;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.projectile.Fireball;
import org.savilusGame.entity.shield.WoodShield;
import org.savilusGame.entity.weapon.NormalSword;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.service.Interactable;
import org.savilusGame.utils.KeyHandler;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Player extends Actor {

  static final int CHANGE_SPRITE_INTERVAL = 12;
  static final int INVINCIBLE_TIME = 60;
  static final int SHOOT_RATE = 50;
  static final String PICKED_UP = "pickedUp";
  static final String DAMAGE_UI_MESSAGE = "damageUiMessage";
  static final String KILLED_UI_MESSAGE = "killedUiMessage";
  static final String UI_MESSAGE = "uiMessages";
  static final String LVL_UP = "lvlUp";
  static final String EXP_UI_MESSAGE = "expUiMessage";
  static final String INVENTORY_FULL = "inventoryFull";
  static final String EVENT_MESSAGES_KEY = "eventMessages";
  static final int MAX_INVENTORY_SIZE = 20;

  final KeyHandler keyHandler;

  final int screenX;
  final int screenY;
  int standCounter = 0;
  boolean attackCanceled = false;
  boolean lightUpdated = true;
  boolean drawing = true;

  public Player(GamePanel gamePanel, KeyHandler keyHandler) {
    super(gamePanel);
    this.keyHandler = keyHandler;
    screenX = gamePanel.getScreenWidth() / 2 - (TILE_SIZE / 2);
    screenY = gamePanel.getScreenHeight() / 2 - (TILE_SIZE / 2);

    solidArea = new Rectangle();
    solidArea.x = 8;
    solidArea.y = 16;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    solidArea.width = 32;
    solidArea.height = 32;
    setDefaultValues();
  }

  public void setSleepingImage(BufferedImage image) {
    this.mainImage = image;
    this.up2 = image;
    this.down1 = image;
    this.down2 = image;
    this.left1 = image;
    this.up1 = image;
    this.left2 = image;
    this.right1 = image;
    this.right2 = image;
  }

  public void getAttackImage() {
    String[] attackKeys = getAttackImages();

    if (attackKeys != null) {
      attackUp1 = SpriteLoader.load(attackKeys[0], TILE_SIZE, TILE_SIZE * 2);
      attackUp2 = SpriteLoader.load(attackKeys[1], TILE_SIZE, TILE_SIZE * 2);
      attackDown1 = SpriteLoader.load(attackKeys[2], TILE_SIZE, TILE_SIZE * 2);
      attackDown2 = SpriteLoader.load(attackKeys[3], TILE_SIZE, TILE_SIZE * 2);
      attackLeft1 = SpriteLoader.load(attackKeys[4], TILE_SIZE * 2, TILE_SIZE);
      attackLeft2 = SpriteLoader.load(attackKeys[5], TILE_SIZE * 2, TILE_SIZE);
      attackRight1 = SpriteLoader.load(attackKeys[6], TILE_SIZE * 2, TILE_SIZE);
      attackRight2 = SpriteLoader.load(attackKeys[7], TILE_SIZE * 2, TILE_SIZE);
    }
  }

  public int getCurrentWeaponSlot() {
    return inventory.indexOf(currentWeapon);
  }

  public int getCurrentShieldSlot() {
    return inventory.indexOf(currentShield);
  }

  public void selectItem() {
    int itemIndex = gamePanel.getUi().getItemIndexFromInventory(gamePanel.getUi().getPlayerSlotCol(), gamePanel.getUi().getPlayerSlotRow());

    if (itemIndex < inventory.size()) {
      Item selectedItem = inventory.get(itemIndex);

      switch (selectedItem.getType()) {
        case SWORD, AXE, PICKAXE -> {
          currentWeapon = selectedItem;
          attack = getAttack();
          getAttackImage();
        }
        case SHIELD -> {
          currentShield = selectedItem;
          defense = getDefense();
        }
        case CONSUMABLE -> {
          if (selectedItem.use(this)) {
            int stack = selectedItem.getStackCount();
            if (stack > 1) selectedItem.setStackCount(stack - 1);
            else inventory.remove(itemIndex);
          }
        }
        case LIGHTING -> {
          if (currentLightItem == selectedItem) currentLightItem = null;
          else currentLightItem = selectedItem;

          lightUpdated = true;
        }
      }
    }
  }

  public void getImage() {
    up1 = SpriteLoader.load(BOY_UP1, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(BOY_UP2, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(BOY_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(BOY_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(BOY_LEFT1, TILE_SIZE, TILE_SIZE);
    up1 = SpriteLoader.load(BOY_UP1, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(BOY_LEFT2, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(BOY_RIGHT1, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(BOY_RIGHT2, TILE_SIZE, TILE_SIZE);
  }

  public void setDefaultValues() {
    worldX = TILE_SIZE * 23;
    worldY = TILE_SIZE * 21;
    defaultSpeed = 4;
    speed = defaultSpeed;
    direction = DOWN;

    // PLAYER STATUS
    maxLife = 6;
    currentLife = maxLife;
    maxMana = 4;
    mana = maxMana;
    ammo = 10;
    level = 1;
    strength = 0;
    dexterity = 1;
    exp = 0;
    nextLevelExp = 5;
    money = 100;
    setItems();
    attack = getAttack();
    defense = getDefense();
    setDialogues(TextManager.getAllDialoguesForTarget(EVENT_MESSAGES_KEY));
    getImage();
    getAttackImage();
    getGuardImage();
  }

  public void setItems() {
    currentWeapon = new NormalSword(gamePanel);
    currentShield = new WoodShield(gamePanel);
    projectile = new Fireball(gamePanel);
    inventory.clear();
    inventory.add(currentWeapon);
    inventory.add(currentShield);
  }

  public int getDefense() {
    defense = dexterity * currentShield.getDefenseValue();
    return defense;
  }

  public int getAttack() {
    var profile = currentWeapon.getAttackProfile();
    attackArea.width = profile.width();
    attackArea.height = profile.height();
    firstAttackMotionDuration = profile.firstMotionDuration();
    secondAttackMotionDuration = profile.secondMotionDuration();
    attack = strength + currentWeapon.getAttackValue();
    return attack;
  }

  public void setDefaultPositions() {
    worldX = TILE_SIZE * 23;
    worldY = TILE_SIZE * 21;
    direction = DOWN;
  }

  public void restorePlayerStatus() {
    currentLife = maxLife;
    mana = maxMana;
    invincible = false;
    transparent = false;
    attacking = false;
    lightUpdated = true;
    knockBack = false;
    speed = defaultSpeed;
  }

  public boolean canObtainItem(Item item) {
    Item newItem = gamePanel.getGameEntityFactory().getItem(item.getName());
    if (newItem == null) {
      return false;
    }
    if (newItem.isStackable()) {
      int itemIndex = searchItemInInventory(item.getId());

      if (itemIndex != INIT_INDEX) {
        var stacked = inventory.get(itemIndex);
        stacked.setStackCount(stacked.getStackCount() + 1);
        return true;
      }
    }
    if (doesInventoryHaveSpace()) {
      inventory.add(newItem);
      return true;
    }
    return false;
  }

  public void damageMonster(Actor attacker, int monsterIndex, int attack, int knockBackPower) {
    if (monsterIndex == INIT_INDEX) return;
    var monster = gamePanel.getWorld().currentMonsters().get(monsterIndex);
    if (monster.isInvincible()) return;

    gamePanel.playSE(HIT_MONSTER);

    if (knockBackPower > 0) {
      setKnockBack(monster, attacker, knockBackPower);
    }
    if (monster.isOffBalance()) {
      attack *= 3;
    }

    int damage = Math.max(0, attack - monster.getDefense());
    monster.setCurrentLife(monster.getCurrentLife() - damage);
    gamePanel.getUi().addMessage(String.format(TextManager.getUiText(UI_MESSAGE, DAMAGE_UI_MESSAGE), damage));

    monster.setInvincible(true);
    monster.damageReaction();

    if (monster.getCurrentLife() <= 0) {
      monster.setDying(true);
      gamePanel.getUi().addMessage(String.format(TextManager.getUiText(UI_MESSAGE, KILLED_UI_MESSAGE), monster.getName()));
      gamePanel.getUi().addMessage(String.format(TextManager.getUiText(UI_MESSAGE, EXP_UI_MESSAGE), monster.getExp()));
      exp += monster.getExp();
      checkLevelUp();
    }
  }

  void damageProjectile(int projectileIndex) {
    if (projectileIndex != INIT_INDEX) {
      var projectile = gamePanel.getWorld().currentProjectiles().get(projectileIndex);
      projectile.setAlive(false);
      generateParticle(projectile, projectile.getWorldX(), projectile.getWorldY());
    }
  }

  void damageInteractiveTile(int interactiveTileIndex) {
    var tiles = gamePanel.getWorld().currentInteractiveTiles();

    if (interactiveTileIndex != INIT_INDEX && !tiles.get(interactiveTileIndex).isInvincible()
        && tiles.get(interactiveTileIndex).isDestructible() && tiles.get(interactiveTileIndex).isCorrectItem(this)) {
      var interactiveTile = tiles.get(interactiveTileIndex);
      interactiveTile.playSoundEffect();
      interactiveTile.setCurrentLife(interactiveTile.getCurrentLife() - 1);
      interactiveTile.setInvincible(true);

      generateParticle(interactiveTile, interactiveTile.getWorldX(), interactiveTile.getWorldY());

      if (interactiveTile.currentLife <= 0) {
        tiles.set(interactiveTileIndex, interactiveTile.getDestroyedForm());
      }
    }
  }

  private void getGuardImage() {
    guardUp = SpriteLoader.load(BOY_GUARD_UP, TILE_SIZE, TILE_SIZE);
    guardDown = SpriteLoader.load(BOY_GUARD_DOWN, TILE_SIZE, TILE_SIZE);
    guardLeft = SpriteLoader.load(BOY_GUARD_LEFT, TILE_SIZE, TILE_SIZE);
    guardRight = SpriteLoader.load(BOY_GUARD_RIGHT, TILE_SIZE, TILE_SIZE);
  }

  // Use also to check if player has special event item
  private int searchItemInInventory(String itemId) {
    return IntStream.range(0, inventory.size())
        .filter(itemIndex -> inventory.get(itemIndex).getId().equals(itemId))
        .findFirst()
        .orElse(INIT_INDEX);
  }

  private void pickUpObject(int objectIndex) {
    if (objectIndex == INIT_INDEX) {
      return;
    }
    var interactedObject = gamePanel.getWorld().currentObjects().get(objectIndex);
    if (interactedObject instanceof Interactable interactable && keyHandler.isEnterPressed()) {
      attackCanceled = true;
      interactable.interact();
    }
  }

  private void pickUpItems(int itemIndex) {
    if (itemIndex == INIT_INDEX) {
      return;
    }
    var itemDrops = gamePanel.getWorld().currentItems();
    var item = itemDrops.get(itemIndex).getItem();

    if (item.getType() == WorldGameTypes.PICK_UP) {
      item.use(this);
      itemDrops.remove(itemIndex);
      return;
    }

    String text;
    if (canObtainItem(item)) {
      gamePanel.playSE(COIN);
      text = String.format(TextManager.getUiText(UI_MESSAGE, PICKED_UP), item.getName());
    } else {
      text = TextManager.getUiText(UI_MESSAGE, INVENTORY_FULL);
    }
    gamePanel.getUi().addMessage(text);
    itemDrops.remove(itemIndex);
  }

  private void checkLevelUp() {
    if (exp >= nextLevelExp) {
      level++;
      nextLevelExp = nextLevelExp + 5;
      exp = 0;
      maxLife += 2;
      strength++;
      dexterity++;
      attack = getAttack();
      defense = getDefense();
      gamePanel.playSE(LEVEL_UP);
      gamePanel.setGameState(DIALOG_STATE);

      startDialogue(this, LVL_UP);
    }
  }

  private void interactNPC(int npcIndex) {
    if (npcIndex != INIT_INDEX) {
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        attackCanceled = true;
        gamePanel.getWorld().currentNpc().get(npcIndex).speak();
      }
      gamePanel.getWorld().currentNpc().get(npcIndex).move(direction);
    }
  }

  private void detectMonsterContact(int monsterIndex) {
    if (monsterIndex != INIT_INDEX && !invincible && !gamePanel.getWorld().currentMonsters().get(monsterIndex).dying) {
      int damage = gamePanel.getWorld().currentMonsters().get(monsterIndex).attack - defense;
      if (damage < 1)
        damage = 1;
      currentLife -= damage;
      gamePanel.playSE(RECEIVE_DAMAGE);
      invincible = true;
      transparent = true;
    }
  }

  private String[] getAttackImages() {
    String[] attackKeys = null;

    switch (currentWeapon.getType()) {
      case WorldGameTypes.SWORD -> attackKeys = new String[]{
          BOY_ATTACK_UP1, BOY_ATTACK_UP2, BOY_ATTACK_DOWN1, BOY_ATTACK_DOWN2,
          BOY_ATTACK_LEFT1, BOY_ATTACK_LEFT2, BOY_ATTACK_RIGHT1, BOY_ATTACK_RIGHT2
      };
      case WorldGameTypes.AXE -> attackKeys = new String[]{
          BOY_AXE_ATTACK_UP1, BOY_AXE_ATTACK_UP2, BOY_AXE_ATTACK_DOWN1, BOY_AXE_ATTACK_DOWN2,
          BOY_AXE_ATTACK_LEFT1, BOY_AXE_ATTACK_LEFT2, BOY_AXE_ATTACK_RIGHT1, BOY_AXE_ATTACK_RIGHT2
      };
      case WorldGameTypes.PICKAXE -> attackKeys = new String[]{
          PICK_AXE_UP_1, PICK_AXE_UP_2, PICK_AXE_DOWN_1, PICK_AXE_DOWN_2,
          PICK_AXE_LEFT_1, PICK_AXE_LEFT_2, PICK_AXE_RIGHT_1, PICK_AXE_RIGHT_2
      };
    }
    return attackKeys;
  }

  private boolean doesInventoryHaveSpace() {
    return inventory.size() < MAX_INVENTORY_SIZE;
  }

  @Override
  public void update() {
    if (knockBack) {
      // CHECK TILE COLLISION
      collisionOn = false;
      gamePanel.getCollisionDetector().checkTile(this);
      gamePanel.getCollisionDetector().checkObject(this, true);
      gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentNpc());
      gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentMonsters());
      if (Objects.nonNull(gamePanel.getWorld().currentInteractiveTiles())) {
        gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentInteractiveTiles());
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
      if (knockBackCounter == 5 || collisionOn) {
        knockBackCounter = 0;
        knockBack = false;
        speed = defaultSpeed;
      }
    } else if (attacking) {
      attacking();
    } else if (keyHandler.isSpacePressed()) {
      guarding = true;
      guardCounter++;
    } else if (keyHandler.isUpPressed() || keyHandler.isDownPressed() || keyHandler.isLeftPressed()
        || keyHandler.isRightPressed() || keyHandler.isEnterPressed()) {
      if (keyHandler.isUpPressed()) {
        direction = UP;
      } else if (keyHandler.isDownPressed()) {
        direction = DOWN;
      } else if (keyHandler.isLeftPressed()) {
        direction = LEFT;
      } else if (keyHandler.isRightPressed()) {
        direction = RIGHT;
      }

      // CHECK TILE COLLISION
      collisionOn = false;
      gamePanel.getCollisionDetector().checkTile(this);

      // CHECK OBJECT COLLISION
      int objectIndex = gamePanel.getCollisionDetector().checkObject(this, true);
      pickUpObject(objectIndex);

      // CHECK DROPPED ITEM COLLISION
      int itemIndex = gamePanel.getCollisionDetector().checkItems(this, gamePanel.getWorld().currentItems());
      pickUpItems(itemIndex);

      //CHECK NPC COLLISION
      int npcIndex = gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentNpc());
      interactNPC(npcIndex);

      // CHECK MONSTER COLLISION
      int monsterIndex = gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentMonsters());
      detectMonsterContact(monsterIndex);

      // CHECK INTERACTIVE TILE COLLISION
      gamePanel.getCollisionDetector().checkEntity(
          this,
          Optional.ofNullable(gamePanel.getWorld().currentInteractiveTiles())
              .orElse(Collections.emptyList())
      );


      // CHECK EVENT
      gamePanel.getEventHandler().checkEvent();

      // IF COLLISION IS FALSE, PLAYER CAN MOVE
      if (!collisionOn && !keyHandler.isEnterPressed()) {
        switch (getDirection()) {
          case UP -> worldY -= speed;
          case DOWN -> worldY += speed;
          case LEFT -> worldX -= speed;
          case RIGHT -> worldX += speed;
        }
      }

      if (keyHandler.isEnterPressed() && !attackCanceled) {
        gamePanel.playSE(SWING_WEAPON);
        attacking = true;
        spriteCounter = 0;
      }

      attackCanceled = false;
      guarding = false;
      gamePanel.getKeyHandler().setEnterPressed(false);
      guardCounter = 0;

      spriteCounter++;
      if (spriteCounter > CHANGE_SPRITE_INTERVAL) {
        spriteNum = spriteNum == 1 ? 2 : 1;
        spriteCounter = 0;
      }
    } else {
      standCounter++;
      guarding = false;
      guardCounter = 0;
      if (standCounter == 20) {
        spriteNum = 1;
        standCounter = 0;
      }
    }

    if (keyHandler.isShotKeyPressed() && !projectile.alive && shootAvailableCounter == 50 &&
        projectile.haveResource(this)) {
      // SET DEFAULT COORDINATES, DIRECTION AND USER
      projectile.set(worldX, worldY, direction, true, this);

      projectile.substractResource(this);
      gamePanel.getWorld().currentProjectiles().add(projectile);
      shootAvailableCounter = 0;
      gamePanel.playSE(FIREBALL_SOUND);
    }

    if (invincible) {
      invincibleCounter++;
      if (invincibleCounter > INVINCIBLE_TIME) {
        invincible = false;
        transparent = false;
        invincibleCounter = 0;
      }
    }

    if (shootAvailableCounter < SHOOT_RATE) {
      shootAvailableCounter++;
    }

    if (currentLife > maxLife) {
      currentLife = maxLife;
    }

    if (mana > maxMana) {
      mana = maxMana;
    }

    if (!keyHandler.isGodModeOn() && currentLife <= 0) {
      gamePanel.setGameState(GAME_OVER_STATE);
      gamePanel.getUi().setCommandNum(-1);
      gamePanel.stopBGM();
      gamePanel.playSE(GAME_OVER);
    }
  }

  @Override
  public void draw(Graphics2D graphic2d) {
    int temporaryScreenX = screenX;
    int temporaryScreenY = screenY;
    BufferedImage image = switch (getDirection()) {
      case UP -> {
        if (attacking) {
          temporaryScreenY = screenY - TILE_SIZE;
          if (spriteNum == 1) yield attackUp1;
          else yield attackUp2;
        } else {
          if (guarding) {
            yield guardUp;
          }
          if (spriteNum == 1) yield up1;
          else yield up2;
        }
      }
      case DOWN -> {
        if (attacking) {
          if (spriteNum == 1) yield attackDown1;
          else yield attackDown2;
        } else {
          if (guarding) {
            yield guardDown;
          }
          if (spriteNum == 1) yield down1;
          else yield down2;
        }
      }
      case LEFT -> {
        if (attacking) {
          temporaryScreenX = screenX - TILE_SIZE;
          if (spriteNum == 1) yield attackLeft1;
          else yield attackLeft2;
        } else {
          if (guarding) {
            yield guardLeft;
          }
          if (spriteNum == 1) yield left1;
          else yield left2;
        }
      }
      case RIGHT -> {
        if (attacking) {
          if (spriteNum == 1) yield attackRight1;
          else yield attackRight2;
        } else {
          if (guarding) {
            yield guardRight;
          }
          if (spriteNum == 1) yield right1;
          else yield right2;
        }
      }
      case ANY -> null;
    };


    if (transparent) {
      graphic2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
    }
    if (drawing) {
      graphic2d.drawImage(image, temporaryScreenX, temporaryScreenY, null);
    }
    graphic2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
    // COLLISION RECTANGLE DRAWING
//    graphic2d.setColor(Color.RED);
//    graphic2d.drawRect(screenX + solidArea.x, screenY + solidArea.y, solidArea.width, solidArea.height);
  }

}
