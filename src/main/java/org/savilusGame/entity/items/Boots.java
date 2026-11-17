package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BOOTS;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;

public class Boots extends Item {

  public Boots(GamePanel gamePanel) {
    super(gamePanel, GameObject.BOOTS);
    icon = SpriteLoader.load(BOOTS, TILE_SIZE, TILE_SIZE);
  }
}
