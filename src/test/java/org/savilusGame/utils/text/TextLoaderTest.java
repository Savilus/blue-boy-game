package org.savilusGame.utils.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.savilusGame.utils.text.TextLoader.DIALOGUES_CATEGORY;
import static org.savilusGame.utils.text.TextLoader.ITEMS_CATEGORY;
import static org.savilusGame.utils.text.TextLoader.SETTINGS_AND_UI_CATEGORY;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Text loading must not depend on the working directory: these tests read everything through the
 * classpath, exactly like a jar, and cross-check the bundled index so a new text file cannot be
 * shipped silently unread.
 */
class TextLoaderTest {

  @Test
  void loadsEveryTextCategoryFromTheClasspath() {
    var dialogues = TextLoader.loadAllDialogues();
    var items = TextLoader.loadItemsInformation();
    var settingsAndUi = TextLoader.loadSettingsAndUITexts();

    assertFalse(dialogues.isEmpty());
    assertTrue(dialogues.containsKey("eventMessages"));
    assertTrue(dialogues.get("eventMessages").containsKey("lvlUp"));

    assertTrue(items.containsKey("normalSword"));
    assertNotNull(items.get("normalSword").get("name"));

    assertTrue(settingsAndUi.get("uiMessages").containsKey("loadFailed"));
  }

  @Test
  @DisplayName("the bundled index lists every text file, so a jar cannot silently lose texts")
  void indexCoversEveryBundledTextFile() throws Exception {
    assertIndexMatches(DIALOGUES_CATEGORY);
    assertIndexMatches(ITEMS_CATEGORY);
    assertIndexMatches(SETTINGS_AND_UI_CATEGORY);
  }

  @Test
  void unknownCategoryFailsWithAnActionableMessage() {
    IllegalStateException error = assertThrows(IllegalStateException.class,
        () -> TextLoader.listedFiles("doesNotExist"));

    assertTrue(error.getMessage().contains("doesNotExist"));
  }

  @Test
  @DisplayName("a missing text key fails fast instead of returning null")
  void missingTextsFailFast() {
    assertThrows(IllegalStateException.class, () -> TextManager.getUiText("uiMessages", "noSuchKey"));
    assertThrows(IllegalStateException.class, () -> TextManager.getItemName("noSuchItem"));
    assertThrows(IllegalStateException.class, () -> TextManager.getSettingText("noSuchGroup", "any"));
  }

  private static void assertIndexMatches(String category) throws Exception {
    URL folder = TextLoader.class.getResource("/texts/" + category);
    assertNotNull(folder, "Missing bundled text folder " + category);

    try (Stream<Path> files = Files.list(Path.of(folder.toURI()))) {
      Set<String> bundled = files.filter(Files::isRegularFile)
          .map(path -> path.getFileName().toString())
          .collect(Collectors.toSet());
      assertEquals(bundled, Set.copyOf(TextLoader.listedFiles(category)),
          "texts/index.json and src/main/resources/texts/" + category + " must list the same files");
    }
  }
}
