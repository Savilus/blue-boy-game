package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BOOTS;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.item.Item;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;

public class Boots extends Item {

  public Boots(GamePanel gamePanel) {
    super(gamePanel, GameObject.BOOTS);
    type = WorldGameTypes.PASSIVE;
    icon = sprites.get(BOOTS, TILE_SIZE, TILE_SIZE);
  }
}
