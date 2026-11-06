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
  static String DIALOGUES_PATH = "src/main/resources/texts/dialogues";
  static String PLAYER_AND_ITEMS_INFO_PATH = "src/main/resources/texts/itemsInformation";
  static String SETTINGS_AND_UI_TEXTS_PATH = "src/main/resources/texts/settingsAndUI";
  static String DESCRIPTION = "description";
  static String USE = "use";
  static String OPEN_DOOR = "openDoor";
  static String COLLECT_ITEM = "collectItem";
  static String LVL_UP = "lvlUp";
  static String NAME = "name";
  static String EVENT_MESSAGES_KEY = "eventMessages";

  @Getter
  Map<String, Map<String, List<String>>> dialogues = TextLoader.loadAllDialogues(DIALOGUES_PATH);
  @Getter
  Map<String, Map<String, String>> items = TextLoader.loadItemsInformation(PLAYER_AND_ITEMS_INFO_PATH);
  @Getter
  Map<String, Map<String, String>> settingsAndUI = TextLoader.loadSettingsAndUITexts(SETTINGS_AND_UI_TEXTS_PATH);

  static Map<String, String> formatKeys = new HashMap<>();

  static {
    // Keyed by the textKey values of GameObject.CHEST / RED_POTION / KEY (kept as literals on
    // purpose: TextManager must not depend on the GameObject class-init order, since the
    // GameObject constructor itself calls TextManager.getItemName).
    formatKeys.put("chest", COLLECT_ITEM);
    formatKeys.put("redPotion", USE);
    formatKeys.put("key", OPEN_DOOR);
    formatKeys.put(EVENT_MESSAGES_KEY, LVL_UP);
  }

  public String getSettingText(String settingField, String settingName) {
    return settingsAndUI.get(settingField).get(settingName);
  }

  public String getUiText(String uiField, String uiTextKey) {
    return settingsAndUI.get(uiField).get(uiTextKey);
  }

  public String getItemName(String itemTextKey) {
    return items.get(itemTextKey).get(NAME);
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

    if (formatKeys.containsKey(itemTextKey)) {
      String formatKey = formatKeys.get(itemTextKey);
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
    return items.get(itemTextKey).get(DESCRIPTION);
  }

}
