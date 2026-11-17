package org.savilusGame.entity.item;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.Map;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.service.DialogueSpeaker;
import org.savilusGame.service.DialogueStarter;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * An inventory/world item: pure data plus a {@link #use(Actor)} hook.
 * <p>Not an entity: no position, no physics, no sprite frames — the world drop representation
 * is {@link ItemDrop}. {@code id} is the stable (locale-independent) identity of the item,
 * derived from {@link GameObject#name()}; {@code name} is the localized display string.
 */
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PROTECTED)
public class Item implements DialogueSpeaker {

  private static final String EVENT_MESSAGES = "eventMessages";

  final GamePanel gamePanel;
  private final String id;
  String name;
  String description;
  int value;
  int price;
  int stackCount = 1;
  boolean stackable = false;
  WorldGameTypes type;
  BufferedImage icon;
  int attackValue;
  int defenseValue;
  int knockBackPower;
  int lightRadius;
  AttackProfile attackProfile;

  // DIALOGUE STATE
  private Map<String, List<String>> dialogues;
  private String dialogueSet = org.apache.commons.lang3.StringUtils.EMPTY;
  private int dialogueIndex = 0;

  protected Item(GamePanel gamePanel, GameObject gameObject) {
    this.gamePanel = gamePanel;
    this.id = gameObject.name();
    this.name = gameObject.getName();
    this.setDialogues(TextManager.getAllDialoguesForTarget(EVENT_MESSAGES));
  }

  /**
   * Equip/consume effect, applied by the caller (player pickup / ui item use).
   *
   * @return {@code true} when the item applied its effect (and should be consumed from the world)
   */
  public boolean use(Actor user) {
    return true;
  }

  /** Identity of two item instances of the same kind, independent of locale or stack size. */
  public boolean sameKind(Item other) {
    return other != null && id.equals(other.id);
  }

  @Override
  public void startDialogue(DialogueSpeaker speaker, String dialogueSet) {
    DialogueStarter.start(gamePanel, speaker, dialogueSet);
  }
}
