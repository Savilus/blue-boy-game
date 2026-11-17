package org.savilusGame.utils.event;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_FIRST_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;
import static org.savilusGame.config.GameEntityNameFactory.POWER_UP;
import static org.savilusGame.config.GameEntityNameFactory.RECEIVE_DAMAGE;
import static org.savilusGame.config.GameEntityNameFactory.STAIRS;
import static org.savilusGame.enums.GameState.CUTSCENE_STATE;
import static org.savilusGame.enums.GameState.DIALOG_STATE;
import static org.savilusGame.enums.GameState.TRANSITION_STATE;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.Player;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.Direction;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventHandler {

  static final String DAMAGE_PIT = "damagePit";
  static final String HEALING_POOL = "healingPool";

  String tempMap;
  final GamePanel gamePanel;
  final Map<String, EventRect[][]> eventRect = new HashMap<>();
  final Map<String, Runnable> mapHandlers = Map.of(
      MAIN_MAP, this::handleMainMapEvents,
      DUNGEON_FIRST_FLOR, this::handleDungeonFirstFloorEvents,
      DUNGEON_SECOND_FLOR, this::handleDungeonSecondFloorEvents,
      INTERIOR_MAP, this::handleInteriorMapEvents);
  int tempCol, tempRow;

  int previousEventX, previousEventY;
  boolean canTouchEvent = true;

  public EventHandler(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    for (String mapName : gamePanel.getTileManager().getGameMaps().keySet()) {
      EventRect[][] mapEvents = new EventRect[gamePanel.getMaxWorldCol()][gamePanel.getMaxWorldRow()];
      for (int row = 0; row < gamePanel.getMaxWorldRow(); row++) {
        for (int col = 0; col < gamePanel.getMaxWorldCol(); col++) {
          EventRect rect = new EventRect();
          rect.x = 23;
          rect.y = 23;
          rect.width = 2;
          rect.height = 2;

          mapEvents[col][row] = rect;
        }
      }
      eventRect.put(mapName, mapEvents);
    }
  }

  public void checkEvent() {
    int xDistance = Math.abs(getPlayer().getWorldX() - previousEventX);
    int yDistance = Math.abs(getPlayer().getWorldY()- previousEventY);
    int distance = Math.max(xDistance, yDistance);

    if (distance > TILE_SIZE) {
      canTouchEvent = true;
    }

    if (canTouchEvent) {
      handleMapEvents(gamePanel.getWorld().getCurrentMap());
    }
  }

  public boolean[][] completedEvents(String mapName) {
    EventRect[][] events = Objects.requireNonNull(eventRect.get(mapName), "Unknown event map: " + mapName);
    boolean[][] completed = new boolean[events.length][];
    for (int col = 0; col < events.length; col++) {
      completed[col] = new boolean[events[col].length];
      for (int row = 0; row < events[col].length; row++) {
        completed[col][row] = events[col][row].oneTimeEventDone;
      }
    }
    return completed;
  }

  public void restoreCompletedEvents(String mapName, boolean[][] completed) {
    EventRect[][] events = Objects.requireNonNull(eventRect.get(mapName), "Unknown event map: " + mapName);
    if (completed.length != events.length) {
      throw new IllegalArgumentException("Invalid event columns for map: " + mapName);
    }
    for (int col = 0; col < events.length; col++) {
      if (completed[col] == null || completed[col].length != events[col].length) {
        throw new IllegalArgumentException("Invalid event rows for map: " + mapName);
      }
    }
    for (int col = 0; col < events.length; col++) {
      for (int row = 0; row < events[col].length; row++) {
        events[col][row].oneTimeEventDone = completed[col][row];
      }
    }
  }

  public void clearCompletedEvents() {
    for (EventRect[][] events : eventRect.values()) {
      for (EventRect[] column : events) {
        for (EventRect event : column) {
          event.oneTimeEventDone = false;
        }
      }
    }
  }

  private void handleMapEvents(String currentMap) {
    Runnable mapHandler = mapHandlers.get(currentMap);
    if (Objects.nonNull(mapHandler)) {
      mapHandler.run();
    }
  }

  private void handleMainMapEvents() {
    if (hit(27, 16, Direction.RIGHT)) damagePit();
    else if (hit(23, 12, Direction.UP)) healingPool();
    else if (hit(10, 39, Direction.ANY)) mapTeleport(INTERIOR_MAP, 12, 13, Area.INDOOR);
    else if (hit(12, 9, Direction.ANY)) mapTeleport(DUNGEON_FIRST_FLOR, 9, 41, Area.DUNGEON);
  }

  private void handleDungeonFirstFloorEvents() {
    if (hit(8, 7, Direction.ANY)) mapTeleport(DUNGEON_SECOND_FLOR, 26, 41, Area.DUNGEON);
    else if (hit(9, 41, Direction.ANY)) mapTeleport(MAIN_MAP, 12, 10, Area.OUTSIDE);
  }

  private void handleDungeonSecondFloorEvents() {
    if (hit(26, 41, Direction.ANY)) mapTeleport(DUNGEON_FIRST_FLOR, 8, 7, Area.DUNGEON);
    if (hit(25, 27, Direction.ANY)) skeletonLord();
  }

  private void handleInteriorMapEvents() {
    if (hit(12, 13, Direction.ANY)) mapTeleport(MAIN_MAP, 10, 39, Area.OUTSIDE);
    else if (hit(12, 9, Direction.UP) && gamePanel.getKeyHandler().isEnterPressed()) {
      var npcs = gamePanel.getWorld().npcs(INTERIOR_MAP);
      if (npcs == null || npcs.isEmpty() || npcs.getFirst() == null) {
        log.warn("Cannot start interior dialogue: merchant is missing");
      } else {
        speak(npcs.getFirst());
      }
    }
  }

  private void speak(Actor gameEntity) {
    if (gamePanel.getKeyHandler().isEnterPressed()) {
      gamePanel.setGameState(DIALOG_STATE);
      getPlayer().setAttackCanceled(true);
      gameEntity.speak();
    }
  }

  private boolean hit(int col, int row, Direction requiredDirection) {
    String currentMap = gamePanel.getWorld().getCurrentMap();
    EventRect[][] mapEvents = eventRect.get(currentMap);
    if (mapEvents == null) throw new IllegalStateException("No event grid for current map: " + currentMap);
    EventRect event = mapEvents[col][row];
    var player = getPlayer();
    if (event.oneTimeEventDone || (requiredDirection != Direction.ANY && player.getDirection() != requiredDirection)) {
      return false;
    }
    if (!player.getSolidArea().intersects(col * TILE_SIZE + event.x - player.getWorldX(),
        row * TILE_SIZE + event.y - player.getWorldY(), event.width, event.height)) {
      return false;
    }
    previousEventX = player.getWorldX();
    previousEventY = player.getWorldY();
    return true;
  }

  private void mapTeleport(String teleportToMap, int col, int row, Area area) {
    gamePanel.setGameState(TRANSITION_STATE);
    gamePanel.setNextArea(area);
    tempMap = teleportToMap;
    tempCol = col;
    tempRow = row;
    canTouchEvent = false;
    gamePanel.playSE(STAIRS);
  }

  private void skeletonLord() {
    if (!gamePanel.getWorld().isBossBattleOn() && !gamePanel.getWorld().isSkeletonLordDefeated()) {
      gamePanel.setGameState(CUTSCENE_STATE);
      gamePanel.getCutsceneManager().setSceneNum(gamePanel.getCutsceneManager().getSkeletonLord());
    }
  }

  private void damagePit() {
    gamePanel.setGameState(DIALOG_STATE);
    getPlayer().startDialogue(getPlayer(), DAMAGE_PIT);
    getPlayer().setCurrentLife(getPlayer().getCurrentLife() - 1);
    gamePanel.playSE(RECEIVE_DAMAGE);
//    eventRect[col][row].eventDone = true;
    canTouchEvent = false;
  }

  private void healingPool() {
    if (gamePanel.getKeyHandler().isEnterPressed()) {
      gamePanel.setGameState(DIALOG_STATE);
      getPlayer().setAttackCanceled(true);
      gamePanel.playSE(POWER_UP);
      getPlayer().startDialogue(getPlayer(), HEALING_POOL);
      getPlayer().setCurrentLife(getPlayer().getMaxLife());
      getPlayer().setMana(getPlayer().getMaxMana());
      gamePanel.getAssetSetter().setMonster();
      gamePanel.getSaveLoad().save();
    }
  }

  private Player getPlayer() {
    return gamePanel.getPlayer();
  }
}
