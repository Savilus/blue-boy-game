package org.savilusGame.entity.shield;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BLUE_SHIELD;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class BlueShield extends Item {

  public BlueShield(GamePanel gamePanel) {
    super(gamePanel, GameObject.BLUE_SHIELD);
    icon = SpriteLoader.load(BLUE_SHIELD, TILE_SIZE, TILE_SIZE);
    defenseValue = 2;
    description = String.format(TextManager.getItemDescription(GameObject.BLUE_SHIELD.getTextKey()), name);
    type = WorldGameTypes.SHIELD;
    price = 40;
  }
}
