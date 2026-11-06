package org.savilusGame.utils;

import static org.savilusGame.config.GameEntityNameFactory.CURSOR;
import static org.savilusGame.config.GameEntityNameFactory.OUTSIDE_MUSIC;
import static org.savilusGame.enums.GameState.CHARACTER_STATE;
import static org.savilusGame.enums.GameState.MAP_STATE;
import static org.savilusGame.enums.GameState.OPTIONS_STATE;
import static org.savilusGame.enums.GameState.PAUSE_STATE;
import static org.savilusGame.enums.GameState.PLAY_STATE;
import static org.savilusGame.enums.GameState.TITLE_STATE;
import static org.savilusGame.enums.SubState.DEFAULT;
import static org.savilusGame.enums.SubState.MAIN_SETTINGS;
import static org.savilusGame.enums.SubState.NPC_INVENTORY;
import static org.savilusGame.enums.SubState.PLAYER_INVENTORY;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import org.savilusGame.GamePanel;
import org.savilusGame.Sound;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class KeyHandler implements KeyListener {

  final GamePanel gamePanel;
  boolean upPressed, downPressed, leftPressed, rightPressed, enterPressed, spacePressed, shotKeyPressed;
  //DEBUG
  boolean showDebugText;
  boolean godModeOn = false;

  private void titleState(int code) {
    switch (code) {
      case KeyEvent.VK_W, KeyEvent.VK_UP -> {
        upPressed = true;
        navigateCommandUp();
        if (gamePanel.getUi().getCommandNum() < 0) {
          gamePanel.getUi().setCommandNum(2);
        }
      }
      case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
        downPressed = true;
        navigateCommandDown();
        if (gamePanel.getUi().getCommandNum() > 2) {
          gamePanel.getUi().setCommandNum(0);
        }
      }
      case KeyEvent.VK_ENTER -> {
        if (gamePanel.getUi().getCommandNum() == 0) {
          gamePanel.setGameState(PLAY_STATE);
          gamePanel.playBGM(OUTSIDE_MUSIC);
        }
        if (gamePanel.getUi().getCommandNum() == 1) {
          gamePanel.getSaveLoad().load();
          gamePanel.setGameState(PLAY_STATE);
          gamePanel.playBGM(OUTSIDE_MUSIC);
        }
        if (gamePanel.getUi().getCommandNum() == 2) {
          System.exit(0);
        }
      }
    }
  }

  private void pauseState(int code) {
    if (code == KeyEvent.VK_P) {
      gamePanel.setGameState(PLAY_STATE);
    }
  }

  private void playState(int code) {
    switch (code) {
      case KeyEvent.VK_W -> upPressed = true;
      case KeyEvent.VK_S -> downPressed = true;
      case KeyEvent.VK_A -> leftPressed = true;
      case KeyEvent.VK_D -> rightPressed = true;
      case KeyEvent.VK_SPACE -> spacePressed = true;
      case KeyEvent.VK_P -> gamePanel.setGameState(PAUSE_STATE);
      case KeyEvent.VK_ENTER -> enterPressed = true;
      case KeyEvent.VK_C -> gamePanel.setGameState(CHARACTER_STATE);
      case KeyEvent.VK_F -> shotKeyPressed = true;
      case KeyEvent.VK_ESCAPE -> {
        gamePanel.setGameState(OPTIONS_STATE);
        gamePanel.getUi().setSubState(MAIN_SETTINGS);
      }
      case KeyEvent.VK_M -> gamePanel.setGameState(MAP_STATE);
      case KeyEvent.VK_X -> gamePanel.getGameMap().setMiniMapOn(!gamePanel.getGameMap().isMiniMapOn());
      case KeyEvent.VK_G -> godModeOn = !godModeOn;

      //DEBUG
      case KeyEvent.VK_T -> showDebugText = !showDebugText;
      //REFRESH MAP
      case KeyEvent.VK_R -> gamePanel.getTileManager().loadMap(gamePanel.getWorld().getCurrentMap());
    }
  }

  private void dialogState(int code) {
    if (code == KeyEvent.VK_ENTER) {
      enterPressed = true;
    }
  }

  private void characterState(int code) {
    switch (code) {
      case KeyEvent.VK_C -> gamePanel.setGameState(PLAY_STATE);
      case KeyEvent.VK_ENTER -> gamePanel.getPlayer().selectItem();
    }
    playerInventory(code);
  }

  private void playerInventory(int code) {
    switch (code) {
      case KeyEvent.VK_W, KeyEvent.VK_UP -> {
        if (gamePanel.getUi().getPlayerSlotRow() != 0) {
          gamePanel.getUi().setPlayerSlotRow(gamePanel.getUi().getPlayerSlotRow() - 1);
          gamePanel.playSE(CURSOR);
        }
      }
      case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
        if (gamePanel.getUi().getPlayerSlotCol() != 0) {
          gamePanel.getUi().setPlayerSlotCol(gamePanel.getUi().getPlayerSlotCol() - 1);
          gamePanel.playSE(CURSOR);
        }
      }
      case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
        if (gamePanel.getUi().getPlayerSlotRow() != 3) {
          gamePanel.getUi().setPlayerSlotRow(gamePanel.getUi().getPlayerSlotRow() + 1);
          gamePanel.playSE(CURSOR);
        }
      }
      case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
        if (gamePanel.getUi().getPlayerSlotCol() != 4) {
          gamePanel.getUi().setPlayerSlotCol(gamePanel.getUi().getPlayerSlotCol() + 1);
          gamePanel.playSE(CURSOR);
        }
      }
    }
  }

  private void npcInventory(int code) {
    switch (code) {
      case KeyEvent.VK_W, KeyEvent.VK_UP -> {
        if (gamePanel.getUi().getNpcSlotRow() != 0) {
          gamePanel.getUi().setNpcSlotRow(gamePanel.getUi().getNpcSlotRow() - 1);
          gamePanel.playSE(CURSOR);
        }
      }
      case KeyEvent.VK_A, KeyEvent.VK_LEFT -> {
        if (gamePanel.getUi().getNpcSlotCol() != 0) {
          gamePanel.getUi().setNpcSlotCol(gamePanel.getUi().getNpcSlotCol() - 1);
          gamePanel.playSE(CURSOR);
        }
      }
      case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
        if (gamePanel.getUi().getNpcSlotRow() != 3) {
          gamePanel.getUi().setNpcSlotRow(gamePanel.getUi().getNpcSlotRow() + 1);
          gamePanel.playSE(CURSOR);
        }
      }
      case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> {
        if (gamePanel.getUi().getNpcSlotCol() != 4) {
          gamePanel.getUi().setNpcSlotCol(gamePanel.getUi().getNpcSlotCol() + 1);
          gamePanel.playSE(CURSOR);
        }
      }
    }
  }

  private void tradeState(int code) {
    if (code == KeyEvent.VK_ENTER) {
      enterPressed = true;
    }
    if (gamePanel.getUi().getSubState() == DEFAULT) {
      switch (code) {
        case KeyEvent.VK_W -> {
          navigateCommandUp();
          if (gamePanel.getUi().getCommandNum() < 0) {
            gamePanel.getUi().setCommandNum(2);
          }
          gamePanel.playSE(CURSOR);
        }
        case KeyEvent.VK_S -> {
          navigateCommandDown();
          if (gamePanel.getUi().getCommandNum() > 2) {
            gamePanel.getUi().setCommandNum(0);
          }
          gamePanel.playSE(CURSOR);
        }
      }
    } else if (gamePanel.getUi().getSubState() == NPC_INVENTORY) {
      npcInventory(code);
      if (code == KeyEvent.VK_ESCAPE) {
        gamePanel.getUi().setSubState(DEFAULT);
      }
    } else if (gamePanel.getUi().getSubState() == PLAYER_INVENTORY) {
      playerInventory(code);
      if (code == KeyEvent.VK_ESCAPE) {
        gamePanel.getUi().setSubState(DEFAULT);

      }
    }
  }

  private void gameOverState(int code) {
    switch (code) {
      case KeyEvent.VK_W, KeyEvent.VK_UP -> {
        navigateCommandUp();
        if (gamePanel.getUi().getCommandNum() < 0) {
          gamePanel.getUi().setCommandNum(1);
        }
        gamePanel.playSE(CURSOR);
      }
      case KeyEvent.VK_S, KeyEvent.VK_DOWN -> {
        navigateCommandDown();
        if (gamePanel.getUi().getCommandNum() > 1) {
          gamePanel.getUi().setCommandNum(0);
        }
        gamePanel.playSE(CURSOR);
      }
      case KeyEvent.VK_ENTER -> {
        if (gamePanel.getUi().getCommandNum() == 0) {
          gamePanel.setGameState(PLAY_STATE);
          gamePanel.resetGame(false);
        } else if (gamePanel.getUi().getCommandNum() == 1) {
          gamePanel.setGameState(TITLE_STATE);
          gamePanel.resetGame(true);
        }
      }
    }
  }

  private void mapState(int code) {
    if (code == KeyEvent.VK_M) {
      gamePanel.setGameState(PLAY_STATE);
    }
  }

  private void optionState(int code) {
    int maxCommandNum = switch (gamePanel.getUi().getSubState()) {
      case MAIN_SETTINGS -> 5;
      case END_GAME -> 1;
      default -> 0;
    };

    switch (code) {
      case KeyEvent.VK_ESCAPE -> {
        gamePanel.setGameState(PLAY_STATE);
        gamePanel.getUi().setSubState(DEFAULT);
      }
      case KeyEvent.VK_ENTER -> enterPressed = true;
      case KeyEvent.VK_W -> {
        navigateCommandUp();
        gamePanel.playSE(CURSOR);
        if (gamePanel.getUi().getCommandNum() < 0) {
          gamePanel.getUi().setCommandNum(maxCommandNum);
        }
      }
      case KeyEvent.VK_S -> {
        navigateCommandDown();
        gamePanel.playSE(CURSOR);
        if (gamePanel.getUi().getCommandNum() > maxCommandNum) {
          gamePanel.getUi().setCommandNum(0);
        }
      }
      case KeyEvent.VK_A, KeyEvent.VK_D -> handleVolumeAdjustment(code == KeyEvent.VK_D);
    }
  }

  private void handleVolumeAdjustment(boolean increase) {
    if (gamePanel.getUi().getSubState() != MAIN_SETTINGS) return;

    int command = gamePanel.getUi().getCommandNum();

    if (command == 1) {
      adjustVolume(gamePanel.getMusic(), increase);
      gamePanel.getMusic().checkVolume();
    } else if (command == 2) {
      adjustVolume(gamePanel.getSoundEffect(), increase);
    }
  }

  private void adjustVolume(Sound audio, boolean increase) {
    int current = audio.getVolumeScale();
    int newVolume = increase ? Math.min(current + 1, 5) : Math.max(current - 1, 0);
    if (newVolume != current) {
      audio.setVolumeScale(newVolume);
      gamePanel.playSE(CURSOR);
    }
  }

  private void navigateCommandUp() {
    gamePanel.getUi().setCommandNum(gamePanel.getUi().getCommandNum() - 1);
  }

  private void navigateCommandDown() {
    gamePanel.getUi().setCommandNum(gamePanel.getUi().getCommandNum() + 1);
  }

  @Override
  public void keyTyped(KeyEvent keyEvent) {
  }

  @Override
  public void keyPressed(KeyEvent keyEvent) {
    int code = keyEvent.getKeyCode();

    switch (gamePanel.getGameState()) {
      case PLAY_STATE -> playState(code);
      case PAUSE_STATE -> pauseState(code);
      case DIALOG_STATE,
           CUTSCENE_STATE -> dialogState(code);
      case TITLE_STATE -> titleState(code);
      case CHARACTER_STATE -> characterState(code);
      case OPTIONS_STATE -> optionState(code);
      case GAME_OVER_STATE -> gameOverState(code);
      case TRADE_STATE -> tradeState(code);
      case MAP_STATE -> mapState(code);
    }
  }

  @Override
  public void keyReleased(KeyEvent e) {
    int code = e.getKeyCode();
    switch (code) {
      case KeyEvent.VK_W -> upPressed = false;
      case KeyEvent.VK_S -> downPressed = false;
      case KeyEvent.VK_A -> leftPressed = false;
      case KeyEvent.VK_D -> rightPressed = false;
      case KeyEvent.VK_F -> shotKeyPressed = false;
      case KeyEvent.VK_SPACE -> spacePressed = false;
      case KeyEvent.VK_ENTER -> enterPressed = false;
    }
  }
}
