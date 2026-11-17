package org.savilusGame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.items.BlueHeart;
import org.savilusGame.entity.items.RedPotion;

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

  @Test
  void pickupTransfersTheItemAndStacksEvenWhenAllSlotsAreFull() {
    var potion = new RedPotion(panel);
    assertTrue(player.canObtainItem(potion));
    assertSame(potion, player.getInventory().getFirst());
    while (player.getInventory().size() < Player.MAX_INVENTORY_SIZE) {
      assertTrue(player.canObtainItem(new BlueHeart(panel)));
    }
    var drop = new RedPotion(panel);
    drop.setStackCount(3);
    assertTrue(player.canObtainItem(drop));
    assertEquals(4, potion.getStackCount());
    assertEquals(Player.MAX_INVENTORY_SIZE, player.getInventory().size());
  }

  @Test
  void buyingTransfersOneUnitWithoutAliasingTheMerchantStock() {
    var stock = new RedPotion(panel);
    stock.setStackCount(5);
    assertTrue(player.obtainFromStock(stock));
    assertTrue(player.obtainFromStock(stock));
    var purchased = player.getInventory().getFirst();
    assertNotSame(stock, purchased);
    assertSame(stock.getIcon(), purchased.getIcon());
    assertEquals(2, purchased.getStackCount());
    assertEquals(5, stock.getStackCount());
  }
}
