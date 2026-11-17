package org.savilusGame.world;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.savilusGame.ai.NavigationGrid;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.tile.TileManager;

/**
 * Owns the per-map entity state of the game world: objects, NPCs, monsters,
 * interactive tiles, projectiles, particles, the currently active map and the
 * boss/event flags.
 * <p>The tile layer itself stays in {@link TileManager}, which is injected via the
 * constructor (the game never reaches it through a singleton anymore).
 * <p>Map switches are performed through {@link #switchMap(String)}; boss/event flags
 * are stored here so they survive map switches. Not thread-safe; confined to the
 * game loop thread like the rest of the simulation.
 */
public class World {

  private String currentMap;
  private final TileManager tileManager;

  private final WorldNavigation navigation = new WorldNavigation();
  private final Map<String, List<InteractiveObject>> mapsObjects = new java.util.HashMap<>();
  private final Map<String, List<ItemDrop>> mapsItems = new java.util.HashMap<>();
  private final Map<String, List<Actor>> mapsNpc = new java.util.HashMap<>();
  private final Map<String, List<Actor>> mapsMonsters = new java.util.HashMap<>();
  private final Map<String, List<InteractiveTile>> mapsInteractiveTiles = new java.util.HashMap<>();
  private final Map<String, List<Actor>> projectiles = new java.util.HashMap<>();

  private final List<Actor> particleList = new ArrayList<>();
  private boolean bossBattleOn;
  private boolean skeletonLordDefeated;

  public World(TileManager tileManager, String initialMap) {
    this.tileManager = Objects.requireNonNull(tileManager);
    this.currentMap = Objects.requireNonNull(initialMap);
    tileManager.getGameMaps().keySet().forEach(this::ensureMap);
    ensureMap(initialMap);
  }

  // ------------------------------------------------------------------
  // Tile manager (tile layer) - injected, not looked up
  // ------------------------------------------------------------------
  public TileManager tileManager() {
    return tileManager;
  }

  // ------------------------------------------------------------------
  // Current map
  // ------------------------------------------------------------------
  public String getCurrentMap() {
    return currentMap;
  }

  /**
   * Switch the active map. Only changes which map is considered current; moving the
   * player, fading and music are the caller's responsibility.
   */
  public void switchMap(String mapName) {
    this.currentMap = Objects.requireNonNull(mapName);
    ensureMap(mapName);
  }

  // ------------------------------------------------------------------
  // Per-map raw access (save/load, asset seeding)
  // ------------------------------------------------------------------
  public Map<String, List<InteractiveObject>> mapsObjects() {
    return mapsObjects;
  }

  public Map<String, List<ItemDrop>> mapsItems() {
    return mapsItems;
  }

  public Map<String, List<Actor>> mapsNpc() {
    return mapsNpc;
  }

  public Map<String, List<Actor>> mapsMonsters() {
    return mapsMonsters;
  }

  public Map<String, List<InteractiveTile>> mapsInteractiveTiles() {
    return Collections.unmodifiableMap(mapsInteractiveTiles);
  }

  public Map<String, List<Actor>> projectiles() {
    return projectiles;
  }

  public List<InteractiveObject> objects(String mapName) {
    return mapsObjects.get(mapName);
  }

  public List<ItemDrop> items(String mapName) {
    return mapsItems.get(mapName);
  }

  public List<Actor> npcs(String mapName) {
    return mapsNpc.get(mapName);
  }

  public List<Actor> monsters(String mapName) {
    return mapsMonsters.get(mapName);
  }

  public List<InteractiveTile> interactiveTiles(String mapName) {
    return mapsInteractiveTiles.get(mapName);
  }

  public List<Actor> projectiles(String mapName) {
    return projectiles.get(mapName);
  }

  // ------------------------------------------------------------------
  // Current-map accessors
  // ------------------------------------------------------------------
  public List<InteractiveObject> currentObjects() {
    return mapsObjects.get(currentMap);
  }

  /** Dropped/pickup items lying on the ground of the current map. */
  public List<ItemDrop> currentItems() {
    return mapsItems.get(currentMap);
  }

  public List<Actor> currentNpc() {
    return mapsNpc.get(currentMap);
  }

  public List<Actor> currentMonsters() {
    return mapsMonsters.get(currentMap);
  }

  public List<InteractiveTile> currentInteractiveTiles() {
    return mapsInteractiveTiles.get(currentMap);
  }

  public List<Actor> currentProjectiles() {
    return projectiles.get(currentMap);
  }

  public List<Actor> particleList() {
    return particleList;
  }

  /** All collision bodies (objects + items + NPCs + monsters) present on the given map. */
  public List<CollisionBody> entitiesOn(String mapName) {
    List<CollisionBody> result = new ArrayList<>();
    List<InteractiveObject> objects = mapsObjects.get(mapName);
    if (objects != null) {
      result.addAll(objects);
    }
    List<ItemDrop> items = mapsItems.get(mapName);
    if (items != null) {
      result.addAll(items);
    }
    List<Actor> npcs = mapsNpc.get(mapName);
    if (npcs != null) {
      result.addAll(npcs);
    }
    List<Actor> monsters = mapsMonsters.get(mapName);
    if (monsters != null) {
      result.addAll(monsters);
    }
    return result;
  }

  // ------------------------------------------------------------------
  // Seeding / mutation helpers
  // ------------------------------------------------------------------
  public void setObjects(Map<String, List<InteractiveObject>> maps) {
    replaceMaps(mapsObjects, maps);
  }

  public void setItems(Map<String, List<ItemDrop>> maps) {
    replaceMaps(mapsItems, maps);
  }

  public void setNpcs(Map<String, List<Actor>> maps) {
    replaceMaps(mapsNpc, maps);
  }

