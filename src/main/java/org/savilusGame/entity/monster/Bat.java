package org.savilusGame.entity.monster;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BAT_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BAT_DOWN2;

import java.util.concurrent.ThreadLocalRandom;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.items.BronzeCoin;
import org.savilusGame.entity.items.Heart;
import org.savilusGame.entity.items.ManaCrystal;
import org.savilusGame.enums.MonsterType;

public class Bat extends Actor {

  private static final int CHANGE_DIRECTION_INTERVAL = 10;

  public Bat(GamePanel gamePanel) {
    super(gamePanel);
    name = MonsterType.BAT.getName();
    defaultSpeed = 4;
    speed = defaultSpeed;
    maxLife = 15;
    currentLife = maxLife;
    monster = true;
    attack = 10;
    defense = 0;
    exp = 6;

    solidArea.x = 3;
    solidArea.y = 15;
    solidArea.width = 42;
    solidArea.height = 21;
    getImage();
  }

  @Override
  public void checkDrop() {
    int percentForDrop = ThreadLocalRandom.current().nextInt(100) + 1;

    if (percentForDrop < 50)
      dropItem(new BronzeCoin(gamePanel));
    else if (percentForDrop < 75)
      dropItem(new Heart(gamePanel));
    else dropItem(new ManaCrystal(gamePanel));
  }

  @Override
  public void damageReaction() {
    actionLockCounter = 0;
    setOnPath(true);
  }

  @Override
  public void setAction() {
    getRandomDirection(CHANGE_DIRECTION_INTERVAL);
  }

  private void getImage() {
    up1 = down1 = left1 = right1 = sprites.get(BAT_DOWN1, TILE_SIZE, TILE_SIZE);
    up2 = down2 = left2 = right2 = sprites.get(BAT_DOWN2, TILE_SIZE, TILE_SIZE);
  }
}
