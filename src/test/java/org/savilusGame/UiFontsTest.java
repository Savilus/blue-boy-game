package org.savilusGame;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Font;

import org.junit.jupiter.api.Test;
import org.savilusGame.config.GameEntityNameFactory;

class UiFontsTest {
  @Test
  void rolesKeepTheirStylesAndShareTheSessionFontFamily() {
    var fonts = UiFonts.load(GameEntityNameFactory.MARU_MONICA_FONT);
    assertEquals(fonts.getBase().getFamily(), fonts.getTitle().getFamily());
    assertTrue(fonts.getTitle().isBold());
    assertEquals(96, fonts.getTitle().getSize());
    assertTrue(fonts.getBody().isPlain());
    assertEquals(25, fonts.getBody().getSize());
    assertSame(fonts.getLabel(), fonts.getLabel());
  }

  @Test
  void missingAndUnreadableFontsUseTheSameSizedFallbackPalette() {
    for (String path : new String[]{"/absent-font.ttf", "/config.yml"}) {
      var fonts = UiFonts.load(path);
      assertEquals(Font.SANS_SERIF, fonts.getBase().getName());
      assertEquals(32, fonts.getLabel().getSize());
    }
  }
}
