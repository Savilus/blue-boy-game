package org.savilusGame.utils.text;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

/**
 * Loads the bundled text files from the classpath, so the game works from any working
 * directory and from inside a jar.
 * <p>{@value #INDEX_RESOURCE} lists every bundled file per category. A missing, unreadable
 * or malformed file aborts start-up with the offending resource name instead of leaving the
 * UI with silently empty text maps and a {@code NullPointerException} on the first draw.
 */
@Slf4j
@UtilityClass
public class TextLoader {

  static final String INDEX_RESOURCE = "/texts/index.json";
  static final String DIALOGUES_CATEGORY = "dialogues";
  static final String ITEMS_CATEGORY = "itemsInformation";
  static final String SETTINGS_AND_UI_CATEGORY = "settingsAndUI";

  private static final String TEXTS_FOLDER = "/texts/";

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public Map<String, Map<String, List<String>>> loadAllDialogues() {
    Map<String, Map<String, List<String>>> dialogues = new LinkedHashMap<>();
    for (String fileName : listedFiles(DIALOGUES_CATEGORY)) {
      dialogues.putAll(read(categoryResource(DIALOGUES_CATEGORY, fileName), DIALOGUES_CATEGORY));
    }
    log.info("Loaded {} dialogue entries from the classpath", dialogues.size());
    return Collections.unmodifiableMap(dialogues);
  }

  public Map<String, Map<String, String>> loadItemsInformation() {
    Map<String, Map<String, String>> items = new LinkedHashMap<>();
    for (String fileName : listedFiles(ITEMS_CATEGORY)) {
      items.putAll(read(categoryResource(ITEMS_CATEGORY, fileName), ITEMS_CATEGORY));
    }
    log.info("Loaded {} item text entries from the classpath", items.size());
    return Collections.unmodifiableMap(items);
  }

  public Map<String, Map<String, String>> loadSettingsAndUITexts() {
    Map<String, Map<String, String>> texts = new LinkedHashMap<>();
    for (String fileName : listedFiles(SETTINGS_AND_UI_CATEGORY)) {
      texts.putAll(read(categoryResource(SETTINGS_AND_UI_CATEGORY, fileName), SETTINGS_AND_UI_CATEGORY));
    }
    log.info("Loaded {} settings/UI text entries from the classpath", texts.size());
    return Collections.unmodifiableMap(texts);
  }

  /** Bundled file names of {@code category} as listed in {@value #INDEX_RESOURCE}. */
  static List<String> listedFiles(String category) {
    try (InputStream input = open(INDEX_RESOURCE)) {
      Map<String, List<String>> index = OBJECT_MAPPER.readValue(input,
          new TypeReference<Map<String, List<String>>>() {
          });
      List<String> files = index.get(category);
      if (files == null || files.isEmpty()) {
        throw new IllegalStateException(
            "Text index " + INDEX_RESOURCE + " lists no files for category '" + category + "'");
      }
      return List.copyOf(files);
    } catch (IOException error) {
      throw new IllegalStateException("Cannot read bundled text index " + INDEX_RESOURCE, error);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Map<String, T> read(String resource, String category) {
    try (InputStream input = open(resource)) {
      Object content = OBJECT_MAPPER.readValue(input, Object.class);
      if (!(content instanceof Map<?, ?> entries)) {
        throw new IllegalStateException("Bundled text file " + resource + " is not a JSON object");
      }
      Map<String, T> typed = new LinkedHashMap<>();
      for (Map.Entry<?, ?> entry : entries.entrySet()) {
        if (!(entry.getKey() instanceof String key) || !(entry.getValue() instanceof Map<?, ?>)) {
          throw new IllegalStateException("Bundled text file " + resource + " may contain only text"
              + " groups, but entry '" + entry.getKey() + "' is not an object");
        }
        typed.put(key, (T) entry.getValue());
      }
      if (typed.isEmpty()) {
        throw new IllegalStateException(
            "Bundled text file " + resource + " contains no " + category + " entries");
      }
      return Collections.unmodifiableMap(typed);
    } catch (IOException error) {
      throw new IllegalStateException("Cannot read bundled text file " + resource, error);
    }
  }

  private static InputStream open(String resource) {
    InputStream input = TextLoader.class.getResourceAsStream(resource);
    if (input == null) {
      throw new IllegalStateException(
          "Missing bundled resource " + resource + "; the game was packaged or started without its texts");
    }
    return input;
  }

  private static String categoryResource(String category, String fileName) {
    return TEXTS_FOLDER + category + "/" + fileName;
  }
}
