package org.savilusGame.entity.monster;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.RED_SLIME_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.RED_SLIME_DOWN2;

import java.util.Random;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.entity.items.BronzeCoin;
import org.savilusGame.entity.items.Heart;
import org.savilusGame.entity.items.ManaCrystal;
import org.savilusGame.entity.projectile.Rock;
import org.savilusGame.enums.MonsterType;
import org.savilusGame.enums.WorldGameTypes;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RedSlime extends Actor {

  static int STOP_CHASE_DISTANCE = 15;
  static int START_CHASE_DISTANCE = 5;
  static int CHASE_POSSIBILITY_RATE = 45;
  static int SHOOT_POSSIBILITY_RATE = 85;
  static int SHOOT_INTERVAL = 50;
  static int RANDOM_DIRECTION_INTERVAL = 80;

  public RedSlime(GamePanel gamePanel) {
    super(gamePanel);
    name = MonsterType.RED_SLIME.getName();
    defaultSpeed = 2;
    speed = defaultSpeed;
    maxLife = 10;
    currentLife = maxLife;
    monster = true;
    attack = 7;
    defense = 0;
    exp = 4;
    projectile = new Rock(gamePanel);

    solidArea.x = 3;
    solidArea.y = 18;
    solidArea.width = 42;
    solidArea.height = 30;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    getImage();
  }

  private void getImage() {
    up1 = SpriteLoader.load(RED_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(RED_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(RED_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(RED_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(RED_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(RED_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(RED_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(RED_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public void checkDrop() {
    int percentForDrop = new Random().nextInt(100) + 1;

    if (percentForDrop < 50)
      dropItem(new BronzeCoin(gamePanel));
    else if (percentForDrop < 75)
      dropItem(new Heart(gamePanel));
    else dropItem(new ManaCrystal(gamePanel));
  }

  @Override
  public void damageReaction() {
    actionLockCounter = 0;
    onPath = true;
  }

  @Override
  public void setAction() {
    if (onPath) {
      checkIfShouldStopChasing(gamePanel.getPlayer(),STOP_CHASE_DISTANCE , CHASE_POSSIBILITY_RATE);
      searchPath(getGoalCol(gamePanel.getPlayer()), getGoalRow(gamePanel.getPlayer()));
      checkIfShouldShoot(SHOOT_POSSIBILITY_RATE, SHOOT_INTERVAL);
    } else {
      getRandomDirection(RANDOM_DIRECTION_INTERVAL);
      checkIfShouldStartChasing(gamePanel.getPlayer(), START_CHASE_DISTANCE, CHASE_POSSIBILITY_RATE);
    }
  }
}
