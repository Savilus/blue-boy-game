package org.savilusGame.tile;

import static org.junit.jupiter.api.Assertions.*;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.config.DevMode;
import org.savilusGame.entity.monster.GreenSlime;
import org.savilusGame.entity.monster.SkeletonLord;
import org.savilusGame.enums.GameState;
import org.savilusGame.utils.KeyHandler;

class ChaseOverlayTest {
  private final GamePanel panel = new GamePanel();

  @Test
  void controlShowsBothCurrentRoutesAndDropsStoppedDyingAndOtherMapEnemies() {
    var world = panel.getWorld();
    for (int[] column : panel.getTileManager().getGameMaps().get(MAIN_MAP)) Arrays.fill(column, 10);
    var player = panel.getPlayer();
    player.setWorldPosition(25 * TILE_SIZE, 25 * TILE_SIZE);
    panel.setGameState(GameState.PLAY_STATE);
    var west = chasingSlime(22);
    var east = chasingSlime(28);
    var westRoute = west.remainingPath();
    assertEquals(3, westRoute.size());
    assertEquals(3, east.remainingPath().size());
    BufferedImage off = draw();
    int left = player.getScreenX() - TILE_SIZE;
    int right = player.getScreenX() + TILE_SIZE;
    int y = player.getScreenY() + TILE_SIZE / 2;
    int ground = off.getRGB(left, y);
    toggle(false);
    assertEquals(ground, draw().getRGB(left, y));
    toggle(true);

    BufferedImage both = draw();
    assertNotEquals(ground, both.getRGB(left, y));
    assertNotEquals(off.getRGB(right, y), both.getRGB(right, y));
    assertSame(westRoute, west.remainingPath(), "drawing must not consume waypoints");
    west.setOnPath(false);
    BufferedImage onlyEast = draw();
    assertEquals(ground, onlyEast.getRGB(left, y));
    assertEquals(both.getRGB(right, y), onlyEast.getRGB(right, y));
    east.setDying(true);
    assertEquals(off.getRGB(right, y), draw().getRGB(right, y));
    east.setDying(false);
    east.setAlive(false);
    assertEquals(off.getRGB(right, y), draw().getRGB(right, y));
    east.setAlive(true);
    world.switchMap(INTERIOR_MAP);
    int otherMap = draw().getRGB(right, y);
    toggle(true);
    assertEquals(otherMap, draw().getRGB(right, y));
  }

  @Test
  void directPursuitShowsTheBossDirectionWithoutInventingAnAStarRoute() {
    var player = panel.getPlayer();
    var boss = new SkeletonLord(panel);
    boss.setWorldPosition(player.getWorldX() - 5 * TILE_SIZE, player.getWorldY() - 2 * TILE_SIZE);
    panel.getWorld().currentMonsters().add(boss);
    panel.getTileManager().togglePathVisibility();
    int endX = player.getScreenX() + (int) player.getSolidArea().getCenterX();
    int endY = player.getScreenY() + (int) player.getSolidArea().getCenterY();
    int asleep = draw().getRGB(endX, endY);
    boss.setSleep(false);
    assertTrue(boss.isPursuingDirectly());
    assertNotEquals(asleep, draw().getRGB(endX, endY));
    boss.setWorldPosition(0, 0);
    assertFalse(boss.isPursuingDirectly());
    assertEquals(asleep, draw().getRGB(endX, endY));
  }

  private GreenSlime chasingSlime(int column) {
    var slime = new GreenSlime(panel);
    slime.setWorldPosition(column * TILE_SIZE, 25 * TILE_SIZE);
    panel.getWorld().currentMonsters().add(slime);
    slime.setOnPath(true);
    slime.setAction(); // close enough never to roll a stop-chasing decision
    return slime;
  }

  private void toggle(boolean dev) {
    new KeyHandler(panel, new DevMode(dev)).keyPressed(
        new KeyEvent(panel, KeyEvent.KEY_PRESSED, 0L, 0, KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED));
  }

  private BufferedImage draw() {
    var image = new BufferedImage(panel.getScreenWidth(), panel.getScreenHeight(), BufferedImage.TYPE_INT_ARGB);
    var graphics = image.createGraphics();
    try {
      graphics.setColor(Color.BLUE);
      panel.getTileManager().draw(graphics);
      assertEquals(Color.BLUE, graphics.getColor());
    } finally {
      graphics.dispose();
    }
    return image;
  }
}
