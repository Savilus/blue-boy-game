package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.SLEEP;
import static org.savilusGame.config.GameEntityNameFactory.TENT;
import static org.savilusGame.enums.GameState.SLEEP_STATE;
import static org.savilusGame.enums.WorldGameTypes.CONSUMABLE;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class Tent extends Item {

  public Tent(GamePanel gamePanel) {
    super(gamePanel, GameObject.TENT);
    type = CONSUMABLE;
    icon = SpriteLoader.load(TENT, TILE_SIZE, TILE_SIZE);
    description = String.format(TextManager.getItemDescription(GameObject.TENT.getTextKey()), name);
    price = 40;
    stackable = true;
  }

  @Override
  public boolean use(Actor user) {
    gamePanel.setGameState(SLEEP_STATE);
    gamePanel.playSE(SLEEP);
    gamePanel.getPlayer().setCurrentLife(gamePanel.getPlayer().getMaxLife());
    gamePanel.getPlayer().setMana(gamePanel.getPlayer().getMaxMana());
    gamePanel.getPlayer().setSleepingImage(icon);
    return true;
  }
}
