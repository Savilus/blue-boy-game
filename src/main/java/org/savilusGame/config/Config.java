package org.savilusGame.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.Properties;

import org.savilusGame.GamePanel;
import org.savilusGame.Sound;
import org.savilusGame.data.UserDataDirectory;

import lombok.extern.slf4j.Slf4j;

/**
 * Reads and writes the player's settings in the per-user data directory
 * ({@code ${user.home}/.blue-boy/config.properties}, overridable with
 * {@code -Dblueboy.dataDir=...}), never inside the source tree.
 * <p>Missing files keep the in-game defaults; malformed values are reported and ignored.
 * Writes are skipped when nothing changed, because the options screen asks to save on every
 * rendered frame, and they replace the file atomically.
 */
@Slf4j
public final class Config {

  public static final String FULL_SCREEN = "fullScreen";
  public static final String MUSIC_VOLUME = "musicVolume";
  public static final String SOUND_EFFECT_VOLUME = "soundEffectVolume";
  public static final String FILE_NAME = "config.properties";

  static final int MIN_VOLUME = 0;
  static final int MAX_VOLUME = 5;

  private static final String CONFIG_COMMENT = "Blue Boy settings";

  private final GamePanel gamePanel;
  private final Path file;
  private String lastSavedSettings;

  public Config(GamePanel gamePanel) {
    this(gamePanel, UserDataDirectory.resolve().resolve(FILE_NAME));
  }

  Config(GamePanel gamePanel, Path file) {
    this.gamePanel = Objects.requireNonNull(gamePanel);
    this.file = Objects.requireNonNull(file).toAbsolutePath();
  }

  /** Applies persisted settings; absent, unreadable or malformed values keep the defaults. */
  public void loadConfig() {
    if (!Files.exists(file)) {
      return;
    }
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(file)) {
      properties.load(input);
    } catch (IOException error) {
      log.warn("Could not load config from {}; keeping the current settings: {}", file, error.getMessage());
      return;
    }
    applyFullScreen(properties.getProperty(FULL_SCREEN));
    applyVolume(properties.getProperty(MUSIC_VOLUME), MUSIC_VOLUME, gamePanel.getMusic());
    applyVolume(properties.getProperty(SOUND_EFFECT_VOLUME), SOUND_EFFECT_VOLUME, gamePanel.getSoundEffect());
    lastSavedSettings = currentSettings();
  }

  /** Persists settings, writing only when a value changed since the last successful write. */
  public void saveConfig() {
    String currentSettings = currentSettings();
    if (currentSettings.equals(lastSavedSettings)) {
      return;
    }
    try {
      Path directory = file.getParent();
      Files.createDirectories(directory);
      Path temporary = Files.createTempFile(directory, "config-", ".tmp");
      try {
        Properties properties = new Properties();
        properties.setProperty(FULL_SCREEN, Boolean.toString(gamePanel.isFullScreenOn()));
        properties.setProperty(MUSIC_VOLUME, Integer.toString(clamp(gamePanel.getMusic().getVolumeScale())));
        properties.setProperty(SOUND_EFFECT_VOLUME,
            Integer.toString(clamp(gamePanel.getSoundEffect().getVolumeScale())));
        try (OutputStream output = Files.newOutputStream(temporary)) {
          properties.store(output, CONFIG_COMMENT);
        }
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
      } finally {
        Files.deleteIfExists(temporary);
      }
      lastSavedSettings = currentSettings;
    } catch (IOException error) {
      log.warn("Could not save config to {}: {}", file, error.getMessage());
    }
  }

  private void applyFullScreen(String value) {
    if (value == null) {
      return;
    }
    String normalized = value.trim();
    if ("true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized)) {
      gamePanel.setFullScreenOn(Boolean.parseBoolean(normalized));
    } else {
      log.warn("Ignoring invalid {} value '{}' in {}", FULL_SCREEN, value, file);
    }
  }

  private void applyVolume(String value, String key, Sound sound) {
    if (value == null) {
      return;
    }
    try {
      sound.setVolumeScale(clamp(Integer.parseInt(value.trim())));
    } catch (NumberFormatException error) {
      log.warn("Ignoring invalid {} value '{}' in {}", key, value, file);
    }
  }

  private String currentSettings() {
    return gamePanel.isFullScreenOn() + "|" + gamePanel.getMusic().getVolumeScale() + "|"
        + gamePanel.getSoundEffect().getVolumeScale();
  }

  private static int clamp(int volume) {
    return Math.min(MAX_VOLUME, Math.max(MIN_VOLUME, volume));
  }
}
