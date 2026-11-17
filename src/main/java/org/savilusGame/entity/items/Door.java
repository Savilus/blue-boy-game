package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DOOR;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class Door extends InteractiveObject {

  private static final String DOOR_DIALOGUE_KEY = "door";
  private static final String OPEN_INFORMATION = "openInformation";

  public Door(GamePanel gamePanel) {
    super(gamePanel);
    name = GameObject.DOOR.name();
    mainImage = SpriteLoader.load(DOOR, TILE_SIZE, TILE_SIZE);
    collision = true;

    solidArea.x = 0;
    solidArea.y = 16;
    solidArea.width = 48;
    solidArea.height = 32;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
    setDialogues(TextManager.getItemTexts(DOOR_DIALOGUE_KEY));
  }

  @Override
  public void interact() {
    startDialogue(this, OPEN_INFORMATION);
  }
}
