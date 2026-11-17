package org.savilusGame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.entity.Player;

/**
 * The slot-to-index mapping is the arithmetic the UI, the cursor and the capacity share; it has to
 * count in rows of {@link Player#INVENTORY_COLUMNS} for the item under the cursor to be the item used.
 */
class UIInventoryGridTest {

  private UI ui;

  @BeforeEach
  void setUp() {
    ui = new GamePanel().getUi();
  }

  @Test
  @DisplayName("the first row keeps the slot columns as indexes")
  void firstRowIsTheColumnIndex() {
    assertEquals(0, ui.getItemIndexFromInventory(0, 0));
    assertEquals(4, ui.getItemIndexFromInventory(4, 0));
  }

  @Test
  @DisplayName("every further row starts a full grid width later")
  void everyRowStartsAFullRowLater() {
    assertEquals(Player.INVENTORY_COLUMNS, ui.getItemIndexFromInventory(0, 1));
    assertEquals(9, ui.getItemIndexFromInventory(4, 1));
    assertEquals(2 * Player.INVENTORY_COLUMNS, ui.getItemIndexFromInventory(0, 2));
  }

  @Test
  @DisplayName("the last slot of the grid is the last inventory entry")
  void theLastSlotIsTheLastEntry() {
    assertEquals(Player.INVENTORY_COLUMNS * Player.INVENTORY_ROWS - 1,
        ui.getItemIndexFromInventory(Player.INVENTORY_COLUMNS - 1, Player.INVENTORY_ROWS - 1));
  }
}
