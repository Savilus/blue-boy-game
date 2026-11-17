package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.LANTERN;
import static org.savilusGame.enums.WorldGameTypes.LIGHTING;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class Lantern extends Item {

  public Lantern(GamePanel gamePanel) {
    super(gamePanel, GameObject.LANTERN);

    type = LIGHTING;
    icon = sprites.get(LANTERN, TILE_SIZE, TILE_SIZE);
    description = String.format(TextManager.getItemDescription(GameObject.LANTERN.getTextKey()), name);
    price = 200;
    lightRadius = 450;
  }
}
