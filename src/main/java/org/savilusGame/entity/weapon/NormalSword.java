package org.savilusGame.entity.weapon;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.NORMAL_SWORD;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.AttackProfile;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class NormalSword extends Item {

  public NormalSword(GamePanel gamePanel) {
    super(gamePanel, GameObject.NORMAL_SWORD);
    icon = SpriteLoader.load(NORMAL_SWORD, TILE_SIZE, TILE_SIZE);
    attackValue = 1;
    attackProfile = new AttackProfile(36, 36, 5, 25);
    description = String.format(TextManager.getItemDescription(GameObject.NORMAL_SWORD.getTextKey()), name);
    type = WorldGameTypes.SWORD;
    price = 15;
    knockBackPower = 2;
  }
}
