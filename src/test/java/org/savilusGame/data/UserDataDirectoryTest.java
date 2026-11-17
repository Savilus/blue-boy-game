package org.savilusGame.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserDataDirectoryTest {

  @TempDir
  Path directory;

  @Test
  void defaultsToThePerUserDirectory() {
    String previous = System.getProperty(UserDataDirectory.OVERRIDE_PROPERTY);
    try {
      System.clearProperty(UserDataDirectory.OVERRIDE_PROPERTY);

      assertEquals(Path.of(System.getProperty("user.home"), ".blue-boy"), UserDataDirectory.resolve());
    } finally {
      restore(previous);
    }
  }

  @Test
  void overridePropertySelectsAnotherDirectory() {
    String previous = System.getProperty(UserDataDirectory.OVERRIDE_PROPERTY);
    try {
      System.setProperty(UserDataDirectory.OVERRIDE_PROPERTY, directory.toString());

      assertEquals(directory.toAbsolutePath(), UserDataDirectory.resolve());
    } finally {
      restore(previous);
    }
  }

  @Test
  void blankOverrideIsIgnored() {
    String previous = System.getProperty(UserDataDirectory.OVERRIDE_PROPERTY);
    try {
      System.setProperty(UserDataDirectory.OVERRIDE_PROPERTY, "  ");

      assertEquals(Path.of(System.getProperty("user.home"), ".blue-boy"), UserDataDirectory.resolve());
    } finally {
      restore(previous);
    }
  }

  private static void restore(String previous) {
    if (previous == null) {
      System.clearProperty(UserDataDirectory.OVERRIDE_PROPERTY);
    } else {
      System.setProperty(UserDataDirectory.OVERRIDE_PROPERTY, previous);
    }
  }
}
