package org.savilusGame.data;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_FIRST_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.data.SaveGame.DropState;
import org.savilusGame.data.SaveGame.EventPosition;
import org.savilusGame.data.SaveGame.ItemState;
import org.savilusGame.data.SaveGame.MapState;
import org.savilusGame.data.SaveGame.ObjectState;
import org.savilusGame.data.SaveGame.PlayerState;
import org.savilusGame.data.SaveGame.TileKind;
import org.savilusGame.data.SaveGame.TileState;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.Player;
import org.savilusGame.entity.interactiveTile.DestructibleWall;
import org.savilusGame.entity.interactiveTile.InteractiveDryTree;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.entity.interactiveTile.MetalPlate;
import org.savilusGame.entity.interactiveTile.Trunk;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.entity.items.Chest;
import org.savilusGame.enums.Area;
import org.savilusGame.enums.GameObject;
import org.savilusGame.enums.WorldGameTypes;
import org.savilusGame.utils.text.TextManager;

import lombok.extern.slf4j.Slf4j;

/**
 * Takes and restores player and per-map snapshots. The save is decoded and validated,
 * including object construction, before any live game state is changed; if applying the
 * prepared save still fails, the captured {@link LiveStateBackup} puts the previous state back.
 */
@Slf4j
public class SaveLoad {
  static final String SAVE_FILE_NAME = "save.json";
  static final String LEGACY_SAVE_FILE_NAME = "save.dat";

  private final GamePanel gamePanel;
  private final SaveFile file;
  private final List<Path> legacyPaths;

  public SaveLoad(GamePanel gamePanel) {
    // New saves live in the per-user data directory. The working directory is only consulted
    // for the one-time import of a legacy save.dat written by older builds.
    this(gamePanel, UserDataDirectory.resolve().resolve(SAVE_FILE_NAME),
        List.of(UserDataDirectory.resolve().resolve(LEGACY_SAVE_FILE_NAME),
            Path.of(LEGACY_SAVE_FILE_NAME)));
  }

  SaveLoad(GamePanel gamePanel, Path savePath, Path legacyPath) {
    this(gamePanel, savePath, List.of(legacyPath));
  }

  SaveLoad(GamePanel gamePanel, Path savePath, List<Path> legacyPaths) {
    this.gamePanel = Objects.requireNonNull(gamePanel);
    this.file = new SaveFile(savePath);
    this.legacyPaths = List.copyOf(legacyPaths);
  }

  public boolean save() {
    try {
      file.write(snapshot());
      return true;
    } catch (IOException | IllegalArgumentException | IllegalStateException error) {
      log.error("Unable to save game", error);
      gamePanel.getUi().addMessage(TextManager.getUiText("uiMessages", "saveFailed"));
      return false;
    }
  }

  public boolean load() {
    PreparedSave prepared;
    try {
      SaveGame save;
      boolean importingLegacy = !file.exists();
      if (!importingLegacy) {
        save = file.read();
      } else {
        save = LegacySaveImporter.read(existingLegacySave(), snapshot(), gamePanel);
      }
      prepared = prepare(save);
      if (importingLegacy) {
        file.write(save);
      }
    } catch (IOException | IllegalArgumentException | IllegalStateException error) {
      log.error("Unable to load game; current game state was not replaced", error);
      gamePanel.getUi().addMessage(TextManager.getUiText("uiMessages", "loadFailed"));
      return false;
    }
    LiveStateBackup backup = null;
    try {
      backup = LiveStateBackup.capture(gamePanel);
      apply(prepared);
      return true;
    } catch (RuntimeException error) {
      log.error("Unable to apply the loaded game; restoring the previous state", error);
      if (backup != null) {
        try {
          backup.restore();
        } catch (RuntimeException restoreError) {
          log.error("Could not fully restore the previous game state after a failed load", restoreError);
        }
      }
      gamePanel.getUi().addMessage(TextManager.getUiText("uiMessages", "loadFailed"));
      return false;
    }
  }

  /** First legacy save that exists, or the preferred location so the error names a real path. */
  private Path existingLegacySave() {
    return legacyPaths.stream()
        .filter(Files::exists)
        .findFirst()
        .orElse(legacyPaths.getFirst());
  }