  public void setMonsters(Map<String, List<Actor>> maps) {
    replaceMaps(mapsMonsters, maps);
  }

  public void setInteractiveTiles(Map<String, List<InteractiveTile>> maps) {
    maps.keySet().forEach(this::ensureMap);
    mapsInteractiveTiles.replaceAll((map, previous) -> immutableTiles(maps.getOrDefault(map, List.of())));
  }

  /** Publish the replacement and its passability together; destroyed walls leave a null slot. */
  public void replaceInteractiveTile(String map, int index, InteractiveTile replacement) {
    var updated = new ArrayList<>(interactiveTiles(map));
    updated.set(index, replacement);
    mapsInteractiveTiles.put(map, Collections.unmodifiableList(updated));
  }

  public NavigationGrid navigationGrid() {
    return navigation.grid(tileManager.getGameMaps().get(currentMap), tileManager.getTile(), currentInteractiveTiles());
  }

  public void setProjectiles(Map<String, List<Actor>> maps) {
    replaceMaps(projectiles, maps);
  }

  public void spawnMonster(String mapName, Actor monster) {
    ensureMap(mapName);
    mapsMonsters.get(mapName).add(monster);
  }

  /** Remove the actor from every actor list of every map (monsters, NPCs, projectiles). */
  public boolean removeEntity(Actor entity) {
    boolean removed = false;
    for (List<Actor> list : mapsNpc.values()) {
      removed |= list.remove(entity);
    }
    for (List<Actor> list : mapsMonsters.values()) {
      removed |= list.remove(entity);
    }
    for (List<Actor> list : projectiles.values()) {
      removed |= list.remove(entity);
    }
    return removed;
  }

  /** Remove the object from every object list of every map. */
  public boolean removeObject(InteractiveObject object) {
    boolean removed = false;
    for (List<InteractiveObject> list : mapsObjects.values()) {
      removed |= list.remove(object);
    }
    return removed;
  }

  /** Place an item as a pick-up drop at the given position on the current map. */
  public void dropItem(Item item, int worldX, int worldY) {
    ItemDrop drop = new ItemDrop(tileManager.getGamePanel(), item)
        .setWorldPosition(worldX, worldY);
    drop.setTemporaryObject(true);
    mapsItems.computeIfAbsent(currentMap, key -> new ArrayList<>()).add(drop);
  }

  /**
   * Register an item drop that is part of the authored world (asset seeding or save restore).
   * Authored drops survive game resets and take part in save/load.
   */
  public void seedItemDrop(String mapName, ItemDrop drop) {
    ensureMap(mapName);
    mapsItems.get(mapName).add(drop);
  }

  /** Remove only the runtime-spawned item drops from every map (used when rebuilding from a save). */
  public void clearTemporaryItems() {
    for (List<ItemDrop> allMapItems : mapsItems.values()) {
      if (allMapItems == null || allMapItems.isEmpty()) {
        continue;
      }
      allMapItems.removeIf(drop -> drop != null && drop.isTemporaryObject());
    }
  }

  /** Remove all runtime-spawned (temporary) objects and items from every map. */
  public void removeTemporaryObjects() {
    for (List<InteractiveObject> allMapObjects : mapsObjects.values()) {
      if (allMapObjects == null || allMapObjects.isEmpty()) {
        continue;
      }
      allMapObjects.removeIf(object -> object != null && object.isTemporaryObject());
    }
    for (List<Actor> allNpcs : mapsNpc.values()) {
      if (allNpcs == null || allNpcs.isEmpty()) {
        continue;
      }
      allNpcs.removeIf(actor -> actor != null && actor.isTemporaryObject());
    }
    for (List<ItemDrop> allMapItems : mapsItems.values()) {
      if (allMapItems == null || allMapItems.isEmpty()) {
        continue;
      }
      allMapItems.removeIf(drop -> drop != null && drop.isTemporaryObject());
    }
  }

  // ------------------------------------------------------------------
  // Boss / event flags (survive map switches)
  // ------------------------------------------------------------------
  public boolean isBossBattleOn() {
    return bossBattleOn;
  }

  public void setBossBattleOn(boolean bossBattleOn) {
    this.bossBattleOn = bossBattleOn;
  }

  public boolean isSkeletonLordDefeated() {
    return skeletonLordDefeated;
  }

  public void setSkeletonLordDefeated(boolean skeletonLordDefeated) {
    this.skeletonLordDefeated = skeletonLordDefeated;
  }

  private void ensureMap(String mapName) {
    mapsObjects.computeIfAbsent(mapName, key -> new ArrayList<>());
    mapsItems.computeIfAbsent(mapName, key -> new ArrayList<>());
    mapsNpc.computeIfAbsent(mapName, key -> new ArrayList<>());
    mapsMonsters.computeIfAbsent(mapName, key -> new ArrayList<>());
    mapsInteractiveTiles.putIfAbsent(mapName, List.of());
    projectiles.computeIfAbsent(mapName, key -> new ArrayList<>());
  }

  private static List<InteractiveTile> immutableTiles(List<InteractiveTile> tiles) {
    // List.copyOf rejects the null slots used for destroyed walls.
    return Collections.unmodifiableList(new ArrayList<>(tiles));
  }

  private <T> void replaceMaps(Map<String, List<T>> target, Map<String, List<T>> maps) {
    maps.keySet().forEach(this::ensureMap);
    for (String mapName : List.copyOf(target.keySet())) {
      List<T> entries = maps.containsKey(mapName)
          ? Objects.requireNonNull(maps.get(mapName), "Null entity list for map: " + mapName)
          : List.of();
      target.put(mapName, new ArrayList<>(entries));
    }
  }
}
