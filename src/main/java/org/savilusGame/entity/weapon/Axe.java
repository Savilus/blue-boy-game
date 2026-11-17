package org.savilusGame.entity.weapon;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.AXE;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.AttackProfile;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class Axe extends Item {

  public Axe(GamePanel gamePanel) {
    super(gamePanel, GameObject.AXE);
    icon = SpriteLoader.load(AXE, TILE_SIZE, TILE_SIZE);
    attackValue = 2;
    attackProfile = new AttackProfile(30, 30, 25, 45);
    price = 40;
    description = String.format(TextManager.getItemDescription(GameObject.AXE.getTextKey()), name);
    type = WorldGameTypes.AXE;
    knockBackPower = 6;
  }
}
