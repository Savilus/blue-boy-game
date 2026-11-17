package org.savilusGame.utils.text;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@UtilityClass
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TextManager {
  static String DESCRIPTION = "description";
  static String USE = "use";
  static String OPEN_DOOR = "openDoor";
  static String COLLECT_ITEM = "collectItem";
  static String LVL_UP = "lvlUp";
  static String NAME = "name";
  static String EVENT_MESSAGES_KEY = "eventMessages";

  // Loaded once, at class initialisation, from the classpath (see TextLoader). A packaging or
  // data problem therefore fails immediately with the offending resource name.
  @Getter
  Map<String, Map<String, List<String>>> dialogues = TextLoader.loadAllDialogues();
  @Getter
  Map<String, Map<String, String>> items = TextLoader.loadItemsInformation();
  @Getter
  Map<String, Map<String, String>> settingsAndUI = TextLoader.loadSettingsAndUITexts();

  // Keyed by the textKey values of GameObject.CHEST / RED_POTION / KEY (kept as literals on
  // purpose: the keys are an API contract with the bundled texts and must match exactly).
  static Map<String, String> FORMAT_KEYS = Map.of(
      "chest", COLLECT_ITEM,
      "redPotion", USE,
      "key", OPEN_DOOR,
      EVENT_MESSAGES_KEY, LVL_UP);

  public String getSettingText(String settingField, String settingName) {
    return requireText(requireGroup(settingsAndUI, settingField), settingField, settingName);
  }

  public String getUiText(String uiField, String uiTextKey) {
    return requireText(requireGroup(settingsAndUI, uiField), uiField, uiTextKey);
  }

  public String getItemName(String itemTextKey) {
    return requireText(requireGroup(items, itemTextKey), itemTextKey, NAME);
  }

  /** Whether the bundled item texts contain an entry for {@code itemTextKey}. */
  public boolean hasItemTexts(String itemTextKey) {
    return items.containsKey(itemTextKey);
  }

  public Map<String, List<String>> getAllDialoguesForTarget(String target) {
    return dialogues.get(target);
  }

  public List<String> getDialoguesKeysForTarget(String target) {
    return dialogues.get(target).keySet().stream().toList();
  }

  public Map<String, List<String>> getItemTexts(String itemTextKey, String... replacements) {
    Map<String, String> originalItemTexts = items.get(itemTextKey);
    if (Objects.isNull(originalItemTexts) || originalItemTexts.isEmpty()) {
      return new HashMap<>();
    }
    Map<String, String> itemTexts = new HashMap<>(originalItemTexts);

    if (FORMAT_KEYS.containsKey(itemTextKey)) {
      String formatKey = FORMAT_KEYS.get(itemTextKey);
      if (itemTexts.containsKey(formatKey)) {
        if (Objects.nonNull(replacements) && replacements.length > 0) {
          itemTexts.put(formatKey, String.format(itemTexts.get(formatKey), replacements));
        }
      }
    }

    return itemTexts.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> List.of(entry.getValue())));
  }

  public String getItemDescription(String itemTextKey) {
    return requireText(requireGroup(items, itemTextKey), itemTextKey, DESCRIPTION);
  }

  private static Map<String, String> requireGroup(Map<String, Map<String, String>> texts, String groupKey) {
    Map<String, String> group = texts.get(groupKey);
    if (group == null) {
      throw new IllegalStateException("Missing text group '" + groupKey + "' in the bundled texts");
    }
    return group;
  }

  private static String requireText(Map<String, String> group, String groupKey, String textKey) {
    String text = group.get(textKey);
    if (text == null) {
      throw new IllegalStateException(
          "Missing text '" + textKey + "' in text group '" + groupKey + "'");
    }
    return text;
  }
}
