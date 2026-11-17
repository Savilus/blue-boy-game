package org.savilusGame;

import java.awt.Font;

import lombok.Getter;

/** Immutable screen typography, derived once from the session's bundled font (or its fallback). */
@Getter
public final class UiFonts {
  private final Font base, title, menu, gameOver, retry, pause, label, description, body, message, bossName;
  private final Font dayStatus, credits, creditsTitle;

  UiFonts(Font base) {
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
}