  private SaveGame snapshot() {
    var world = gamePanel.getWorld();
    Player player = gamePanel.getPlayer();
    List<ItemState> inventory = player.getInventory().stream()
        .map(item -> new ItemState(item.getId(), item.getStackCount())).toList();
    PlayerState playerState = new PlayerState(
        player.getWorldX(), player.getWorldY(), player.getDirection(), player.getLevel(),
        player.getMaxLife(), player.getCurrentLife(), player.getMana(), player.getMaxMana(),
        player.getStrength(), player.getDexterity(), player.getExp(), player.getNextLevelExp(),
        player.getMoney(), inventory, player.getCurrentWeaponSlot(), player.getCurrentShieldSlot(),
        player.getInventory().indexOf(player.getCurrentLightItem()));

    Map<String, MapState> maps = new HashMap<>();
    for (String map : gamePanel.getTileManager().getGameMaps().keySet()) {
      List<ObjectState> objects = new ArrayList<>();
      for (InteractiveObject object : world.objects(map)) {
        if (object == null || object.isTemporaryObject()) continue;
        if (object.getGameObject() == null) {
          throw new IllegalStateException("Cannot save object without an identity on map " + map);
        }
        objects.add(new ObjectState(object.getGameObject().name(), object.getWorldX(), object.getWorldY(),
            object.isOpened(), object.getLoot() == null ? null : object.getLoot().getId()));
      }
      List<DropState> drops = new ArrayList<>();
      for (ItemDrop drop : world.items(map)) {
        if (drop != null) {
          drops.add(new DropState(drop.getItem().getId(), drop.getItem().getStackCount(),
              drop.getWorldX(), drop.getWorldY(), drop.isTemporaryObject()));
        }
      }
      List<TileState> tiles = new ArrayList<>();
      for (InteractiveTile tile : world.interactiveTiles(map)) {
        if (tile == null) {
          tiles.add(null);
        } else {
          TileKind kind = switch (tile) {
            case InteractiveDryTree ignored -> TileKind.DRY_TREE;
            case Trunk ignored -> TileKind.TRUNK;
            case DestructibleWall ignored -> TileKind.WALL;
            case MetalPlate ignored -> TileKind.METAL_PLATE;
            default -> throw new IllegalStateException("Cannot save interactive tile " + tile.getClass().getName());
          };
          tiles.add(new TileState(kind, tile.getWorldX() / TILE_SIZE, tile.getWorldY() / TILE_SIZE,
              tile.getCurrentLife(), tile.isInvincible()));
        }
      }
      boolean[][] eventGrid = gamePanel.getEventHandler().completedEvents(map);
      List<EventPosition> events = new ArrayList<>();
      for (int col = 0; col < eventGrid.length; col++) {
        for (int row = 0; row < eventGrid[col].length; row++) {
          if (eventGrid[col][row]) events.add(new EventPosition(col, row));
        }
      }
      maps.put(map, new MapState(objects, drops, tiles, events));
    }
    return new SaveGame(SaveGame.SCHEMA_VERSION, world.getCurrentMap(), playerState,
        world.isSkeletonLordDefeated(), maps);
  }

  private record PreparedSave(
      SaveGame save, List<Item> inventory, Item weapon, Item shield, Item light,
      Map<String, List<InteractiveObject>> objects, Map<String, List<ItemDrop>> drops,
      Map<String, List<InteractiveTile>> tiles, Map<String, boolean[][]> events
  ) {}

