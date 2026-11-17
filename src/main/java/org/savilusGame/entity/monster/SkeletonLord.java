package org.savilusGame.entity.monster;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DOOR_OPEN;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SONG;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_UP1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_PHASE2_UP2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_UP1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_ATTACK_UP2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_UP1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_PHASE2_UP2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_UP1;
import static org.savilusGame.config.GameEntityNameFactory.SKELETON_LORD_UP2;
import static org.savilusGame.enums.MonsterType.SKELETON_LORD;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.items.IronDoor;
import org.savilusGame.enums.Direction;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SkeletonLord extends Actor {

  private static final int SHOOT_INTERVAL = 60;
  private static final int DIRECT_CHASE_DISTANCE = 8;
  private static final int MOVE_TOWARDS_PLAYER_INTERVAL = 60;
  private static final int RANDOM_DIRECTION_INTERVAL = 120;

  int bossScale = 5;
  int widthHeight = TILE_SIZE * bossScale;

  public SkeletonLord(GamePanel gamePanel) {
    super(gamePanel);
    name = SKELETON_LORD.getName();
    direction = Direction.DOWN;
    boss = true;
    defaultSpeed = 1;
    speed = defaultSpeed;
    maxLife = 60;
    currentLife = maxLife;
    monster = true;
    attack = 14;
    attackValue = 18;
    defense = 2;
    exp = 50;
    knockBackPower = 5;
    sleep = true;

    int size = TILE_SIZE * 5;
    solidArea.x = 48;
    solidArea.y = 48;
    solidArea.width = size - 48 * 2;
    solidArea.height = size - 48;
    attackArea.width = 170;
    attackArea.height = 170;
    firstAttackMotionDuration = 25;
    secondAttackMotionDuration = 50;
    getImage();
    getAttackImage();
    setDialogues(TextManager.getAllDialoguesForTarget(SKELETON_LORD.getDialogueKey()));
  }

  @Override
  public void checkDrop() {
    gamePanel.getWorld().setBossBattleOn(false);
    gamePanel.getWorld().setSkeletonLordDefeated(true);

    gamePanel.stopBGM();
    gamePanel.playBGM(DUNGEON_SONG);

    var objects = gamePanel.getWorld().currentObjects();
    if (objects == null) return;

    objects.removeIf(object -> object instanceof IronDoor);
    gamePanel.playSE(DOOR_OPEN);
  }

  @Override
  public void damageReaction() {
    actionLockCounter = 0;
  }

  @Override
  public boolean isPursuingDirectly() {
    return alive && !dying && !sleep && !attacking && !knockBack
        && getTileDistance(gamePanel.getPlayer()) < DIRECT_CHASE_DISTANCE;
  }

  @Override
  public void setAction() {
    if (!inRage && currentLife < maxLife / 2) {
      inRage = true;
      getImage();
      getAttackImage();
      defaultSpeed++;
      speed = defaultSpeed;
      attack *= 2;
    }

    if (isPursuingDirectly()) {
      moveTowardPlayer(MOVE_TOWARDS_PLAYER_INTERVAL);
    } else {
      getRandomDirection(RANDOM_DIRECTION_INTERVAL);
    }

    if (!attacking) {
      checkIfShouldAttack(SHOOT_INTERVAL, TILE_SIZE * 7, TILE_SIZE * 5);
    }
  }

  private void getImage() {
    if (!inRage) {
      loadImages(SKELETON_LORD_UP1, SKELETON_LORD_UP2, SKELETON_LORD_DOWN1, SKELETON_LORD_DOWN2,
          SKELETON_LORD_LEFT1, SKELETON_LORD_LEFT2, SKELETON_LORD_RIGHT1, SKELETON_LORD_RIGHT2);
    } else {
      loadImages(SKELETON_LORD_PHASE2_UP1, SKELETON_LORD_PHASE2_UP2, SKELETON_LORD_PHASE2_DOWN1, SKELETON_LORD_PHASE2_DOWN2,
          SKELETON_LORD_PHASE2_LEFT1, SKELETON_LORD_PHASE2_LEFT2, SKELETON_LORD_PHASE2_RIGHT1, SKELETON_LORD_PHASE2_RIGHT2);
    }
  }

  private void getAttackImage() {
    if (!inRage) {
      loadAttackImages(SKELETON_LORD_ATTACK_UP1, SKELETON_LORD_ATTACK_UP2, SKELETON_LORD_ATTACK_DOWN1, SKELETON_LORD_ATTACK_DOWN2,
          SKELETON_LORD_ATTACK_LEFT1, SKELETON_LORD_ATTACK_LEFT2, SKELETON_LORD_ATTACK_RIGHT1, SKELETON_LORD_ATTACK_RIGHT2);
    } else {
      loadAttackImages(SKELETON_LORD_ATTACK_PHASE2_UP1, SKELETON_LORD_ATTACK_PHASE2_UP2, SKELETON_LORD_ATTACK_PHASE2_DOWN1,
          SKELETON_LORD_ATTACK_PHASE2_DOWN2, SKELETON_LORD_ATTACK_PHASE2_LEFT1, SKELETON_LORD_ATTACK_PHASE2_LEFT2, SKELETON_LORD_ATTACK_PHASE2_RIGHT1, SKELETON_LORD_ATTACK_PHASE2_RIGHT2);
    }
  }

  private void loadImages(String skeletonLordUp1, String skeletonLordUp2, String skeletonLordDown1, String skeletonLordDown2, String skeletonLordLeft1, String skeletonLordLeft2, String skeletonLordRight1, String skeletonLordRight2) {
    up1 = sprites.get(skeletonLordUp1, widthHeight, widthHeight);
    up2 = sprites.get(skeletonLordUp2, widthHeight, widthHeight);
    down1 = sprites.get(skeletonLordDown1, widthHeight, widthHeight);
    down2 = sprites.get(skeletonLordDown2, widthHeight, widthHeight);
    left1 = sprites.get(skeletonLordLeft1, widthHeight, widthHeight);
    left2 = sprites.get(skeletonLordLeft2, widthHeight, widthHeight);
    right1 = sprites.get(skeletonLordRight1, widthHeight, widthHeight);
    right2 = sprites.get(skeletonLordRight2, widthHeight, widthHeight);
  }

  private void loadAttackImages(String skeletonLordAttackUp1, String skeletonLordAttackUp2, String skeletonLordAttackDown1, String skeletonLordAttackDown2, String skeletonLordAttackLeft1, String skeletonLordAttackLeft2, String skeletonLordAttackRight1, String skeletonLordAttackRight2) {
    attackUp1 = sprites.get(skeletonLordAttackUp1, widthHeight, widthHeight * 2);
    attackUp2 = sprites.get(skeletonLordAttackUp2, widthHeight, widthHeight * 2);
    attackDown1 = sprites.get(skeletonLordAttackDown1, widthHeight, widthHeight * 2);
    attackDown2 = sprites.get(skeletonLordAttackDown2, widthHeight, widthHeight * 2);
    attackLeft1 = sprites.get(skeletonLordAttackLeft1, widthHeight * 2, widthHeight);
    attackLeft2 = sprites.get(skeletonLordAttackLeft2, widthHeight * 2, widthHeight);
    attackRight1 = sprites.get(skeletonLordAttackRight1, widthHeight * 2, widthHeight);
    attackRight2 = sprites.get(skeletonLordAttackRight2, widthHeight * 2, widthHeight);
  }
}

