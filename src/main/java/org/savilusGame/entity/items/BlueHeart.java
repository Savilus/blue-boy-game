package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BLUE_HEART;
import static org.savilusGame.enums.GameState.CUTSCENE_STATE;
import static org.savilusGame.enums.WorldGameTypes.PICK_UP;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class BlueHeart extends Item {

  public BlueHeart(GamePanel gamePanel) {
    super(gamePanel, GameObject.BLUE_HEART);
    type = PICK_UP;
    icon = SpriteLoader.load(BLUE_HEART, TILE_SIZE, TILE_SIZE);
    setDialogues(TextManager.getAllDialoguesForTarget(GameObject.BLUE_HEART.getTextKey()));
  }

  @Override
  public boolean use(Actor user) {
    gamePanel.setGameState(CUTSCENE_STATE);
    gamePanel.getCutsceneManager().setSceneNum(gamePanel.getCutsceneManager().getEnding());
    return true;
  }
}
