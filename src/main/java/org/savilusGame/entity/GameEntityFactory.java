package org.savilusGame.entity;

import static org.savilusGame.enums.GameObject.AXE;
import static org.savilusGame.enums.GameObject.BLUE_HEART;
import static org.savilusGame.enums.GameObject.BLUE_SHIELD;
import static org.savilusGame.enums.GameObject.BOOTS;
import static org.savilusGame.enums.GameObject.BRONZE_COIN;
import static org.savilusGame.enums.GameObject.CHEST;
import static org.savilusGame.enums.GameObject.DOOR;
import static org.savilusGame.enums.GameObject.HEART;
import static org.savilusGame.enums.GameObject.IRON_DOOR;
import static org.savilusGame.enums.GameObject.KEY;
import static org.savilusGame.enums.GameObject.LANTERN;
import static org.savilusGame.enums.GameObject.MANA_CRYSTAL;
import static org.savilusGame.enums.GameObject.NORMAL_SWORD;
import static org.savilusGame.enums.GameObject.PICKAXE;
import static org.savilusGame.enums.GameObject.RED_POTION;
import static org.savilusGame.enums.GameObject.TENT;
import static org.savilusGame.enums.GameObject.WOOD_SHIELD;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.items.BlueHeart;
import org.savilusGame.entity.items.Boots;
import org.savilusGame.entity.items.BronzeCoin;
import org.savilusGame.entity.items.Chest;
import org.savilusGame.entity.items.Door;
import org.savilusGame.entity.items.Heart;
import org.savilusGame.entity.items.IronDoor;
import org.savilusGame.entity.items.Key;
import org.savilusGame.entity.items.Lantern;
import org.savilusGame.entity.items.ManaCrystal;
import org.savilusGame.entity.items.RedPotion;
import org.savilusGame.entity.items.Tent;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.shield.BlueShield;
import org.savilusGame.entity.shield.WoodShield;
import org.savilusGame.entity.weapon.Axe;
import org.savilusGame.entity.weapon.NormalSword;
import org.savilusGame.entity.weapon.PickAxe;
import org.savilusGame.enums.GameObject;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Creates fresh items and world objects for save-game restore and inventory re-creation.
 * <p>Lookups accept both the localized name stored by current save files and the raw
 * {@link GameObject} enum constant (legacy doors were stored under {@code DOOR}).
 * Unknown names resolve to {@code null} so load paths can skip stale entries instead of crashing.
 */
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GameEntityFactory {

  GamePanel gamePanel;
  Map<GameObject, Supplier<Item>> itemMap = new EnumMap<>(GameObject.class);
  Map<GameObject, Supplier<InteractiveObject>> objectMap = new EnumMap<>(GameObject.class);

  public GameEntityFactory(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    initMaps();
  }

  /** Fresh item instance for the given stored name, or {@code null} when unknown. */
  public Item getItem(String itemName) {
    GameObject type = resolve(itemName);
    if (type == null) {
      return null;
    }
    Supplier<Item> supplier = itemMap.get(type);
    return supplier != null ? supplier.get() : null;
  }

  /** Fresh world object (chest/door/iron door) for the given stored name, or {@code null} when unknown. */
  public InteractiveObject getWorldObject(String objectName) {
    GameObject type = resolve(objectName);
    if (type == null) {
      return null;
    }
    Supplier<InteractiveObject> supplier = objectMap.get(type);
    return supplier != null ? supplier.get() : null;
  }

  /** Whether the stored name resolves to an item (used to migrate legacy drops into ItemDrops). */
  public boolean isItem(String storedName) {
    GameObject type = resolve(storedName);
    return type != null && itemMap.containsKey(type);
  }

  private GameObject resolve(String storedName) {
    if (storedName == null) {
      return null;
    }
    try {
      return GameObject.getObjectFromName(storedName);
    } catch (IllegalArgumentException ignored) {
      // Fall through: legacy saves stored raw enum constants (e.g. doors under "DOOR").
    }
    for (GameObject type : GameObject.values()) {
      if (type.name().equalsIgnoreCase(storedName)) {
        return type;
      }
    }
    return null;
  }

  private void initMaps() {
    itemMap.put(AXE, () -> new Axe(gamePanel));
    itemMap.put(NORMAL_SWORD, () -> new NormalSword(gamePanel));
    itemMap.put(BLUE_SHIELD, () -> new BlueShield(gamePanel));
    itemMap.put(WOOD_SHIELD, () -> new WoodShield(gamePanel));
    itemMap.put(BOOTS, () -> new Boots(gamePanel));
    itemMap.put(KEY, () -> new Key(gamePanel));
    itemMap.put(LANTERN, () -> new Lantern(gamePanel));
    itemMap.put(TENT, () -> new Tent(gamePanel));
    itemMap.put(RED_POTION, () -> new RedPotion(gamePanel));
    itemMap.put(MANA_CRYSTAL, () -> new ManaCrystal(gamePanel));
    itemMap.put(BRONZE_COIN, () -> new BronzeCoin(gamePanel));
    itemMap.put(HEART, () -> new Heart(gamePanel));
    itemMap.put(PICKAXE, () -> new PickAxe(gamePanel));
    itemMap.put(BLUE_HEART, () -> new BlueHeart(gamePanel));

    objectMap.put(CHEST, () -> new Chest(gamePanel));
    objectMap.put(DOOR, () -> new Door(gamePanel));
    objectMap.put(IRON_DOOR, () -> new IronDoor(gamePanel));
  }
}
