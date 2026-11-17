package org.savilusGame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.items.BlueHeart;

/**
 * One authoritative inventory limit: {@link Player#MAX_INVENTORY_SIZE} is derived from the grid the
 * UI draws, so the capacity and the visible slots cannot drift apart.
 */
class PlayerInventoryCapacityTest {

  private GamePanel panel;
  private Player player;

  @BeforeEach
  void setUp() {
    panel = new GamePanel();
    player = panel.getPlayer();
    player.getInventory().clear();
  }

  @Test
  @DisplayName("the limit is the number of slots the grid shows")
  void theLimitIsTheGridSize() {
    assertEquals(Player.INVENTORY_COLUMNS * Player.INVENTORY_ROWS, Player.MAX_INVENTORY_SIZE);
  }

  @Test
  @DisplayName("the inventory accepts exactly MAX_INVENTORY_SIZE items and then refuses more")
  void theInventoryAcceptsExactlyTheCapacity() {
    for (int slot = 0; slot < Player.MAX_INVENTORY_SIZE; slot++) {
      assertTrue(player.canObtainItem(new BlueHeart(panel)), "slot " + slot + " must still accept an item");
    }

    assertEquals(Player.MAX_INVENTORY_SIZE, player.getInventory().size());
    assertFalse(player.canObtainItem(new BlueHeart(panel)), "a full inventory must refuse a new item");
  }
}
