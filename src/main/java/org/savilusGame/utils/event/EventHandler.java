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

import java.awt.*;
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

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventHandler {

  static final String DAMAGE_PIT = "damagePit";
  static final String HEALING_POOL = "healingPool";

  final GamePanel gamePanel;
  final Map<String, EventRect[][]> eventRect = new HashMap<>();
  String tempMap;
  int tempCol, tempRow;

  int previousEventX, previousEventY;
  boolean canTouchEvent = true;

  public EventHandler(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    eventRect.put(MAIN_MAP, new EventRect[gamePanel.getMaxWorldCol()][gamePanel.getMaxWorldRow()]);
    for (int row = 0; row < gamePanel.getMaxWorldRow(); row++) {
      for (int col = 0; col < gamePanel.getMaxWorldCol(); col++) {
        EventRect rect = new EventRect();
        rect.x = 23;
        rect.y = 23;
        rect.width = 2;
        rect.height = 2;
        rect.eventRectDefaultX = rect.x;
        rect.eventRectDefaultY = rect.y;

        eventRect.get(MAIN_MAP)[col][row] = rect;
      }
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

  private void handleMapEvents(String currentMap) {
    Map<String, Runnable> mapHandlers = new HashMap<>();
    mapHandlers.put(MAIN_MAP, this::handleMainMapEvents);
    mapHandlers.put(DUNGEON_FIRST_FLOR, this::handleDungeonFirstFloorEvents);
    mapHandlers.put(DUNGEON_SECOND_FLOR, this::handleDungeonSecondFloorEvents);
    mapHandlers.put(INTERIOR_MAP, this::handleInteriorMapEvents);

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
    else if (hit(12, 9, Direction.UP)) speak(gamePanel.getWorld().mapsNpc().get(INTERIOR_MAP).getFirst());
  }

  private void speak(Actor gameEntity) {
    if (gamePanel.getKeyHandler().isEnterPressed()) {
      gamePanel.setGameState(DIALOG_STATE);
      getPlayer().setAttackCanceled(true);
      gameEntity.speak();
    }
  }

  private boolean hit(int col, int row, Direction requiredDirection) {
    var playerSolidArea = new Rectangle(
        getPlayer().getWorldX() + getPlayer().getSolidArea().x,
        getPlayer().getWorldY() + getPlayer().getSolidArea().y,
        getPlayer().getSolidArea().width,
        getPlayer().getSolidArea().height
    );

    var eventArea = new Rectangle(
        col * TILE_SIZE + eventRect.get(MAIN_MAP)[col][row].x,
        row * TILE_SIZE + eventRect.get(MAIN_MAP)[col][row].y,
        eventRect.get(MAIN_MAP)[col][row].width,
        eventRect.get(MAIN_MAP)[col][row].height
    );

    boolean hitResult = playerSolidArea.intersects(eventArea) && !eventRect.get(MAIN_MAP)[col][row].oneTimeEventDone;

    if (hitResult && (getPlayer().getDirection().equals(requiredDirection) || requiredDirection.equals(Direction.ANY))) {
      previousEventX = getPlayer().getWorldX();
      previousEventY = getPlayer().getWorldY();
      return true;
    }

    resetPlayerSolidArea();
    resetEventArea(col, row);
    return false;
  }

  private void resetPlayerSolidArea() {
    getPlayer().getSolidArea().x = getPlayer().getSolidAreaDefaultX();
    getPlayer().getSolidArea().y = getPlayer().getSolidAreaDefaultY();
  }

  private void resetEventArea(int col, int row) {
    eventRect.get(MAIN_MAP)[col][row].x = eventRect.get(MAIN_MAP)[col][row].eventRectDefaultX;
    eventRect.get(MAIN_MAP)[col][row].y = eventRect.get(MAIN_MAP)[col][row].eventRectDefaultY;
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
