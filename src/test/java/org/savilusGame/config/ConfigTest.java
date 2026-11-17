package org.savilusGame.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.savilusGame.GamePanel;

/** Settings persistence outside the working directory, with defensive loading. */
class ConfigTest {

  @TempDir
  Path directory;

  private Path file;
  private GamePanel panel;

  @BeforeEach
  void setUp() {
    file = directory.resolve("nested").resolve(Config.FILE_NAME);
    panel = new GamePanel();
  }

  @Test
  @DisplayName("settings round-trip through the given file, creating its directory")
  void savesAndLoadsSettings() {
    panel.setFullScreenOn(true);
    panel.getMusic().setVolumeScale(1);
    panel.getSoundEffect().setVolumeScale(4);

    new Config(panel, file).saveConfig();
    assertTrue(Files.exists(file), "Saving must create the directory and the file");

    GamePanel reloaded = new GamePanel();
    new Config(reloaded, file).loadConfig();

    assertTrue(reloaded.isFullScreenOn());
    assertEquals(1, reloaded.getMusic().getVolumeScale());
    assertEquals(4, reloaded.getSoundEffect().getVolumeScale());
  }

  @Test
  void missingFileKeepsTheDefaults() {
    new Config(panel, file).loadConfig();

    assertFalse(panel.isFullScreenOn());
    assertEquals(3, panel.getMusic().getVolumeScale());
    assertEquals(3, panel.getSoundEffect().getVolumeScale());
  }

  @Test
  @DisplayName("an unchanged options screen does not rewrite the file on every frame")
  void unchangedSettingsAreNotRewritten() throws IOException {
    Config config = new Config(panel, file);
    config.saveConfig();
    Files.writeString(file, "sentinel");

    config.saveConfig();

    assertEquals("sentinel", Files.readString(file));
  }

  @Test
  @DisplayName("corrupt values are ignored and volumes are clamped to the configured range")
  void invalidValuesAreIgnored() throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, """
        fullScreen=maybe
        musicVolume=99
        soundEffectVolume=loud
        """);

    new Config(panel, file).loadConfig();

    assertFalse(panel.isFullScreenOn(), "An unparseable full-screen flag must not change the setting");
    assertEquals(Config.MAX_VOLUME, panel.getMusic().getVolumeScale());
    assertEquals(3, panel.getSoundEffect().getVolumeScale(), "An unparseable volume keeps the default");
  }

  @Test
  void unreadableFileKeepsTheCurrentSettings() throws IOException {
    Files.createDirectories(file);
    panel.setFullScreenOn(true);

    new Config(panel, file).loadConfig();

    assertTrue(panel.isFullScreenOn());
  }
}
