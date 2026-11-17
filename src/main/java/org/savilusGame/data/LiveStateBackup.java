package org.savilusGame.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.Player;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.Direction;

/**
 * Snapshot of the mutable live game state, used to roll back a save that could not be applied.
 * <p>Holds the existing entities by reference (never copies them) and copies every list and
 * primitive that {@link SaveLoad#apply} replaces, so a failed application cannot leave the
 * player with a half-restored world, inventory or map. Capturing and restoring are confined to
 * the game thread, like the rest of the simulation.
 */
final class LiveStateBackup {

  private final GamePanel gamePanel;

  private final Map<String, List<InteractiveObject>> objects;
  private final Map<String, List<ItemDrop>> items;
  private final Map<String, List<Actor>> npcs;
  private final Map<String, List<Actor>> monsters;
  private final Map<String, List<InteractiveTile>> interactiveTiles;
  private final Map<String, boolean[][]> completedEvents;

  private final String currentMap;
  private final boolean skeletonLordDefeated;
  private final boolean bossBattleOn;
  private final int previousEventX;
  private final int previousEventY;
  private final boolean canTouchEvent;
  private final Area currentArea;
  private final Area nextArea;

  private final PlayerSnapshot player;

  private LiveStateBackup(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    var world = gamePanel.getWorld();
    this.objects = copy(world.mapsObjects());
    this.items = copy(world.mapsItems());
    this.npcs = copy(world.mapsNpc());
    this.monsters = copy(world.mapsMonsters());
    this.interactiveTiles = copy(world.mapsInteractiveTiles());

    Map<String, boolean[][]> events = new HashMap<>();
    for (String map : gamePanel.getTileManager().getGameMaps().keySet()) {
      events.put(map, gamePanel.getEventHandler().completedEvents(map));
    }
    this.completedEvents = events;

    this.currentMap = world.getCurrentMap();
    this.skeletonLordDefeated = world.isSkeletonLordDefeated();
    this.bossBattleOn = world.isBossBattleOn();
    this.previousEventX = gamePanel.getEventHandler().getPreviousEventX();
    this.previousEventY = gamePanel.getEventHandler().getPreviousEventY();
    this.canTouchEvent = gamePanel.getEventHandler().isCanTouchEvent();
    this.currentArea = gamePanel.getCurrentArea();
    this.nextArea = gamePanel.getNextArea();
    this.player = PlayerSnapshot.of(gamePanel.getPlayer());
  }

  static LiveStateBackup capture(GamePanel gamePanel) {
    return new LiveStateBackup(Objects.requireNonNull(gamePanel));
  }

  /** Puts every captured list, flag and player field back; safe to call once. */
  void restore() {
    var world = gamePanel.getWorld();
    world.setObjects(objects);
    world.setItems(items);
    world.setNpcs(npcs);
    world.setMonsters(monsters);
    world.setInteractiveTiles(interactiveTiles);
    completedEvents.forEach(gamePanel.getEventHandler()::restoreCompletedEvents);
    world.switchMap(currentMap);
    world.setSkeletonLordDefeated(skeletonLordDefeated);
    world.setBossBattleOn(bossBattleOn);

    var eventHandler = gamePanel.getEventHandler();
    eventHandler.setPreviousEventX(previousEventX);
    eventHandler.setPreviousEventY(previousEventY);
    eventHandler.setCanTouchEvent(canTouchEvent);

    player.restore(gamePanel.getPlayer());

    gamePanel.setCurrentArea(currentArea);
    gamePanel.setNextArea(nextArea);
    gamePanel.getGameMap().createWorldMap();
  }

  private static <T> Map<String, List<T>> copy(Map<String, List<T>> source) {
    Map<String, List<T>> copy = new HashMap<>();
    source.forEach((map, entries) -> copy.put(map, entries == null ? new ArrayList<>() : new ArrayList<>(entries)));
    return copy;
  }

  private record PlayerSnapshot(
      int worldX, int worldY, Direction direction,
      int level, int maxLife, int currentLife, int maxMana, int mana,
      int strength, int dexterity, int exp, int nextLevelExp, int money,
      List<Item> inventory, Item weapon, Item shield, Item light, boolean lightUpdated
  ) {

    static PlayerSnapshot of(Player player) {
      return new PlayerSnapshot(player.getWorldX(), player.getWorldY(), player.getDirection(),
          player.getLevel(), player.getMaxLife(), player.getCurrentLife(), player.getMaxMana(),
          player.getMana(), player.getStrength(), player.getDexterity(), player.getExp(),
          player.getNextLevelExp(), player.getMoney(), new ArrayList<>(player.getInventory()),
          player.getCurrentWeapon(), player.getCurrentShield(), player.getCurrentLightItem(),
          player.isLightUpdated());
    }

    void restore(Player player) {
      player.setWorldPosition(worldX, worldY);
      player.setDirection(direction);
      player.setLevel(level);
      player.setMaxLife(maxLife);
      player.setCurrentLife(currentLife);
      player.setMaxMana(maxMana);
      player.setMana(mana);
      player.setStrength(strength);
      player.setDexterity(dexterity);
      player.setExp(exp);
      player.setNextLevelExp(nextLevelExp);
      player.setMoney(money);
      player.setCurrentWeapon(weapon);
      player.setCurrentShield(shield);
      player.setCurrentLightItem(light);
      player.setLightUpdated(lightUpdated);
      player.getInventory().clear();
      player.getInventory().addAll(inventory);
      // Re-derive the combat statistics from the restored equipment.
      player.getAttack();
      player.getDefense();
    }
  }
}
