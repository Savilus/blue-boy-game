package org.savilusGame.environment;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.DayState;

class LightingTest {
  @Test
  void onlyTheEndingLooksUpDayStatusAndItReusesTheFont() {
    var panel = new GamePanel() {
      private int lookups;
      @Override public String getUiText(String group, String key) {
        lookups++;
        return super.getUiText(group, key);
      }
    };
    panel.setCurrentArea(Area.OUTSIDE);
    var lighting = new Lighting(panel);
    var graphics = new BufferedImage(960, 576, BufferedImage.TYPE_INT_ARGB).createGraphics();
    try {
      lighting.draw(graphics);
      assertEquals(0, panel.lookups);
      panel.getCutsceneManager().setSceneNum(panel.getCutsceneManager().getEnding());
      for (DayState state : DayState.values()) {
        lighting.setDayState(state);
        lighting.draw(graphics);
        assertSame(panel.getUi().getFonts().getDayStatus(), graphics.getFont());
      }
      assertEquals(DayState.values().length, panel.lookups);
    } finally {
      graphics.dispose();
    }
  }
}
