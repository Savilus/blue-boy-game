package org.savilusGame.service;

import java.util.List;
import java.util.Map;

/**
 * Read access to the localized UI/dialogue/item texts.
 */
public interface TextRepository {

  /** All dialogue entries for the given dialogue set (e.g. "eventMessages"). */
  Map<String, List<String>> getDialoguesForTarget(String target);

  /** Ordered dialogue keys for the given dialogue set. */
  List<String> getDialoguesKeysForTarget(String target);

  /** Single UI text. */
  String getUiText(String uiField, String uiTextKey);

  /** Single settings text. */
  String getSettingText(String settingField, String settingName);

  /** Display (localized) name of an item text key. */
  String getItemName(String itemTextKey);

  /** Display (localized) description of an item text key. */
  String getItemDescription(String itemTextKey);

  /** Item texts (name/description/use) with optional {@code String.format} replacements. */
  Map<String, List<String>> getItemTexts(String itemTextKey, String... replacements);
}
