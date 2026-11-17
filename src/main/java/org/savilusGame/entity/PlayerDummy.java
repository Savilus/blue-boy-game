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
import org.savilusGame.utils.SpriteLoader;

public class PlayerDummy extends Actor {
  private static final String NAME = "PlayerDummy";

  public PlayerDummy(GamePanel gamePanel) {
    super(gamePanel);
    name = NAME;
    getImage();
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
}
