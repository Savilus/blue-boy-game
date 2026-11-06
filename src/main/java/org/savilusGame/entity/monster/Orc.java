package org.savilusGame.entity.monster;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_UP1;
import static org.savilusGame.config.GameEntityNameFactory.ATTACK_ORC_UP2;
import static org.savilusGame.config.GameEntityNameFactory.ORC_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.ORC_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.ORC_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.ORC_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.ORC_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.ORC_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.ORC_UP1;
import static org.savilusGame.config.GameEntityNameFactory.ORC_UP2;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.entity.items.Key;
import org.savilusGame.enums.MonsterType;
import org.savilusGame.enums.WorldGameTypes;

public class Orc extends Actor {

  private final static int CHASE_RATE = 100;
  private final static int ATTACK_RATE = 30;
  private final static int RANDOM_DIRECTION_INTERVAL = 120;
  private final static int STOP_CHASING_DISTANCE = 10;
  private final static int START_CHASING_DISTANCE = 8;

  public Orc(GamePanel gamePanel) {
    super(gamePanel);
    name = MonsterType.ORC.getName();
    defaultSpeed = 1;
    speed = defaultSpeed;
    maxLife = 24;
    currentLife = maxLife;
    monster = true;
    attack = 10;
    attackValue = 16;
    defense = 8;
    exp = 6;
    knockBackPower = 5;

    solidArea.x = 4;
    solidArea.y = 4;
    solidArea.width = 37;
    solidArea.height = 41;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    attackArea.width = 48;
    attackArea.height = 48;
    firstAttackMotionDuration = 40;
    secondAttackMotionDuration = 85;
    getImage();
    getAttackImage();
  }

  private void getImage() {
    up1 = SpriteLoader.load(ORC_UP1, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(ORC_UP2, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(ORC_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(ORC_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(ORC_LEFT1, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(ORC_LEFT2, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(ORC_RIGHT1, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(ORC_RIGHT2, TILE_SIZE, TILE_SIZE);
  }

  public void getAttackImage() {
    attackUp1 = SpriteLoader.load(ATTACK_ORC_UP1, TILE_SIZE, TILE_SIZE * 2);
    attackUp2 = SpriteLoader.load(ATTACK_ORC_UP2, TILE_SIZE, TILE_SIZE * 2);
    attackDown1 = SpriteLoader.load(ATTACK_ORC_DOWN1, TILE_SIZE, TILE_SIZE * 2);
    attackDown2 = SpriteLoader.load(ATTACK_ORC_DOWN2, TILE_SIZE, TILE_SIZE * 2);
    attackLeft1 = SpriteLoader.load(ATTACK_ORC_LEFT1, TILE_SIZE * 2, TILE_SIZE);
    attackLeft2 = SpriteLoader.load(ATTACK_ORC_LEFT2, TILE_SIZE * 2, TILE_SIZE);
    attackRight1 = SpriteLoader.load(ATTACK_ORC_RIGHT1, TILE_SIZE * 2, TILE_SIZE);
    attackRight2 = SpriteLoader.load(ATTACK_ORC_RIGHT2, TILE_SIZE * 2, TILE_SIZE);
  }

  @Override
  public void checkDrop() {

    dropItem(new Key(gamePanel));
  }

  @Override
  public void damageReaction() {
    actionLockCounter = 0;
    onPath = true;
  }

  @Override
  public void setAction() {
    if (onPath) {
      checkIfShouldStopChasing(gamePanel.getPlayer(), START_CHASING_DISTANCE, CHASE_RATE);
      searchPath(getGoalCol(gamePanel.getPlayer()), getGoalRow(gamePanel.getPlayer()));
    } else {
      getRandomDirection(RANDOM_DIRECTION_INTERVAL);
      checkIfShouldStartChasing(gamePanel.getPlayer(), STOP_CHASING_DISTANCE, CHASE_RATE);
    }
    if (!attacking) {
      checkIfShouldAttack(ATTACK_RATE, TILE_SIZE * 4, TILE_SIZE);
    }
  }
}
