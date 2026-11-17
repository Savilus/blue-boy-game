package org.savilusGame.environment;

import static java.awt.Color.WHITE;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.enums.DayState.DAWN;
import static org.savilusGame.enums.DayState.DAY;
import static org.savilusGame.enums.DayState.DUSK;
import static org.savilusGame.enums.DayState.NIGHT;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.DayState;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Lighting {

  static final int DAY_STATE_CHANGE_INTERVAL = 3000;
  static final String DAY_STATUS_KEY = "dayStatus";
  static final String DAY_KEY = "day";
  static final String DUSK_KEY = "dusk";
  static final String NIGHT_KEY = "night";
  static final String DAWN_KEY = "dawn";

  static final Color[] GRADIENT_COLORS = {
      new Color(0, 0, 0.1f, 0.1F),
      new Color(0, 0, 0.1f, 0.42F),
      new Color(0, 0, 0.1f, 0.52F),
      new Color(0, 0, 0.1f, 0.61F),
      new Color(0, 0, 0.1f, 0.69F),
      new Color(0, 0, 0.1f, 0.76F),
      new Color(0, 0, 0.1f, 0.82F),
      new Color(0, 0, 0.1f, 0.87F),
      new Color(0, 0, 0.1f, 0.91F),
      new Color(0, 0, 0.1f, 0.94F),
      new Color(0, 0, 0.1f, 0.95F),
      new Color(0, 0, 0.1f, 0.97F)
  };

  static final float[] GRADIENT_FRACTIONS = {
      0f, 0.4f, 0.5f, 0.6f, 0.65f, 0.7f, 0.75f, 0.8f, 0.85f, 0.92f, 0.98f, 1f
  };

  final GamePanel gamePanel;
  BufferedImage darknessFilter;
  int dayCounter;
  float filterAlpha = 0f;
  DayState dayState = DAY;

  public Lighting(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    setLightSource();
  }

  public void setLightSource() {
    darknessFilter = new BufferedImage(gamePanel.getScreenWidth(), gamePanel.getScreenHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2 = (Graphics2D) darknessFilter.getGraphics();

    if (Objects.isNull(gamePanel.getPlayer().getCurrentLightItem())) {
      g2.setColor(new Color(0, 0, 0.1f, 0.98f));
    } else {
      g2.setPaint(createPlayerLightGradient());
    }

    g2.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
    g2.dispose();
  }

  public void resetDay() {
    dayState = DAY;
    filterAlpha = 0f;
  }

  void draw(Graphics2D g2) {
    boolean isOutside = gamePanel.getCurrentArea() == Area.OUTSIDE;
    boolean isDungeon = gamePanel.getCurrentArea() == Area.DUNGEON;

    if (isOutside) {
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, filterAlpha));
    }

    if (isOutside || isDungeon) {
      g2.drawImage(darknessFilter, 0, 0, null);
    }

    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1F));

    if (gamePanel.getCutsceneManager().getSceneNum() == gamePanel.getCutsceneManager().getEnding()) {
      String key = switch (dayState) {
        case DAY -> DAY_KEY;
        case DUSK -> DUSK_KEY;
        case NIGHT -> NIGHT_KEY;
        case DAWN -> DAWN_KEY;
      };
      g2.setColor(WHITE);
      g2.setFont(gamePanel.getUi().getFonts().getDayStatus());
      g2.drawString(gamePanel.getUiText(DAY_STATUS_KEY, key), 800, 500);
    }
  }

  void update() {
    if (gamePanel.getPlayer().isLightUpdated()) {
      setLightSource();
      gamePanel.getPlayer().setLightUpdated(false);
    }

    switch (dayState) {
      case DAY -> {
        if (++dayCounter > DAY_STATE_CHANGE_INTERVAL) {
          dayCounter = 0;
          dayState = DUSK;
        }
      }
      case DUSK -> {
        filterAlpha = Math.min(filterAlpha + 0.001F, 1F);
        if (filterAlpha >= 1F) {
          dayState = NIGHT;
        }
      }
      case NIGHT -> {
        if (++dayCounter > DAY_STATE_CHANGE_INTERVAL) {
          dayCounter = 0;
          dayState = DAWN;
        }
      }
      case DAWN -> {
        filterAlpha = Math.max(filterAlpha - 0.001F, 0F);
        if (filterAlpha <= 0F) {
          dayState = DAY;
        }
      }
    }
  }

  private RadialGradientPaint createPlayerLightGradient() {
    int centerX = gamePanel.getPlayer().getScreenX() + TILE_SIZE / 2;
    int centerY = gamePanel.getPlayer().getScreenY() + TILE_SIZE / 2;
    float radius = gamePanel.getPlayer().getCurrentLightItem().getLightRadius() / 2f;

    return new RadialGradientPaint(centerX, centerY, radius, GRADIENT_FRACTIONS, GRADIENT_COLORS);
  }
}
