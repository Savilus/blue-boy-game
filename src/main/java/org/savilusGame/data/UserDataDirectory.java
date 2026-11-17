package org.savilusGame.data;

import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;

/**
 * Resolves the per-user directory that holds mutable game state (save file, settings).
 * <p>Installed games must never write into the source tree or into whatever directory the
 * process happened to start in, so every writable artefact lives under
 * {@code ${user.home}/.blue-boy}. The {@value #OVERRIDE_PROPERTY} system property points the
 * game at another directory (portable installs, tests, release candidates).
 */
public final class UserDataDirectory {

  /** System property that overrides the data directory. */
  public static final String OVERRIDE_PROPERTY = "blueboy.dataDir";

  private static final String DEFAULT_DIRECTORY_NAME = ".blue-boy";
  private static final String USER_HOME_PROPERTY = "user.home";

  private UserDataDirectory() {
  }

  /** Directory for {@code save.json} and {@code config.properties}; never {@code null}. */
  public static Path resolve() {
    String override = StringUtils.trimToNull(System.getProperty(OVERRIDE_PROPERTY));
    if (override != null) {
      return Path.of(override).toAbsolutePath();
    }
    String home = StringUtils.trimToNull(System.getProperty(USER_HOME_PROPERTY));
    if (home == null) {
      throw new IllegalStateException(
          "System property '" + USER_HOME_PROPERTY + "' is not set; cannot locate the game data directory");
    }
    return Path.of(home, DEFAULT_DIRECTORY_NAME);
  }
}
