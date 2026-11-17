package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.KEY;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;
import static org.savilusGame.enums.WorldGameTypes.CONSUMABLE;
import static org.savilusGame.service.CollisionService.INIT_INDEX;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.utils.ObjectDetector;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Key extends Item {

  private static final String OPEN_DOOR_TEXT_KEY = "openDoor";
  private static final String WRONG_USE_TEXT_KEY = "wrongUse";

  public Key(GamePanel gamePanel) {
    super(gamePanel, GameObject.KEY);
    type = CONSUMABLE;
    icon = sprites.get(KEY, TILE_SIZE, TILE_SIZE);
    description = String.format(TextManager.getItemDescription(GameObject.KEY.getTextKey()), name);
    setDialogues(TextManager.getItemTexts(GameObject.KEY.getTextKey(), name));
    price = 1;
  }

  @Override
  public boolean use(Actor user) {
    var objects = gamePanel.getWorld().currentObjects();
    int objIndex = ObjectDetector.detect(user, objects, GameObject.DOOR);

    if (objIndex != INIT_INDEX) {
      if (objects == null || objIndex < 0 || objIndex >= objects.size()
          || objects.get(objIndex) == null || objects.get(objIndex).getGameObject() != GameObject.DOOR) {
        log.warn("Ignoring stale door collision index {}", objIndex);
        startDialogue(this, WRONG_USE_TEXT_KEY);
        return false;
      }
      startDialogue(this, OPEN_DOOR_TEXT_KEY);
      gamePanel.playSE(UNLOCK);
      objects.remove(objIndex);
      return true;
    } else {
      startDialogue(this, WRONG_USE_TEXT_KEY);
      return false;
    }
  }
}
