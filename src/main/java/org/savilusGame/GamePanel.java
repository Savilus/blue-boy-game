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
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import javax.swing.*;

import org.savilusGame.ai.PathFinder;
import org.savilusGame.config.Config;
import org.savilusGame.data.SaveLoad;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.GameEntityFactory;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.Player;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.GameState;
import org.savilusGame.environment.EnvironmentManager;
import org.savilusGame.service.AudioService;
import org.savilusGame.service.CollisionService;
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
import org.savilusGame.utils.event.EventHandler;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GamePanel extends JPanel implements Runnable, CollisionService, PathfindingService, AudioService, TextRepository, ItemCatalog, RenderContext {

  final static int ALLOWED_ITEMS = 20;

  // SCREEN SETTINGS
  final static int ORIGINAL_TITLE_SIZE = 16; // 16x16 tile
  final static int SCALE = 3;
  public final static int TILE_SIZE = ORIGINAL_TITLE_SIZE * SCALE; // 48 x 48 tile
  final int maxScreenColumn = 20;
  final int maxScreenRow = 12;
  final int screenWidth = TILE_SIZE * maxScreenColumn; // 960 pixels
  final int screenHeight = TILE_SIZE * maxScreenRow; // 576 pixels

  // WORLD SETTINGS
  final int maxWorldCol = 50;
  final int maxWorldRow = 50;
  // FOR FULL SCREEN
  int screenWidthFull = screenWidth;
  int screenHeightFull = screenHeight;
  BufferedImage tempScreen;
  Graphics2D tempGraphic2d;
  boolean fullScreenOn = false;

  // SYSTEM
  static final int FPS = 60;
  final TileManager tileManager = new TileManager(this);
  final World world = new World(tileManager, MAIN_MAP);
  final KeyHandler keyHandler = new KeyHandler(this);
  Sound music = new Sound();
  Sound soundEffect = new Sound();
  PathFinder pathFinder = new PathFinder(this);
  CollisionDetector collisionDetector = new CollisionDetector(this);
  AssetSetter assetSetter = new AssetSetter(this);
  Thread gameThread;
  SaveLoad saveLoad = new SaveLoad(this);
  Config config = new Config(this);
  UI ui = new UI(this);
  EventHandler eventHandler = new EventHandler(this);
  final EnvironmentManager environmentManager = new EnvironmentManager(this);
  final GameMap gameMap = new GameMap(this);
  final GameEntityFactory gameEntityFactory = new GameEntityFactory(this);
  final CutsceneManager cutsceneManager = new CutsceneManager(this);

  // ENTITY AND OBJECT
  Player player = new Player(this, keyHandler);

  // AREA
  Area currentArea;
  Area nextArea;

  // GAME STATE
  GameState gameState;

  public GamePanel() {
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

  private void setFullScreen() {
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    double width = screenSize.getWidth();
    double height = screenSize.getHeight();
    Main.window.setExtendedState(JFrame.MAXIMIZED_BOTH);
    screenWidthFull = (int) width;
    screenHeightFull = (int) height;
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
  public void setNodes(int startCol, int startRow, int goalCol, int goalRow) {
    pathFinder.setNodes(startCol, startRow, goalCol, goalRow);
  }

  @Override
  public boolean search() {
    return pathFinder.search();
  }

  @Override
  public java.util.List<org.savilusGame.ai.Node> getPathList() {
    return pathFinder.getPathList();
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
      var interactiveTiles = world.currentInteractiveTiles();
      if (Objects.nonNull(interactiveTiles)) {
        for (InteractiveTile interactiveTile : interactiveTiles) {
          if (Objects.nonNull(interactiveTile)) {
            interactiveTile.draw(tempGraphic2d);
          }
        }
      }

      // ONE y-sorted draw list: player + actors + world objects + item drops (identical z-order as before).
      List<CollisionBody> drawables = new ArrayList<>();
      drawables.add(player);
      if (Objects.nonNull(world.currentNpc())) {
        for (Actor npc : world.currentNpc()) {
          if (Objects.nonNull(npc)) {
            drawables.add(npc);
          }
        }
      }
      if (Objects.nonNull(world.currentObjects())) {
        for (InteractiveObject object : world.currentObjects()) {
          if (Objects.nonNull(object)) {
            drawables.add(object);
          }
        }
      }
      if (Objects.nonNull(world.currentMonsters())) {
        for (Actor monster : world.currentMonsters()) {
          if (Objects.nonNull(monster)) {
            drawables.add(monster);
          }
        }
      }
      if (Objects.nonNull(world.currentProjectiles())) {
        for (Actor projectile : world.currentProjectiles()) {
          if (Objects.nonNull(projectile)) {
            drawables.add(projectile);
          }
        }
      }
      for (Actor particle : world.particleList()) {
        if (Objects.nonNull(particle)) {
          drawables.add(particle);
        }
      }
      if (Objects.nonNull(world.currentItems())) {
        for (var itemDrop : world.currentItems()) {
          if (Objects.nonNull(itemDrop)) {
            drawables.add(itemDrop);
          }
        }
      }

      drawables.sort(Comparator.comparingInt(CollisionBody::getWorldY));

      for (CollisionBody drawable : drawables) {
        if (drawable instanceof Actor actor) {
          actor.draw(tempGraphic2d);
        } else if (drawable instanceof InteractiveObject interactiveObject) {
          interactiveObject.draw(tempGraphic2d);
        } else if (drawable instanceof org.savilusGame.entity.item.ItemDrop itemDrop) {
          itemDrop.draw(tempGraphic2d);
        }
      }

      environmentManager.draw(tempGraphic2d);
      gameMap.drawMiniMap(tempGraphic2d);
      cutsceneManager.draw(tempGraphic2d);
      ui.draw(tempGraphic2d);
    }

    //DEBUG
    if (keyHandler.isShowDebugText()) {

      tempGraphic2d.setFont(new Font("Arial", Font.PLAIN, 20));
      tempGraphic2d.setColor(Color.WHITE);
      int x = 10;
      int y = 400;
      int lineHeight = 20;

      tempGraphic2d.drawString("WorldX -> " + player.getWorldX(), x, y);
      y += lineHeight;
      tempGraphic2d.drawString("WorldY ->" + player.getWorldY(), x, y);
      y += lineHeight;
      tempGraphic2d.drawString("Col ->" + (player.getWorldX() + player.getSolidArea().x) / TILE_SIZE, x, y);
      y += lineHeight;
      tempGraphic2d.drawString("Row -> " + (player.getWorldY() + player.getSolidArea().y) / TILE_SIZE, x, y);
      y += lineHeight;
      tempGraphic2d.drawString("God Mode: -> " + keyHandler.isGodModeOn(), x, y);
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

  @Override
  public void run() {
    double drawInterval = (double) 1000000000 / FPS;
    double delta = 0;
    long lastTime = System.nanoTime();
    long currentTime;
    long timer = 0;
    long drawCount = 0;

    while (Objects.nonNull(gameThread)) {

      currentTime = System.nanoTime();

      delta += (currentTime - lastTime) / drawInterval;
      timer += (currentTime - lastTime);
      lastTime = currentTime;

      if (delta >= 1) {
        update();
        drawToTempScreen(); // draw everything to the buffered image
        drawToScreen(); // draw the buffered image to the screen
        delta--;
        drawCount++;
      }

      if (timer >= 1000000000) {
        System.out.println("FPS: " + drawCount);
        drawCount = 0;
        timer = 0;
      }
    }
  }
}
