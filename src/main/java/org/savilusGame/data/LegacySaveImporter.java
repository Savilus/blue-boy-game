package org.savilusGame.data;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.savilusGame.GamePanel;
import org.savilusGame.data.SaveGame.DropState;
import org.savilusGame.data.SaveGame.ItemState;
import org.savilusGame.data.SaveGame.MapState;
import org.savilusGame.data.SaveGame.ObjectState;
import org.savilusGame.data.SaveGame.PlayerState;
import org.savilusGame.data.SaveGame.TileKind;
import org.savilusGame.data.SaveGame.TileState;

/** Reads the previous local save format once; no binary files are written. */
final class LegacySaveImporter {
  private LegacySaveImporter() {}

  static SaveGame read(Path path, SaveGame defaults, GamePanel panel) throws IOException {
    DataStorage old;
    try (ObjectInputStream input = new ObjectInputStream(Files.newInputStream(path))) {
      input.setObjectInputFilter(ObjectInputFilter.Config.createFilter(
          "maxdepth=40;maxrefs=100000;maxarray=100000;"
              + "org.savilusGame.data.DataStorage;java.util.HashMap;java.util.ArrayList;"
              + "java.util.Map$Entry;java.lang.Object;java.lang.Integer;java.lang.Number;"
              + "java.lang.Boolean;java.lang.String;!*"));
      Object value = input.readObject();
      if (!(value instanceof DataStorage storage)) throw new InvalidObjectException("Invalid legacy save");
      old = storage;
    } catch (ClassNotFoundException error) {
      throw new IOException("Cannot read legacy save", error);
    }

    if (old.getItemNames() == null || old.getItemAmounts() == null
        || old.getItemNames().size() != old.getItemAmounts().size()) {
      throw new InvalidObjectException("Invalid legacy inventory");
    }
    List<ItemState> inventory = new ArrayList<>();
    for (int i = 0; i < old.getItemNames().size(); i++) {
      if (old.getItemNames().get(i) == null || old.getItemAmounts().get(i) == null) {
        throw new InvalidObjectException("Invalid legacy inventory entry " + i);
      }
      inventory.add(new ItemState(itemId(panel, old.getItemNames().get(i)), old.getItemAmounts().get(i)));
    }
    PlayerState initial = defaults.player();
    PlayerState player = new PlayerState(initial.worldX(), initial.worldY(), initial.direction(),
        old.getLevel(), old.getMaxLife(), old.getLife(), old.getMana(), old.getMaxMana(),
        old.getStrength(), old.getDexterity(), old.getExp(), old.getNextLevelExp(), old.getMoney(),
        inventory, old.getCurrentWeaponSlot(), old.getCurrentShieldSlot(), -1);

    Map<String, MapState> maps = new HashMap<>();
    for (var entry : defaults.maps().entrySet()) {
      String map = entry.getKey();
      MapState base = entry.getValue();
      List<ObjectState> objects = new ArrayList<>(base.objects());
      List<DropState> drops = new ArrayList<>(base.drops());
      if (old.getMapObjectNames() != null && old.getMapObjectNames().containsKey(map)) {
        List<String> names = requiredList(old.getMapObjectNames(), map);
        List<Integer> xs = values(old.getMapObjectWorldX(), map, names.size());
        List<Integer> ys = values(old.getMapObjectWorldY(), map, names.size());
        List<String> loot = values(old.getMapObjectLootNames(), map, names.size());
        List<Boolean> opened = values(old.getMapObjectOpened(), map, names.size());
        objects.clear();
        for (int i = 0; i < names.size(); i++) {
          String name = names.get(i);
          if ("NA".equals(name)) continue;
          if (name == null || xs.get(i) == null || ys.get(i) == null || opened.get(i) == null) {
            throw new InvalidObjectException("Invalid legacy object on " + map);
          }
          var object = panel.getGameEntityFactory().getWorldObject(name);
          if (object != null) {
            objects.add(new ObjectState(object.getGameObject().name(), xs.get(i), ys.get(i),
                opened.get(i), loot.get(i) == null ? null : itemId(panel, loot.get(i))));
          } else if (panel.getGameEntityFactory().isItem(name)) {
            drops.add(new DropState(itemId(panel, name), 1, xs.get(i), ys.get(i), true));
          } else {
            objects.add(new ObjectState(name, xs.get(i), ys.get(i), opened.get(i), loot.get(i)));
          }
        }
      }

      if (old.getMapItemDropNames() != null && old.getMapItemDropNames().containsKey(map)) {
        List<String> names = requiredList(old.getMapItemDropNames(), map);
        List<Integer> xs = values(old.getMapItemDropWorldX(), map, names.size());
        List<Integer> ys = values(old.getMapItemDropWorldY(), map, names.size());
        for (int i = 0; i < names.size(); i++) {
          if (names.get(i) == null || xs.get(i) == null || ys.get(i) == null) {
            throw new InvalidObjectException("Invalid legacy item drop on " + map);
          }
          drops.add(new DropState(itemId(panel, names.get(i)), 1, xs.get(i), ys.get(i), true));
        }
      }

      List<TileState> tiles = new ArrayList<>(base.interactiveTiles());
      if (old.getMapInteractiveTilesCurrentLife() != null
          && old.getMapInteractiveTilesCurrentLife().containsKey(map)) {
        List<Integer> life = requiredList(old.getMapInteractiveTilesCurrentLife(), map);
        List<Boolean> invincible = values(old.getMapInteractiveTilesInvincible(), map, life.size());
        if (life.size() != tiles.size()) throw new InvalidObjectException("Legacy tile count differs on " + map);
        for (int i = 0; i < life.size(); i++) {
          if (life.get(i) == null || invincible.get(i) == null) {
            throw new InvalidObjectException("Invalid legacy tile on " + map);
          }
          TileState tile = tiles.get(i);
          if (tile == null) continue;
          if (life.get(i) <= 0 && tile.kind() == TileKind.WALL) {
            tiles.set(i, null);
          } else {
            TileKind kind = life.get(i) <= 0 && tile.kind() == TileKind.DRY_TREE ? TileKind.TRUNK : tile.kind();
            tiles.set(i, new TileState(kind, tile.col(), tile.row(), life.get(i), invincible.get(i)));
          }
        }
      }
      maps.put(map, new MapState(objects, drops, tiles, base.completedEvents()));
    }
    return new SaveGame(SaveGame.SCHEMA_VERSION, defaults.currentMap(), player, false, maps);
  }

  private static String itemId(GamePanel panel, String name) {
    var item = panel.getGameEntityFactory().getItem(name);
    return item == null ? name : item.getId();
  }

  private static <T> List<T> values(Map<String, List<T>> maps, String map, int expected)
      throws InvalidObjectException {
    if (maps == null || maps.get(map) == null || maps.get(map).size() != expected) {
      throw new InvalidObjectException("Invalid legacy map data for " + map);
    }
    return maps.get(map);
  }

  private static <T> List<T> requiredList(Map<String, List<T>> maps, String map)
      throws InvalidObjectException {
    if (maps.get(map) == null) throw new InvalidObjectException("Missing legacy map data for " + map);
    return maps.get(map);
  }
}
