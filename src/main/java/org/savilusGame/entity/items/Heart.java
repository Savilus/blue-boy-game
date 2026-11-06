package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.HEART_BLANK;
import static org.savilusGame.config.GameEntityNameFactory.HEART_FULL;
import static org.savilusGame.config.GameEntityNameFactory.HEART_HALF;
import static org.savilusGame.config.GameEntityNameFactory.POWER_UP;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

public class Heart extends Item {

  private static final String LIFE = "life";
  private static final String UI_MESSAGES = "uiMessages";

  public Heart(GamePanel gamePanel) {
    super(gamePanel, GameObject.HEART);
    type = WorldGameTypes.PICK_UP;
    value = 2;
    icon = SpriteLoader.load(HEART_FULL, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public boolean use(Actor user) {
    gamePanel.playSE(POWER_UP);
    gamePanel.getUi().addMessage(TextManager.getUiText(UI_MESSAGES, LIFE) + value);
    user.setCurrentLife(user.getCurrentLife() + value);
    return true;
  }
}
