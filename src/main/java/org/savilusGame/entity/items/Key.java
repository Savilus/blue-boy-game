package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.KEY;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;
import static org.savilusGame.enums.WorldGameTypes.CONSUMABLE;
import static org.savilusGame.utils.CollisionDetector.INIT_INDEX;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.ObjectDetector;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class Key extends Item {

  private static final String OPEN_DOOR_TEXT_KEY = "openDoor";
  private static final String WRONG_USE_TEXT_KEY = "wrongUse";

  public Key(GamePanel gamePanel) {
    super(gamePanel, GameObject.KEY);
    type = CONSUMABLE;
    stackable = true;
        icon = SpriteLoader.load(KEY, TILE_SIZE, TILE_SIZE);
    description = String.format(TextManager.getItemDescription(GameObject.KEY.getTextKey()), name);
    setDialogues(TextManager.getItemTexts(GameObject.KEY.getTextKey(), name));
    price = 1;
  }

  @Override
  public boolean use(Actor user) {
    int objIndex = ObjectDetector.detect(user, gamePanel.getWorld().currentObjects(), GameObject.DOOR.name());

    if (objIndex != INIT_INDEX) {
      startDialogue(this, OPEN_DOOR_TEXT_KEY);
      gamePanel.playSE(UNLOCK);
      gamePanel.getWorld().currentObjects().remove(objIndex);
      return true;
    } else {
      startDialogue(this, WRONG_USE_TEXT_KEY);
      return false;
    }
  }
}
