package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.IRON_DOOR;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class IronDoor extends InteractiveObject {

  private static final String IRON_DOOR_DIALOGUE_KEY = "ironDoor";
  private static final String OPEN_INFORMATION = "openInformation";

  public IronDoor(GamePanel gamePanel) {
    super(gamePanel);
    name = GameObject.IRON_DOOR.getName();
    mainImage = SpriteLoader.load(IRON_DOOR, TILE_SIZE, TILE_SIZE);
    collision = true;

    solidArea.x = 0;
    solidArea.y = 16;
    solidArea.width = 48;
    solidArea.height = 32;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    setDialogues(TextManager.getItemTexts(IRON_DOOR_DIALOGUE_KEY));
  }

  @Override
  public void interact() {
    startDialogue(this, OPEN_INFORMATION);
  }
}
