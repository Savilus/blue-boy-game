package org.savilusGame.entity.items;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.CHEST;
import static org.savilusGame.config.GameEntityNameFactory.CHEST_OPENED;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.item.Item;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.utils.text.TextManager;

import lombok.Getter;

@Getter
public class Chest extends InteractiveObject {

  private static final String FULL_INVENTORY_TEXT_KEY = "fullInventory";
  private static final String COLLECT_ITEM_TEXT_KEY = "collectItem";
  private static final String EMPTY_TEXT_KEY = "empty";

  public Chest(GamePanel gamePanel) {
    super(gamePanel);
    name = GameObject.CHEST.getName();
    mainImage = SpriteLoader.load(CHEST, TILE_SIZE, TILE_SIZE);
    mainImage2 = SpriteLoader.load(CHEST_OPENED, TILE_SIZE, TILE_SIZE);
    collision = true;

    solidArea.x = 4;
    solidArea.y = 16;
    solidArea.width = 40;
    solidArea.height = 32;
    solidAreaDefaultX = solidArea.x;
    solidAreaDefaultY = solidArea.y;
  }

  public Chest setLoot(Item loot) {
    this.loot = loot;
    return this;
  }

  @Override
  public void interact() {
    if (!opened) {
      setDialogues(TextManager.getItemTexts(GameObject.CHEST.getTextKey(), this.loot.getName()));
      gamePanel.playSE(UNLOCK);
      if (!gamePanel.getPlayer().canObtainItem(loot)) {
        startDialogue(this, FULL_INVENTORY_TEXT_KEY);
      } else {
        startDialogue(this, COLLECT_ITEM_TEXT_KEY);
        mainImage = mainImage2;
        opened = true;
      }
    } else {
      startDialogue(this, EMPTY_TEXT_KEY);
    }
  }
}
