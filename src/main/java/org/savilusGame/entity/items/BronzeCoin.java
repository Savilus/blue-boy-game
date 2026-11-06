package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BRONZE_COIN;
import static org.savilusGame.config.GameEntityNameFactory.COIN;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;

public class BronzeCoin extends Item {
  private static final String COIN_TEXT = "Coin ";

  public BronzeCoin(GamePanel gamePanel) {
    super(gamePanel, GameObject.BRONZE_COIN);

    type = WorldGameTypes.PICK_UP;
    value = 5;
    icon = SpriteLoader.load(BRONZE_COIN, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public boolean use(Actor user) {
    gamePanel.playSE(COIN);
    gamePanel.getUi().addMessage(COIN_TEXT + value);
    gamePanel.getPlayer().setMoney(gamePanel.getPlayer().getMoney() + value);
    return true;
  }
}
