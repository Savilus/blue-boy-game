import static java.awt.event.KeyEvent.*;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.enums.GameState.*;

import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import javax.swing.SwingUtilities;
import org.savilusGame.*;
import org.savilusGame.entity.items.Door;
import org.savilusGame.entity.items.RedPotion;
import org.savilusGame.entity.monster.GreenSlime;

/** Branch-only graphical smoke (not a headless JUnit test). Sound hardware is not under test. */
class DesktopCheck {
  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { error.printStackTrace(); System.exit(1); });
    var main = Main.class.getDeclaredMethod("main");
    main.setAccessible(true);
    main.invoke(null);
    var panel = (GamePanel) Main.window.getContentPane().getComponent(0);
    var world = panel.getWorld();
    var player = panel.getPlayer();
    var robot = new Robot();
    var origin = panel.getLocationOnScreen();
    int px = origin.x + player.getScreenX() + TILE_SIZE / 2;
    int py = origin.y + player.getScreenY() + TILE_SIZE / 2;
    try {
      until(() -> Main.window.isShowing() && panel.getGameThread().isAlive(), "title window and game thread");
      SwingUtilities.invokeAndWait(() -> { Main.window.toFront(); panel.requestFocusInWindow(); });
      // Arrange a repeatable encounter before leaving the title screen.
      String map = world.getCurrentMap();
      var west = new GreenSlime(panel); west.setWorldPosition(18 * TILE_SIZE, 21 * TILE_SIZE); west.setOnPath(true);
      var east = new GreenSlime(panel); east.setWorldPosition(28 * TILE_SIZE, 21 * TILE_SIZE); east.setOnPath(true);
      world.setMonsters(Map.of(map, List.of(west, east)));
      var potion = new RedPotion(panel);
      world.dropItem(potion, player.getWorldX(), player.getWorldY());
      int startY = player.getWorldY();
      world.currentObjects().add(new Door(panel).setWorldPosition(player.getWorldX(), startY + TILE_SIZE));
      var silent = new Sound() {
        @Override protected void setFile(String id) {}
        @Override protected void play() {}
        @Override protected void loop() {}
      };
      panel.setMusic(silent); panel.setSoundEffect(silent);
      key(robot, VK_ENTER);
      until(() -> panel.getGameState() == PLAY_STATE && !west.remainingPath().isEmpty() && !east.remainingPath().isEmpty(), "two chasers");
      int left = px - TILE_SIZE, right = px + TILE_SIZE;
      Color beforeLeft = robot.getPixelColor(left, py), beforeRight = robot.getPixelColor(right, py);
      key(robot, VK_CONTROL);
      until(() -> panel.getTileManager().isPathVisible() && redder(robot.getPixelColor(left, py), beforeLeft)
          && redder(robot.getPixelColor(right, py), beforeRight), "red paths on BOTH sides of player");
      key(robot, VK_CONTROL);
      until(() -> !panel.getTileManager().isPathVisible() && robot.getPixelColor(left, py).equals(beforeLeft)
          && robot.getPixelColor(right, py).equals(beforeRight), "overlay cleared");
      robot.keyPress(VK_S);
      until(() -> player.getInventory().contains(potion) && player.isCollisionOn(), "pickup and solid door");
      robot.keyRelease(VK_S);
      if (player.getWorldY() <= startY || player.getWorldY() > startY + 16) throw new AssertionError("door boundary");
      key(robot, VK_C); until(() -> panel.getGameState() == CHARACTER_STATE, "inventory"); key(robot, VK_C);
      key(robot, VK_X); until(() -> panel.getGameMap().isMiniMapOn(), "minimap");
      key(robot, VK_M); until(() -> panel.getGameState() == MAP_STATE, "full map"); key(robot, VK_M);
      var previous = panel.getTileManager().getGameMaps().get(map);
      key(robot, VK_R); until(() -> panel.getTileManager().getGameMaps().get(map) != previous, "dev reload");
      until(() -> panel.getGameThread().isAlive() && !east.remainingPath().isEmpty(), "live play after reload");
      System.out.println("::notice::Desktop smoke: Main window, new game, two visible red paths, toggle off, movement stopped by door, potion pickup, inventory, minimap, full map and dev reload OK (silent audio).");
    } finally {
      robot.keyRelease(VK_S);
    }
    panel.quitGame();
  }

  private static boolean redder(Color after, Color before) {
    return after.getRed() > before.getRed() && after.getGreen() < before.getGreen();
  }

  private static void key(Robot robot, int code) {
    robot.keyPress(code); robot.keyRelease(code); robot.waitForIdle();
  }

  private static void until(BooleanSupplier condition, String description) throws Exception {
    long deadline = System.nanoTime() + 5_000_000_000L;
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) throw new AssertionError(description);
      Thread.sleep(20);
    }
  }
}
