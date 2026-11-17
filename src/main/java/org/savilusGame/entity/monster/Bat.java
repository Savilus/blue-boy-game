package org.savilusGame.entity.monster;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BAT_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BAT_DOWN2;

import java.util.Random;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.entity.items.BronzeCoin;
import org.savilusGame.entity.items.Heart;
import org.savilusGame.entity.items.ManaCrystal;
import org.savilusGame.enums.MonsterType;
import org.savilusGame.enums.WorldGameTypes;

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
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    getImage();
  }

  private void getImage() {
    up1 = SpriteLoader.load(BAT_DOWN1, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(BAT_DOWN2, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(BAT_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(BAT_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(BAT_DOWN1, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(BAT_DOWN2, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(BAT_DOWN1, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(BAT_DOWN2, TILE_SIZE, TILE_SIZE);
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
    getRandomDirection(CHANGE_DIRECTION_INTERVAL);
  }
}