  private PreparedSave prepare(SaveGame save) {
    require(save != null && save.schemaVersion() == SaveGame.SCHEMA_VERSION, "Unsupported save version");
    require(save.maps() != null, "Missing map data");
    require(save.currentMap() != null && gamePanel.getTileManager().getGameMaps().containsKey(save.currentMap()),
        "Unknown current map");
    areaForMap(save.currentMap());
    PlayerState p = save.player();
    require(p != null && p.direction() != null && p.direction() != org.savilusGame.enums.Direction.ANY,
        "Invalid player state");
    require(p.level() > 0 && p.maxLife() > 0 && p.life() >= 0 && p.life() <= p.maxLife()
        && p.maxMana() >= 0 && p.mana() >= 0 && p.mana() <= p.maxMana()
        && p.strength() >= 0 && p.dexterity() >= 0 && p.exp() >= 0 && p.nextLevelExp() > 0
        && p.money() >= 0 && p.inventory() != null, "Invalid player statistics");
    validatePosition(p.worldX(), p.worldY());

    List<Item> inventory = new ArrayList<>();
    List<Item> originalSlots = new ArrayList<>();
    for (ItemState state : p.inventory()) {
      require(state != null && state.id() != null && state.amount() > 0, "Invalid inventory item");
      Item item = gamePanel.getGameEntityFactory().getItem(state.id());
      if (item == null) {
        log.warn("Skipping unknown inventory item '{}'", state.id());
      } else {
        item.setStackCount(state.amount());
        inventory.add(item);
      }
      originalSlots.add(item);
    }
    Item weapon = itemAt(originalSlots, p.weaponSlot());
    if (weapon == null || (weapon.getType() != WorldGameTypes.SWORD
        && weapon.getType() != WorldGameTypes.AXE && weapon.getType() != WorldGameTypes.PICKAXE)) {
      log.warn("Saved weapon slot {} is invalid; using the default sword", p.weaponSlot());
      weapon = gamePanel.getGameEntityFactory().getItem(GameObject.NORMAL_SWORD.name());
      inventory.add(weapon);
    }
    Item shield = itemAt(originalSlots, p.shieldSlot());
    if (shield == null || shield.getType() != WorldGameTypes.SHIELD) {
      log.warn("Saved shield slot {} is invalid; using the default shield", p.shieldSlot());
      shield = gamePanel.getGameEntityFactory().getItem(GameObject.WOOD_SHIELD.name());
      inventory.add(shield);
    }
    Item light = itemAt(originalSlots, p.lightSlot());
    if (light != null && light.getType() != WorldGameTypes.LIGHTING) {
      throw new IllegalArgumentException("Invalid light item slot");
    }

    Map<String, List<InteractiveObject>> objects = new HashMap<>();
    Map<String, List<ItemDrop>> drops = new HashMap<>();
    Map<String, List<InteractiveTile>> tiles = new HashMap<>();
    Map<String, boolean[][]> events = new HashMap<>();
    Map<String, MapState> mapStates = new HashMap<>(snapshot().maps());
    for (var entry : save.maps().entrySet()) {
      if (mapStates.containsKey(entry.getKey())) {
        mapStates.put(entry.getKey(), entry.getValue());
      } else {
        log.warn("Skipping unknown saved map '{}'", entry.getKey());
      }
    }
    for (String map : mapStates.keySet()) {
      if (!save.maps().containsKey(map)) {
        log.warn("Saved data has no map '{}'; keeping its current state", map);
      }
    }
    for (var entry : mapStates.entrySet()) {
      String map = entry.getKey();
      MapState state = entry.getValue();
      require(state != null && state.objects() != null && state.drops() != null
          && state.interactiveTiles() != null && state.completedEvents() != null, "Invalid map state: " + map);
      List<InteractiveObject> mapObjects = new ArrayList<>();
      for (ObjectState object : state.objects()) {
        require(object != null && object.id() != null, "Invalid object on " + map);
        validatePosition(object.worldX(), object.worldY());
        InteractiveObject instance = gamePanel.getGameEntityFactory().getWorldObject(object.id());
        if (instance == null) {
          log.warn("Skipping unknown map object '{}' on {}", object.id(), map);
          continue;
        }
        if (instance instanceof Chest chest) {
          Item loot = object.lootId() == null ? null : gamePanel.getGameEntityFactory().getItem(object.lootId());
          if (loot == null && !object.opened()) {
            log.warn("Skipping unopened chest with unknown loot '{}' on {}", object.lootId(), map);
            continue;
          }
          chest.setLoot(loot);
        }
        instance.setWorldX(object.worldX());
        instance.setWorldY(object.worldY());
        instance.setOpened(object.opened());
        if (instance.isOpened() && instance.getMainImage2() != null) instance.setMainImage(instance.getMainImage2());
        mapObjects.add(instance);
      }
      objects.put(map, mapObjects);

      List<ItemDrop> mapDrops = new ArrayList<>();
      for (DropState drop : state.drops()) {
        require(drop != null && drop.id() != null && drop.amount() > 0, "Invalid item drop on " + map);
        validatePosition(drop.worldX(), drop.worldY());
        Item item = gamePanel.getGameEntityFactory().getItem(drop.id());
        if (item == null) {
          log.warn("Skipping unknown item drop '{}' on {}", drop.id(), map);
          continue;
        }
        item.setStackCount(drop.amount());
        ItemDrop instance = new ItemDrop(gamePanel, item).setWorldPosition(drop.worldX(), drop.worldY());
        instance.setTemporaryObject(drop.temporary());
        mapDrops.add(instance);
      }
      drops.put(map, mapDrops);

      List<InteractiveTile> mapTiles = new ArrayList<>();
      for (TileState tile : state.interactiveTiles()) {
        if (tile == null) {
          mapTiles.add(null);
          continue;
        }
        require(tile.kind() != null && tile.col() >= 0 && tile.col() < gamePanel.getMaxWorldCol()
            && tile.row() >= 0 && tile.row() < gamePanel.getMaxWorldRow(), "Invalid tile on " + map);
        InteractiveTile instance = switch (tile.kind()) {
          case DRY_TREE -> new InteractiveDryTree(gamePanel, tile.col(), tile.row());
          case TRUNK -> new Trunk(gamePanel, tile.col(), tile.row());
          case WALL -> new DestructibleWall(gamePanel, tile.col(), tile.row());
          case METAL_PLATE -> new MetalPlate(gamePanel, tile.col(), tile.row());
        };
        instance.setCurrentLife(tile.life());
        instance.setInvincible(tile.invincible());
        mapTiles.add(instance);
      }
      tiles.put(map, mapTiles);

      boolean[][] grid = new boolean[gamePanel.getMaxWorldCol()][gamePanel.getMaxWorldRow()];
      for (EventPosition event : state.completedEvents()) {
        require(event != null && event.col() >= 0 && event.col() < grid.length
            && event.row() >= 0 && event.row() < grid[event.col()].length, "Invalid event on " + map);
        grid[event.col()][event.row()] = true;
      }
      events.put(map, grid);
    }
    return new PreparedSave(save, inventory, weapon, shield, light, objects, drops, tiles, events);
  }

