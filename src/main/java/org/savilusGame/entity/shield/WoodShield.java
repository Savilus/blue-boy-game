package org.savilusGame.entity.shield;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.WOODEN_SHIELD;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class WoodShield extends Item {

  public WoodShield(GamePanel gamePanel) {
    super(gamePanel, GameObject.WOOD_SHIELD);
    icon = SpriteLoader.load(WOODEN_SHIELD, TILE_SIZE, TILE_SIZE);
    defenseValue = 1;
    description = String.format(TextManager.getItemDescription(GameObject.WOOD_SHIELD.getTextKey()), name);
    type = WorldGameTypes.SHIELD;
    price = 10;

  }
}
