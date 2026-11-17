import static java.awt.event.KeyEvent.*;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.enums.GameState.*;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;

import org.savilusGame.GamePanel;
import org.savilusGame.Main;
import org.savilusGame.Sound;
import org.savilusGame.entity.items.Door;
import org.savilusGame.entity.items.RedPotion;
import org.savilusGame.entity.monster.GreenSlime;

/** Drives the real game under Xvfb, independently of headless JUnit. Sound hardware is not under test. */
class DesktopCheck {
  private static final long TIMEOUT_NANOS = 5_000_000_000L;

  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler((thread, error) -> { error.printStackTrace(); System.exit(1); });
    var main = Main.class.getDeclaredMethod("main");
    main.setAccessible(true);
    main.invoke(null);
    var panel = (GamePanel) Main.window.getContentPane().getComponent(0);
    if (!panel.getDevMode().enabled()) throw new AssertionError("Smoke requires its dev-mode resource override");
    var robot = new Robot();
    until(() -> Main.window.isShowing() && panel.getGameThread().isAlive(), "title window and game thread");
    SwingUtilities.invokeAndWait(() -> {
      // A bare X server need not keep the initial, requested centred window position.
      Main.window.setLocation(0, 0);
      Main.window.toFront();
      panel.requestFocusInWindow();
      Toolkit.getDefaultToolkit().sync();
    });
    robot.waitForIdle();
    var view = new View(panel, robot);
    view.frame(); // Warm up X11 capture before entering play.
    try {
      exerciseGame(panel, robot, view);
    } catch (AssertionError failure) {
      System.err.printf("State=%s panel=%s size=%s player=(%d,%d) overlay=%s%n", panel.getGameState(),
          panel.getLocationOnScreen(), panel.getSize(), panel.getPlayer().getWorldX(), panel.getPlayer().getWorldY(),
          panel.getTileManager().isPathVisible());
      save(view.frame(), "failure");
      throw failure;
    } finally {
      robot.keyRelease(VK_S);
    }
    panel.quitGame();
  }

  private static void exerciseGame(GamePanel panel, Robot robot, View view) throws Exception {
    var world = panel.getWorld();
    var player = panel.getPlayer();
    String map = world.getCurrentMap();
    var west = chasingSlime(panel, 18);
    var east = chasingSlime(panel, 28);
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
    panel.setMusic(silent);
    panel.setSoundEffect(silent);

    key(robot, VK_ENTER);
    until(() -> panel.getGameState() == PLAY_STATE && !west.remainingPath().isEmpty() && !east.remainingPath().isEmpty(),
        "two chasers");
    checkChaseFrames(panel, robot, view);
    west.setSpeed(west.getDefaultSpeed());
    east.setSpeed(east.getDefaultSpeed());
    until(() -> west.getWorldX() > 18 * TILE_SIZE && east.getWorldX() < 28 * TILE_SIZE, "both slimes advancing");

    robot.keyPress(VK_S);
    until(() -> player.getInventory().contains(potion) && player.isCollisionOn(), "pickup and solid door");
    robot.keyRelease(VK_S);
    if (player.getWorldY() <= startY || player.getWorldY() > startY + 16) throw new AssertionError("door boundary");
    int field = view.frame().getRGB(120, 100);
    key(robot, VK_C);
    save(view.awaitFrame(frame -> panel.getGameState() == CHARACTER_STATE && field != frame.getRGB(120, 100),
        "inventory"), "inventory");
    key(robot, VK_C);
    key(robot, VK_X);
    view.awaitFrame(frame -> panel.getGameMap().isMiniMapOn() && frame.getRGB(845, 15) == Color.WHITE.getRGB(),
        "visible minimap border");
    key(robot, VK_M);
    save(view.awaitFrame(frame -> panel.getGameState() == MAP_STATE && frame.getRGB(100, 45) == Color.BLACK.getRGB(),
        "full map"), "map");
    key(robot, VK_M);
    var previous = panel.getTileManager().getGameMaps().get(map);
    key(robot, VK_R);
    until(() -> panel.getTileManager().getGameMaps().get(map) != previous, "dev reload");
    until(() -> panel.getGameThread().isAlive() && !east.remainingPath().isEmpty(), "live play after reload");
    System.out.println("::notice::Desktop smoke: Main window, new game, two visible red paths, toggle off, movement stopped by door, potion pickup, inventory, minimap, full map and dev reload OK (silent audio).");
  }

  private static GreenSlime chasingSlime(GamePanel panel, int column) {
    var slime = new GreenSlime(panel);
    slime.setWorldPosition(column * TILE_SIZE, 21 * TILE_SIZE);
    slime.setOnPath(true);
    slime.setSpeed(0); // Hold both probes unobstructed while capturing; movement is checked next.
    return slime;
  }

  private static void checkChaseFrames(GamePanel panel, Robot robot, View view) throws Exception {
    var player = panel.getPlayer();
    int middle = player.getScreenX() + TILE_SIZE / 2;
    int left = middle - TILE_SIZE, right = middle + TILE_SIZE;
    int y = player.getScreenY() + TILE_SIZE / 2;
    int groundLeft = tileCenter(panel, 22, 21), groundRight = tileCenter(panel, 24, 21);
    int redLeft = overlay(groundLeft), redRight = overlay(groundRight);
    Predicate<BufferedImage> plain = frame -> frame.getRGB(left, y) == groundLeft && frame.getRGB(right, y) == groundRight;
    save(view.awaitFrame(plain, "first blitted PLAY frame"), "before");
    key(robot, VK_CONTROL);
    until(() -> panel.getTileManager().isPathVisible(), "Ctrl enabled overlay");
    save(view.awaitFrame(frame -> frame.getRGB(left, y) == redLeft && frame.getRGB(right, y) == redRight,
        "two red paths in the SAME frame"), "tracking");
    key(robot, VK_CONTROL);
    until(() -> !panel.getTileManager().isPathVisible(), "Ctrl disabled overlay");
    view.awaitFrame(plain, "both paths cleared");
  }

  private static int tileCenter(GamePanel panel, int column, int row) {
    var terrain = panel.getTileManager().getGameMaps().get(panel.getWorld().getCurrentMap());
    return panel.getTileManager().getTile()[terrain[column][row]].image().getRGB(TILE_SIZE / 2, TILE_SIZE / 2);
  }

  private static int overlay(int before) {
    var sample = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    sample.setRGB(0, 0, before);
    var graphics = sample.createGraphics();
    try {
      graphics.setColor(new Color(255, 0, 0, 70));
      graphics.fillRect(0, 0, 1, 1);
    } finally {
      graphics.dispose();
    }
    int expected = sample.getRGB(0, 0);
    if (expected == before) throw new AssertionError("overlay indistinguishable from ground");
    return expected;
  }

  private static void save(BufferedImage frame, String name) throws Exception {
    var directory = Files.createDirectories(Path.of("build", "desktop-check"));
    ImageIO.write(frame, "png", directory.resolve(name + ".png").toFile());
  }

  private static void key(Robot robot, int code) {
    robot.keyPress(code);
    robot.keyRelease(code);
    robot.waitForIdle();
  }

  private static void until(BooleanSupplier condition, String description) throws InterruptedException {
    long deadline = System.nanoTime() + TIMEOUT_NANOS;
    while (!condition.getAsBoolean()) {
      if (System.nanoTime() > deadline) throw new AssertionError(description);
      Thread.sleep(20);
    }
  }

  private record View(GamePanel panel, Robot robot) {
    BufferedImage frame() {
      // Sample in panel-local coordinates, never relative to a stale startup screen origin.
      return robot.createScreenCapture(new Rectangle(panel.getLocationOnScreen(), panel.getSize()));
    }

    BufferedImage awaitFrame(Predicate<BufferedImage> condition, String description) throws InterruptedException {
      long deadline = System.nanoTime() + TIMEOUT_NANOS;
      for (;;) {
        BufferedImage image = frame();
        if (condition.test(image)) return image;
        if (System.nanoTime() > deadline) throw new AssertionError(description);
        Thread.sleep(20);
      }
    }
  }
}
