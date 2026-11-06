package org.savilusGame.service;

import java.util.List;
import java.util.Map;

/**
 * Something that can speak (NPCs, monsters, player, items, interactive objects).
 * <p>Implementations keep the full dialogue text map and a pointer into the active
 * dialogue set, mirrored onto {@code GamePanel.getUi()} when a conversation starts.
 */
public interface DialogueSpeaker {

  Map<String, List<String>> getDialogues();

  void setDialogues(Map<String, List<String>> dialogues);

  String getDialogueSet();

  void setDialogueSet(String dialogueSet);

  int getDialogueIndex();

  void setDialogueIndex(int dialogueIndex);

  void startDialogue(DialogueSpeaker speaker, String dialogueSet);
}
