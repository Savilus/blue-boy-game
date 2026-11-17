package org.savilusGame.entity.interactiveTile;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.METAL_PLATE;

import org.savilusGame.GamePanel;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class MetalPlate extends InteractiveTile {

  public MetalPlate(GamePanel gamePanel, int col, int row) {
    super(gamePanel);
    gameObject = GameObject.METAL_PLATE;

    this.worldX = TILE_SIZE * col;
    this.worldY = TILE_SIZE * row;

    name = TextManager.getItemName(GameObject.METAL_PLATE.getTextKey());
    mainImage = sprites.get(METAL_PLATE, TILE_SIZE, TILE_SIZE);

    solidArea.x = 0;
    solidArea.y = 0;
    solidArea.width = 0;
    solidArea.height = 0;
  }
}
