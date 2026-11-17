package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.POWER_UP;
import static org.savilusGame.config.GameEntityNameFactory.RED_POTION;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class RedPotion extends Item {

  private static final String USE = "use";

  private final int healingValue = 5;

  public RedPotion(GamePanel gamePanel) {
    super(gamePanel, GameObject.RED_POTION);
    setDialogues(TextManager.getItemTexts(GameObject.RED_POTION.getTextKey(), name));
    type = WorldGameTypes.CONSUMABLE;
    stackable = true;
    icon = SpriteLoader.load(RED_POTION, TILE_SIZE, TILE_SIZE);
    description = String.format(TextManager.getItemDescription(GameObject.RED_POTION.getTextKey()), name, healingValue);
    price = 8;
  }

  @Override
  public boolean use(Actor user) {
    startDialogue(this, USE);
    user.setCurrentLife(user.getCurrentLife() + healingValue);
    gamePanel.playSE(POWER_UP);
    return true;
  }
}
