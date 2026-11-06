package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.MANA_CRYSTAL_BLANK;
import static org.savilusGame.config.GameEntityNameFactory.MANA_CRYSTAL_FULL;
import static org.savilusGame.config.GameEntityNameFactory.POWER_UP;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class ManaCrystal extends Item {

  private static final String MANA = "mana";
  private static final String UI_MESSAGES = "uiMessages";

  public ManaCrystal(GamePanel gamePanel) {
    super(gamePanel, GameObject.MANA_CRYSTAL);
    type = WorldGameTypes.PICK_UP;
    value = 1;
    icon = SpriteLoader.load(MANA_CRYSTAL_FULL, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public boolean use(Actor user) {
    gamePanel.playSE(POWER_UP);
    gamePanel.getUi().addMessage(TextManager.getUiText(UI_MESSAGES, MANA) + value);
    user.setMana(user.getMana() + value);
    return true;
  }
}