  private static Item itemAt(List<Item> items, int index) {
    return index >= 0 && index < items.size() ? items.get(index) : null;
  }

  private void validatePosition(int x, int y) {
    require(x >= 0 && y >= 0 && x < gamePanel.getMaxWorldCol() * TILE_SIZE
        && y < gamePanel.getMaxWorldRow() * TILE_SIZE, "Position outside map");
  }

  private static void require(boolean valid, String message) {
    if (!valid) throw new IllegalArgumentException(message);
  }

  private void apply(PreparedSave prepared) {
    SaveGame save = prepared.save();
    PlayerState p = save.player();
    Player player = gamePanel.getPlayer();
    var world = gamePanel.getWorld();
    world.setObjects(prepared.objects());
    world.setItems(prepared.drops());
    world.setInteractiveTiles(prepared.tiles());
    prepared.events().forEach(gamePanel.getEventHandler()::restoreCompletedEvents);
    world.setSkeletonLordDefeated(save.skeletonLordDefeated());
    world.setBossBattleOn(false);
    gamePanel.getAssetSetter().setMonster();
    gamePanel.getAssetSetter().setNPC();
    gamePanel.getAssetSetter().setInteractiveObjects();
    world.switchMap(save.currentMap());
    gamePanel.getEventHandler().setPreviousEventX(p.worldX());
    gamePanel.getEventHandler().setPreviousEventY(p.worldY());
    gamePanel.getEventHandler().setCanTouchEvent(false);
    player.setWorldPosition(p.worldX(), p.worldY());
    player.setDirection(p.direction());
    player.setLevel(p.level());
    player.setMaxLife(p.maxLife());
    player.setCurrentLife(p.life());
    player.setMaxMana(p.maxMana());
    player.setMana(p.mana());
    player.setStrength(p.strength());
    player.setDexterity(p.dexterity());
    player.setExp(p.exp());
    player.setNextLevelExp(p.nextLevelExp());
    player.setMoney(p.money());
    player.getInventory().clear();
    player.getInventory().addAll(prepared.inventory());
    player.setCurrentWeapon(prepared.weapon());
    player.setCurrentShield(prepared.shield());
    player.setCurrentLightItem(prepared.light());
    player.setLightUpdated(true);
    player.getAttack();
    player.getDefense();
    player.getAttackImage();
    Area area = areaForMap(save.currentMap());
    gamePanel.setCurrentArea(area);
    gamePanel.setNextArea(area);
  }

  private static Area areaForMap(String map) {
    return MAIN_MAP.equals(map) ? Area.OUTSIDE
        : INTERIOR_MAP.equals(map) ? Area.INDOOR
        : DUNGEON_FIRST_FLOR.equals(map) || DUNGEON_SECOND_FLOR.equals(map)
            ? Area.DUNGEON : throwUnknownMap(map);
  }

  private static Area throwUnknownMap(String map) {
    throw new IllegalArgumentException("Unknown area for map: " + map);
  }
}
