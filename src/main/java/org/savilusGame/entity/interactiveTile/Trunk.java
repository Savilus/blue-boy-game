package org.savilusGame.entity.interactiveTile;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.TRUNK;

import org.savilusGame.GamePanel;
import org.savilusGame.utils.SpriteLoader;

public class Trunk extends InteractiveTile {

  public Trunk(GamePanel gamePanel, int col, int row) {
    super(gamePanel);

    this.worldX = TILE_SIZE * col;
    this.worldY = TILE_SIZE * row;

    mainImage = SpriteLoader.load(TRUNK, TILE_SIZE, TILE_SIZE);

    solidArea.x = 0;
    solidArea.y = 0;
    solidArea.width = 0;
    solidArea.height = 0;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
  }
}
