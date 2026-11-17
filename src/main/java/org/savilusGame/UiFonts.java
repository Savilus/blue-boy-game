package org.savilusGame;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/** Immutable screen typography, derived once from the session's bundled font (or its fallback). */
@Getter
@Slf4j
public final class UiFonts {
  private final Font base, title, menu, gameOver, retry, pause, label, description, body, message, bossName;
  private final Font dayStatus, credits, creditsTitle;

  private UiFonts(Font base) {
    this.base = base;
    title = base.deriveFont(Font.BOLD, 96f);
    menu = base.deriveFont(Font.BOLD, 48f);
    gameOver = base.deriveFont(Font.BOLD, 110f);
    retry = base.deriveFont(Font.BOLD, 50f);
    pause = base.deriveFont(Font.PLAIN, 80f);
    label = base.deriveFont(Font.PLAIN, 32f);
    description = base.deriveFont(Font.PLAIN, 28f);
    body = base.deriveFont(Font.PLAIN, 25f);
    message = base.deriveFont(Font.BOLD, 20f);
    bossName = base.deriveFont(Font.BOLD, 24f);
    dayStatus = base.deriveFont(Font.PLAIN, 50f);
    credits = base.deriveFont(Font.PLAIN, 38f);
    creditsTitle = base.deriveFont(Font.PLAIN, 120f);
  }

  static UiFonts load(String path) {
    try (var stream = UiFonts.class.getResourceAsStream(path)) {
      if (stream != null) return new UiFonts(Font.createFont(Font.TRUETYPE_FONT, stream));
      log.warn("Missing UI font {}; using the system font", path);
    } catch (IOException | FontFormatException error) {
      log.warn("Cannot load UI font {}; using the system font", path, error);
    }
    return new UiFonts(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
  }
}
