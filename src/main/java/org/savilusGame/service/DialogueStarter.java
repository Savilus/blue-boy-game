package org.savilusGame.service;

import static org.savilusGame.enums.GameState.CUTSCENE_STATE;
import static org.savilusGame.enums.GameState.DIALOG_STATE;

import org.savilusGame.GamePanel;

/** Shared start-of-conversation behaviour for all dialogue speakers. */
public final class DialogueStarter {

  private DialogueStarter() {
  }

  public static void start(GamePanel gamePanel, DialogueSpeaker speaker, String dialogueSet) {
    if (gamePanel.getGameState() != CUTSCENE_STATE) {
      gamePanel.setGameState(DIALOG_STATE);
    }
    gamePanel.getUi().setDialogueObject(speaker);
    speaker.setDialogueSet(dialogueSet);
  }
}
