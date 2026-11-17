package org.savilusGame;

import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SONG;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MERCHANT_SONG;
import static org.savilusGame.config.GameEntityNameFactory.OUTSIDE_MUSIC;
import static org.savilusGame.enums.GameState.MAP_STATE;
import static org.savilusGame.enums.GameState.PAUSE_STATE;
import static org.savilusGame.enums.GameState.PLAY_STATE;
import static org.savilusGame.enums.GameState.TITLE_STATE;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.LockSupport;

import javax.swing.*;

import org.savilusGame.ai.PathFinder;
import org.savilusGame.config.Config;
import org.savilusGame.config.DevMode;
import org.savilusGame.data.SaveLoad;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.GameEntityFactory;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.Player;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.GameState;
import org.savilusGame.environment.EnvironmentManager;
import org.savilusGame.service.AudioService;
import org.savilusGame.service.CollisionService;
import org.savilusGame.service.FrameScheduler;
import org.savilusGame.service.ItemCatalog;
import org.savilusGame.service.PathfindingService;
import org.savilusGame.service.RenderContext;
import org.savilusGame.service.TextRepository;
import org.savilusGame.tile.GameMap;
import org.savilusGame.tile.TileManager;
import org.savilusGame.world.World;
import org.savilusGame.utils.AssetSetter;
import org.savilusGame.utils.text.TextManager;
import org.savilusGame.utils.CollisionDetector;
import org.savilusGame.utils.CutsceneManager;
import org.savilusGame.utils.KeyHandler;
import org.savilusGame.utils.SpriteCache;
import org.savilusGame.utils.event.EventHandler;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GamePanel extends JPanel implements Runnable, CollisionService, AudioService, TextRepository, ItemCatalog, RenderContext {

  /** Original 16-pixel artwork at 3x scale. */
  public static final int TILE_SIZE = 16 * 3;

  static final int FPS = 60;
  static final Font DEBUG_FONT = new Font("Arial", Font.PLAIN, 20);

  final SpriteCache spriteCache = new SpriteCache();
  final PathfindingService pathFinder = new PathFinder();
  @Getter(AccessLevel.NONE)
  final WorldRenderer worldRenderer = new WorldRenderer();
  final TileManager tileManager;
  final World world;
  final DevMode devMode;
  final KeyHandler keyHandler;
  Sound music = new Sound();
  Sound soundEffect = new Sound();
  CollisionDetector collisionDetector;
  AssetSetter assetSetter;
  SaveLoad saveLoad;
  Config config;
  UI ui;
  EventHandler eventHandler;
  final EnvironmentManager environmentManager;
  final GameMap gameMap;
  final GameEntityFactory gameEntityFactory;
  final CutsceneManager cutsceneManager;
  Player player;
  Area currentArea, nextArea;
  GameState gameState;
  Thread gameThread;
  BufferedImage tempScreen;
  Graphics2D tempGraphic2d;

  final int maxScreenColumn = 20;
  final int maxScreenRow = 12;
  final int screenWidth = TILE_SIZE * maxScreenColumn;
  final int screenHeight = TILE_SIZE * maxScreenRow;
  final int maxWorldCol = 50;
  final int maxWorldRow = 50;
  int screenWidthFull = screenWidth;
  int screenHeightFull = screenHeight;
  boolean fullScreenOn;

  public GamePanel() {
    this(DevMode.fromConfig());
  }

  public GamePanel(DevMode devMode) {
    this.devMode = Objects.requireNonNull(devMode);
    tileManager = new TileManager(this);
    world = new World(tileManager, MAIN_MAP);
    keyHandler = new KeyHandler(this, devMode);
    collisionDetector = new CollisionDetector(this);
    assetSetter = new AssetSetter(this);
    saveLoad = new SaveLoad(this);
    config = new Config(this);
    ui = new UI(this);
    eventHandler = new EventHandler(this);
    environmentManager = new EnvironmentManager(this);
    gameMap = new GameMap(this);
    gameEntityFactory = new GameEntityFactory(this);
    cutsceneManager = new CutsceneManager(this);
    player = new Player(this, keyHandler);
    this.setPreferredSize(new Dimension(screenWidth, screenHeight));
    this.setBackground(Color.BLACK);
    this.setDoubleBuffered(true);
    this.addKeyListener(keyHandler);
    this.setFocusable(true);
    gameState = TITLE_STATE;
  }

  public void startGameThread() {
    gameThread = new Thread(this);
    gameThread.start();
  }

  public void setupGame() {
    world.mapsObjects().put(world.getCurrentMap(), new ArrayList<>());
    assetSetter.setNPC();
    assetSetter.setObject();
    assetSetter.setInteractiveTiles();
    assetSetter.setMonster();
    assetSetter.setProjectile();
    assetSetter.setInteractiveObjects();
    currentArea = Area.OUTSIDE;
    environmentManager.setup();

    tempScreen = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_ARGB);
    tempGraphic2d = (Graphics2D) tempScreen.getGraphics();

    if (fullScreenOn)
      setFullScreen();
  }

  @Override
  public void playBGM(String soundId) {
    music.setFile(soundId);
    music.play();
    music.loop();
  }

  @Override
  public void stopBGM() {
    music.stop();
  }

  /**
   * The single exit path: stops audio, ends the game loop, closes the window and terminates the
   * JVM. The title-menu quit and the window's close button both come through here, so cleanup
   * cannot be skipped by a scattered {@code System.exit} call.
   */
  public void quitGame() {
    stopBGM();
    soundEffect.stop();
    gameThread = null;
    Window window = SwingUtilities.getWindowAncestor(this);
    if (window == null) {
      window = Main.window;
    }
    if (window != null) {
      window.dispose();
    }
    System.exit(0);
  }

  @Override
  public void playSE(String soundId) {
    soundEffect.setFile(soundId);
    soundEffect.play();
  }

  public void resetGame(boolean restart) {
    player.setDefaultPositions();
    player.restorePlayerStatus();
    player.resetCounter();
    world.removeTemporaryObjects();
    world.setBossBattleOn(false);
    if (restart) {
      world.setSkeletonLordDefeated(false);
      eventHandler.clearCompletedEvents();
    }
    world.switchMap(MAIN_MAP);
    currentArea = Area.OUTSIDE;
    assetSetter.setMonster();
    assetSetter.setNPC();
    playBGM(OUTSIDE_MUSIC);

    if (restart) {
      stopBGM();
      player.setDefaultValues();
      player.setItems();
      assetSetter.setObject();
      assetSetter.setInteractiveTiles();
      assetSetter.setInteractiveObjects();
      environmentManager.getLighting().resetDay();
    }
  }

  public void changeArea() {
    if (nextArea != currentArea) {
      stopBGM();

      switch (nextArea) {
        case OUTSIDE -> playBGM(OUTSIDE_MUSIC);
        case INDOOR -> playBGM(MERCHANT_SONG);
        case DUNGEON -> playBGM(DUNGEON_SONG);
        default -> throw new IllegalStateException("Unexpected value: " + nextArea);
      }
    }

    currentArea = nextArea;
    assetSetter.setMonster();
  }

  // ------------------------------------------------------------------
  // Service interface delegations (Phase 0: no behavior change)
  // ------------------------------------------------------------------
  @Override
  public void checkTile(org.savilusGame.entity.CollisionBody entity) {
    collisionDetector.checkTile(entity);
  }

  @Override
  public int checkObject(org.savilusGame.entity.CollisionBody entity, boolean player) {
    return collisionDetector.checkObject(entity, player);
  }

  @Override
  public int checkEntity(org.savilusGame.entity.CollisionBody entity, List<? extends org.savilusGame.entity.CollisionBody> target) {
    return collisionDetector.checkEntity(entity, target);
  }

  @Override
  public boolean checkPlayer(org.savilusGame.entity.CollisionBody entity) {
    return collisionDetector.checkPlayer(entity);
  }

  @Override
  public int checkItems(org.savilusGame.entity.CollisionBody entity, List<org.savilusGame.entity.item.ItemDrop> items) {
    return collisionDetector.checkItems(entity, items);
  }

  @Override
  public java.util.Map<String, List<String>> getDialoguesForTarget(String target) {
    return TextManager.getAllDialoguesForTarget(target);
  }

  @Override
  public List<String> getDialoguesKeysForTarget(String target) {
    return TextManager.getDialoguesKeysForTarget(target);
  }

  @Override
  public String getUiText(String uiField, String uiTextKey) {
    return TextManager.getUiText(uiField, uiTextKey);
  }

  @Override
  public String getSettingText(String settingField, String settingName) {
    return TextManager.getSettingText(settingField, settingName);
  }

  @Override
  public String getItemName(String itemTextKey) {
    return TextManager.getItemName(itemTextKey);
  }

  @Override
  public String getItemDescription(String itemTextKey) {
    return TextManager.getItemDescription(itemTextKey);
  }

  @Override
  public java.util.Map<String, List<String>> getItemTexts(String itemTextKey, String... replacements) {
    return TextManager.getItemTexts(itemTextKey, replacements);
  }

  @Override
  public Item getItem(String name) {
    return gameEntityFactory.getItem(name);
  }

  @Override
  public Graphics2D graphics() {
    return tempGraphic2d;
  }

  @Override
  public int cameraX() {
    return player.getWorldX() - player.getScreenX();
  }

  @Override
  public int cameraY() {
    return player.getWorldY() - player.getScreenY();
  }

  /**
   * One update+render pass per frame slot. The loop waits out the rest of the slot instead of
   * spinning, and a stall never produces a burst of catch-up frames ({@link FrameScheduler}).
   */
  @Override
  public void run() {
    FrameScheduler scheduler = new FrameScheduler(FPS);
    long timer = System.nanoTime();
    long drawCount = 0;

    while (Objects.nonNull(gameThread)) {
      update();
      drawToTempScreen(); // draw everything to the buffered image
      drawToScreen(); // draw the buffered image to the screen
      drawCount++;

      long now = System.nanoTime();
      scheduler.advance(now);

      if (now - timer >= 1_000_000_000L) {
        log.debug("FPS: {}", drawCount);
        drawCount = 0;
        timer = now;
      }

      awaitNextFrame(scheduler);
    }
  }

  private void setFullScreen() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    double width = screenSize.getWidth();
    double height = screenSize.getHeight();
    Main.window.setExtendedState(JFrame.MAXIMIZED_BOTH);
    screenWidthFull = (int) width;
    screenHeightFull = (int) height;
  }

  private void drawToScreen() {
    var screenGraphics = getGraphics();
    screenGraphics.drawImage(tempScreen, 0, 0, screenWidthFull, screenHeightFull, null);
    screenGraphics.dispose();
  }

  private void drawToTempScreen() {
    if (gameState == TITLE_STATE) {
      ui.draw(tempGraphic2d);
    } else if (gameState == MAP_STATE) {
      gameMap.drawFullMapScreen(tempGraphic2d);
    } else {
      tileManager.draw(tempGraphic2d);
      worldRenderer.draw(tempGraphic2d, world, player);
      environmentManager.draw(tempGraphic2d);
      gameMap.drawMiniMap(tempGraphic2d);
      cutsceneManager.draw(tempGraphic2d);
      ui.draw(tempGraphic2d);
    }

    //DEBUG
    if (keyHandler.isShowDebugText()) {

      tempGraphic2d.setFont(DEBUG_FONT);
      tempGraphic2d.setColor(Color.WHITE);
      int x = 10;
      int y = 400;
      int lineHeight = 20;

      tempGraphic2d.drawString(getUiText("debugLabels", "worldX") + player.getWorldX(), x, y);
      y += lineHeight;
      tempGraphic2d.drawString(getUiText("debugLabels", "worldY") + player.getWorldY(), x, y);
      y += lineHeight;
      tempGraphic2d.drawString(getUiText("debugLabels", "column") + (player.getWorldX() + player.getSolidArea().x) / TILE_SIZE, x, y);
      y += lineHeight;
      tempGraphic2d.drawString(getUiText("debugLabels", "row") + (player.getWorldY() + player.getSolidArea().y) / TILE_SIZE, x, y);
      y += lineHeight;
      tempGraphic2d.drawString(getUiText("debugLabels", "godMode") + keyHandler.isGodModeOn(), x, y);
    }
  }

  private void update() {
    if (gameState == PLAY_STATE) {
      player.update();
      if (Objects.nonNull(world.currentNpc())) {
        for (Actor gameEntity : world.currentNpc()) {
          if (Objects.nonNull(gameEntity)) {
            gameEntity.update();
          }
        }
      }

      if (Objects.nonNull(world.currentMonsters())) {
        var monsters = world.currentMonsters();
        for (int monsterIndex = 0; monsterIndex < monsters.size(); monsterIndex++) {
          var monster = monsters.get(monsterIndex);
          if (monster.isAlive() && !monster.isDying()) {
            monster.update();
          }
          if (!monster.isAlive()) {
            monster.checkDrop();
            monsters.remove(monsterIndex);
            monsterIndex--;
          }
        }
      }

      var projectilesList = world.currentProjectiles();
      for (int currentProjectile = 0; currentProjectile < projectilesList.size(); currentProjectile++) {
        var projectile = projectilesList.get(currentProjectile);
        if (Objects.nonNull(projectile) && projectile.isAlive()) {
          projectile.update();
        } else {
          projectilesList.remove(currentProjectile);
          currentProjectile--;
        }
      }

      var interactiveTiles = world.currentInteractiveTiles();
      if (Objects.nonNull(interactiveTiles)) {
        for (InteractiveTile interactiveTile : interactiveTiles) {
          if (Objects.nonNull(interactiveTile)) {
            interactiveTile.update();
          }
        }
      }

      Iterator<Actor> particleIterator = world.particleList().iterator();
      while (particleIterator.hasNext()) {
        Actor particle = particleIterator.next();
        if (Objects.nonNull(particle) && particle.isAlive()) {
          particle.update();
        } else {
          particleIterator.remove();
        }
      }

      environmentManager.update();
    } else if (gameState == PAUSE_STATE) {
      // nothing
    }
  }

  /** Waits until the current frame is due; the loop cannot run faster than the frame rate. */
  private static void awaitNextFrame(FrameScheduler scheduler) {
    long idleNanos = scheduler.idleNanos(System.nanoTime());
    while (idleNanos > 0) {
      LockSupport.parkNanos(idleNanos);
      idleNanos = scheduler.idleNanos(System.nanoTime());
    }
  }
}
