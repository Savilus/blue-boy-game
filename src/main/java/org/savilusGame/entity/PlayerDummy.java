package org.savilusGame.entity;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BOY_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.BOY_UP1;
import static org.savilusGame.config.GameEntityNameFactory.BOY_UP2;

import org.savilusGame.GamePanel;

public class PlayerDummy extends Actor {
  private static final String NAME = "PlayerDummy";

  public PlayerDummy(GamePanel gamePanel) {
    super(gamePanel);
    name = NAME;
    getImage();
  }

  public void getImage() {
    up1 = sprites.get(BOY_UP1, TILE_SIZE, TILE_SIZE);
    up2 = sprites.get(BOY_UP2, TILE_SIZE, TILE_SIZE);
    down1 = sprites.get(BOY_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = sprites.get(BOY_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = sprites.get(BOY_LEFT1, TILE_SIZE, TILE_SIZE);
    up1 = sprites.get(BOY_UP1, TILE_SIZE, TILE_SIZE);
    left2 = sprites.get(BOY_LEFT2, TILE_SIZE, TILE_SIZE);
    right1 = sprites.get(BOY_RIGHT1, TILE_SIZE, TILE_SIZE);
    right2 = sprites.get(BOY_RIGHT2, TILE_SIZE, TILE_SIZE);
  }
}
