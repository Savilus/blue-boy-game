package org.savilusGame.entity.monster;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.GREEN_SLIME_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.GREEN_SLIME_DOWN2;

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

public class GreenSlime extends Actor {

  private final static int SHOOT_RATE = 100;
  private final static int RANDOM_DIRECTION_INTERVAL = 120;
  private final static int STOP_CHASING_DISTANCE = 15;
  private final static int START_CHASING_DISTANCE = 8;

  public GreenSlime(GamePanel gamePanel) {
    super(gamePanel);
    name = MonsterType.GREEN_SLIME.getName();
    defaultSpeed = 1;
    speed = defaultSpeed;
    maxLife = 4;
    currentLife = maxLife;
    monster = true;
    attack = 5;
    defense = 0;
    exp = 2;
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
    up1 = SpriteLoader.load(GREEN_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(GREEN_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(GREEN_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(GREEN_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(GREEN_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(GREEN_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(GREEN_SLIME_DOWN1, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(GREEN_SLIME_DOWN2, TILE_SIZE, TILE_SIZE);
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
      checkIfShouldStopChasing(gamePanel.getPlayer(), STOP_CHASING_DISTANCE, SHOOT_RATE);
      searchPath(getGoalCol(gamePanel.getPlayer()), getGoalRow(gamePanel.getPlayer()));
// Enable if you want green slime to shoot a rock
//      checkIfShouldShoot(100, 50);
    } else {
      getRandomDirection(RANDOM_DIRECTION_INTERVAL);
      checkIfShouldStartChasing(gamePanel.getPlayer(), START_CHASING_DISTANCE, SHOOT_RATE);
    }
  }
}
