package org.savilusGame.entity.weapon;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.PICK_AXE;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.AttackProfile;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class PickAxe extends Item {

  public PickAxe(GamePanel gamePanel) {
    super(gamePanel, GameObject.PICKAXE);
    icon = SpriteLoader.load(PICK_AXE, TILE_SIZE, TILE_SIZE);
    attackValue = 1;
    attackProfile = new AttackProfile(30, 30, 10, 20);
    price = 55;
    description = String.format(TextManager.getItemDescription(GameObject.PICKAXE.getTextKey()), name);
    type = WorldGameTypes.PICKAXE;
    knockBackPower = 10;
  }
}
