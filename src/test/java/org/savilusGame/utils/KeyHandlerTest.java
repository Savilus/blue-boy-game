package org.savilusGame.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;
import static org.savilusGame.enums.GameState.CHARACTER_STATE;
import static org.savilusGame.enums.GameState.PLAY_STATE;
import static org.savilusGame.enums.GameState.TITLE_STATE;
import static org.savilusGame.enums.SubState.PLAYER_INVENTORY;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.config.DevMode;
import org.savilusGame.entity.Player;

/** Release-package input rules: cheats need {@link DevMode}, quitting uses the single exit path. */
class KeyHandlerTest {

  private static final class RecordingGamePanel extends GamePanel {
    private int quitRequests;

    @Override
    public void quitGame() {
      quitRequests++;
    }

    @Override
    public void playSE(String soundId) {
    }

    int quitRequests() {
      return quitRequests;
    }
  }

  private RecordingGamePanel panel;

  @BeforeEach
  void setUp() {
    panel = new RecordingGamePanel();
    panel.setGameState(PLAY_STATE);
  }

  private KeyEvent pressed(int keyCode) {
    return new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L, 0, keyCode, KeyEvent.CHAR_UNDEFINED);
  }

  @Test
  @DisplayName("god mode and the debug overlay stay inactive in release builds")
  void developmentKeysAreIgnoredWithoutDevMode() {
    KeyHandler releaseInput = new KeyHandler(panel, new DevMode(false));

    releaseInput.keyPressed(pressed(KeyEvent.VK_G));
    releaseInput.keyPressed(pressed(KeyEvent.VK_T));

    assertFalse(releaseInput.isGodModeOn());
    assertFalse(releaseInput.isShowDebugText());
  }

  @Test
  @DisplayName("development builds can still toggle god mode and the debug overlay")
  void developmentKeysWorkWithDevMode() {
    KeyHandler developerInput = new KeyHandler(panel, new DevMode(true));

    developerInput.keyPressed(pressed(KeyEvent.VK_G));
    developerInput.keyPressed(pressed(KeyEvent.VK_T));

    assertTrue(developerInput.isGodModeOn());
    assertTrue(developerInput.isShowDebugText());
  }

  @Test
  @DisplayName("Control toggles path visualization only in development mode")
  void pathVisualizationRequiresDevMode() {
    KeyHandler releaseInput = new KeyHandler(panel, new DevMode(false));
    releaseInput.keyPressed(pressed(KeyEvent.VK_CONTROL));
    assertFalse(panel.getTileManager().isPathVisible());

    KeyHandler developerInput = new KeyHandler(panel, new DevMode(true));
    developerInput.keyPressed(pressed(KeyEvent.VK_CONTROL));
    assertTrue(panel.getTileManager().isPathVisible());

    developerInput.keyPressed(pressed(KeyEvent.VK_CONTROL));
    assertFalse(panel.getTileManager().isPathVisible());
  }

  @Test
  @DisplayName("the plus key grants one level only in development mode")
  void plusKeyGrantsOneLevelInDevMode() {
    Player player = panel.getPlayer();
    player.setLevel(1);
    player.setExp(0);
    player.setNextLevelExp(5);
    KeyHandler developerInput = new KeyHandler(panel, new DevMode(true));

    developerInput.keyPressed(pressed(KeyEvent.VK_ADD));

    assertEquals(2, player.getLevel());
    assertEquals(0, player.getExp());
    assertEquals(10, player.getNextLevelExp());
  }

  @Test
  @DisplayName("the shifted equals key grants a level and release mode ignores plus")
  void shiftedEqualsIsPlusAndReleaseModeIgnoresIt() {
    Player player = panel.getPlayer();
    player.setLevel(1);
    player.setExp(0);
    player.setNextLevelExp(5);
    KeyHandler releaseInput = new KeyHandler(panel, new DevMode(false));

    releaseInput.keyPressed(new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L,
        InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_EQUALS, '+'));
    assertEquals(1, player.getLevel());

    new KeyHandler(panel, new DevMode(true)).keyPressed(
        new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L,
            InputEvent.SHIFT_DOWN_MASK, KeyEvent.VK_EQUALS, '+'));

    assertEquals(2, player.getLevel());
  }

  @Test
  @DisplayName("reloading the current map is a development-only action")
  void mapReloadRequiresDevMode() {
    panel.getTileManager().getGameMaps().remove(MAIN_MAP);

    new KeyHandler(panel, new DevMode(false)).keyPressed(pressed(KeyEvent.VK_R));
    assertFalse(panel.getTileManager().getGameMaps().containsKey(MAIN_MAP),
        "Release input must not reload maps");

    new KeyHandler(panel, new DevMode(true)).keyPressed(pressed(KeyEvent.VK_R));
    assertTrue(panel.getTileManager().getGameMaps().containsKey(MAIN_MAP));
  }

  @Test
  @DisplayName("the title menu quits through the game's single exit path")
  void titleMenuQuitUsesTheSingleExitPath() {
    panel.setGameState(TITLE_STATE);
    panel.getUi().setCommandNum(2);

    panel.getKeyHandler().keyPressed(pressed(KeyEvent.VK_ENTER));

    assertEquals(1, panel.quitRequests(), "Quit must be routed through GamePanel.quitGame()");
  }

  @Test
  @DisplayName("the inventory cursor stops at the last slot of the grid the UI draws")
  void inventoryCursorStopsAtTheLastSlot() {
    panel.setGameState(CHARACTER_STATE);
    panel.getUi().setSubState(PLAYER_INVENTORY);
    panel.getUi().setPlayerSlotCol(0);
    panel.getUi().setPlayerSlotRow(0);
    KeyHandler input = new KeyHandler(panel, new DevMode(false));

    for (int press = 0; press < Player.INVENTORY_COLUMNS + 3; press++) {
      input.keyPressed(pressed(KeyEvent.VK_D));
    }
    for (int press = 0; press < Player.INVENTORY_ROWS + 3; press++) {
      input.keyPressed(pressed(KeyEvent.VK_S));
    }

    assertEquals(Player.INVENTORY_COLUMNS - 1, panel.getUi().getPlayerSlotCol());
    assertEquals(Player.INVENTORY_ROWS - 1, panel.getUi().getPlayerSlotRow());
  }
}
