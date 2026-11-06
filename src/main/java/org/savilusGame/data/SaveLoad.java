package org.savilusGame.data;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.entity.items.Chest;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SaveLoad {

  static String SAVE_LOAD_PATH_FILE = "save.dat";
  static String NA = "NA";

  GamePanel gamePanel;

  public void save() {
    Try.run(() -> {
      ObjectOutputStream outputStream =
          new ObjectOutputStream(new FileOutputStream(SAVE_LOAD_PATH_FILE));
      DataStorage dataStorage = new DataStorage();

      // Interactive tiles
      for (String mapName : gamePanel.getTileManager().getGameMaps().keySet()) {
        var interactiveTiles = gamePanel.getWorld().interactiveTiles(mapName);
        if (gamePanel.getWorld().mapsInteractiveTiles().containsKey(mapName) && Objects.nonNull(interactiveTiles)) {
          List<Integer> currentLifeList = new ArrayList<>();
          List<Boolean> invincibleList = new ArrayList<>();
          for (InteractiveTile interactiveTile : interactiveTiles) {
            if (Objects.nonNull(interactiveTile)) {
              currentLifeList.add(interactiveTile.getCurrentLife());
              invincibleList.add(interactiveTile.isInvincible());
            } else {
              currentLifeList.add(0);
              invincibleList.add(false);
            }
          }
          dataStorage.mapInteractiveTilesCurrentLife.put(mapName, currentLifeList);
          dataStorage.mapInteractiveTilesInvincible.put(mapName, invincibleList);
        }
      }

      // Player stats
      dataStorage.setLevel(gamePanel.getPlayer().getLevel());
      dataStorage.setMaxLife(gamePanel.getPlayer().getMaxLife());
      dataStorage.setLife(gamePanel.getPlayer().getCurrentLife());
      dataStorage.setMana(gamePanel.getPlayer().getMana());
      dataStorage.setMaxMana(gamePanel.getPlayer().getMaxMana());
      dataStorage.setStrength(gamePanel.getPlayer().getStrength());
      dataStorage.setDexterity(gamePanel.getPlayer().getDexterity());
      dataStorage.setExp(gamePanel.getPlayer().getExp());
      dataStorage.setNextLevelExp(gamePanel.getPlayer().getNextLevelExp());
      dataStorage.setMoney(gamePanel.getPlayer().getMoney());

      // Player inventory
      for (int invetoryIndex = 0; invetoryIndex < gamePanel.getPlayer().getInventory().size(); invetoryIndex++) {
        dataStorage.getItemNames().add(gamePanel.getPlayer().getInventory().get(invetoryIndex).getName());
        dataStorage.getItemAmounts().add(gamePanel.getPlayer().getInventory().get(invetoryIndex).getStackCount());
      }

      // Player equipment
      dataStorage.setCurrentWeaponSlot(gamePanel.getPlayer().getCurrentWeaponSlot());
      dataStorage.setCurrentShieldSlot(gamePanel.getPlayer().getCurrentShieldSlot());


      for (var mapName : gamePanel.getTileManager().getGameMaps().keySet()) {
        var mapObjectList = gamePanel.getWorld().objects(mapName);
        if (Objects.isNull(mapObjectList)) {
          continue;
        }

        List<String> objectNamesList = new ArrayList<>();
        List<Integer> worldXList = new ArrayList<>();
        List<Integer> worldYList = new ArrayList<>();
        List<String> lootNamesList = new ArrayList<>();
        List<Boolean> openedList = new ArrayList<>();

        for (var mapObject : mapObjectList) {
          if (Objects.isNull(mapObject)) {
            objectNamesList.add(NA);
            worldXList.add(0);
            worldYList.add(0);
            lootNamesList.add(null);
            openedList.add(false);
          } else {
            objectNamesList.add(mapObject.getName());
            worldXList.add(mapObject.getWorldX());
            worldYList.add(mapObject.getWorldY());
            if (Objects.nonNull(mapObject.getLoot())) {
              lootNamesList.add(mapObject.getLoot().getName());
            } else {
              lootNamesList.add(null);
            }
            openedList.add(mapObject.isOpened());
          }
        }

        dataStorage.getMapObjectNames().put(mapName, objectNamesList);
        dataStorage.getMapObjectWorldX().put(mapName, worldXList);
        dataStorage.getMapObjectWorldY().put(mapName, worldYList);
        dataStorage.getMapObjectLootNames().put(mapName, lootNamesList);
        dataStorage.getMapObjectOpened().put(mapName, openedList);
      }

      // Item drops (loot on the ground) - runtime-spawned only; seeded drops re-seed at boot.
      for (var mapName : gamePanel.getTileManager().getGameMaps().keySet()) {
        var mapItemList = gamePanel.getWorld().items(mapName);
        if (Objects.isNull(mapItemList)) {
          continue;
        }

        List<String> dropNamesList = new ArrayList<>();
        List<Integer> dropWorldXList = new ArrayList<>();
        List<Integer> dropWorldYList = new ArrayList<>();

        for (var itemDrop : mapItemList) {
          if (Objects.isNull(itemDrop) || !itemDrop.isTemporaryObject()) {
            continue;
          }
          dropNamesList.add(itemDrop.getItem().getName());
          dropWorldXList.add(itemDrop.getWorldX());
          dropWorldYList.add(itemDrop.getWorldY());
        }

        if (!dropNamesList.isEmpty()) {
          dataStorage.getMapItemDropNames().put(mapName, dropNamesList);
          dataStorage.getMapItemDropWorldX().put(mapName, dropWorldXList);
          dataStorage.getMapItemDropWorldY().put(mapName, dropWorldYList);
        }
      }

      // Write the DataStorage object
      outputStream.writeObject(dataStorage);

    }).onFailure(error -> log.error("Error saving data", error));
  }

  public void load() {
    Try.run(() -> {
      ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(SAVE_LOAD_PATH_FILE));
      DataStorage dataStorage = (DataStorage) inputStream.readObject();

      // Interactive tiles
      for (String mapName : gamePanel.getTileManager().getGameMaps().keySet()) {
        if (dataStorage.mapInteractiveTilesCurrentLife.containsKey(mapName) && Objects.nonNull(dataStorage.mapInteractiveTilesCurrentLife.get(mapName))) {
          List<Integer> currentLifeList = dataStorage.mapInteractiveTilesCurrentLife.get(mapName);
          List<Boolean> invincibleList = dataStorage.mapInteractiveTilesInvincible.get(mapName);
          var interactiveTiles = gamePanel.getWorld().interactiveTiles(mapName);

          if (Objects.nonNull(currentLifeList) && Objects.nonNull(invincibleList) &&
              currentLifeList.size() == invincibleList.size()) {
            for (int index = 0; index < currentLifeList.size(); index++) {
              if (gamePanel.getWorld().mapsInteractiveTiles().containsKey(mapName) && interactiveTiles.size() > index && Objects.nonNull(interactiveTiles.get(index))) {
                interactiveTiles.get(index).setCurrentLife(currentLifeList.get(index));
                interactiveTiles.get(index).setInvincible(invincibleList.get(index));
                if (interactiveTiles.get(index).getCurrentLife() <= 0 && Objects.nonNull(interactiveTiles.get(index).getDestroyedForm())) {
                  gamePanel.getWorld().interactiveTiles(mapName).set(index, gamePanel.getWorld().interactiveTiles(mapName).get(index).getDestroyedForm());
                }
              }
            }
          }
        }
      }

      // Player stats
      gamePanel.getPlayer().setDefaultPositions();
      gamePanel.getPlayer().setLevel(dataStorage.getLevel());
      gamePanel.getPlayer().setMaxLife(dataStorage.getMaxLife());
      gamePanel.getPlayer().setCurrentLife(dataStorage.getLife());
      gamePanel.getPlayer().setMana(dataStorage.getMana());
      gamePanel.getPlayer().setMaxMana(dataStorage.getMaxMana());
      gamePanel.getPlayer().setStrength(dataStorage.getStrength());
      gamePanel.getPlayer().setDexterity(dataStorage.getDexterity());
      gamePanel.getPlayer().setExp(dataStorage.getExp());
      gamePanel.getPlayer().setNextLevelExp(dataStorage.getNextLevelExp());
      gamePanel.getPlayer().setMoney(dataStorage.getMoney());

      // Player equipment
      gamePanel.getPlayer().getInventory().clear();
      for (int i = 0; i < dataStorage.getItemNames().size(); i++) {
        var item = gamePanel.getGameEntityFactory().getItem(dataStorage.getItemNames().get(i));
        if (Objects.isNull(item)) {
          log.warn("Skipping unknown inventory item from save: {}", dataStorage.getItemNames().get(i));
          continue;
        }
        var amounts = dataStorage.getItemAmounts();
        if (i < amounts.size()) {
          item.setStackCount(amounts.get(i));
        }
        gamePanel.getPlayer().getInventory().add(item);
      }

      var inventory = gamePanel.getPlayer().getInventory();
      int weaponSlot = dataStorage.getCurrentWeaponSlot();
      int shieldSlot = dataStorage.getCurrentShieldSlot();
      if (weaponSlot >= 0 && weaponSlot < inventory.size()) {
        gamePanel.getPlayer().setCurrentWeapon(inventory.get(weaponSlot));
      }
      if (shieldSlot >= 0 && shieldSlot < inventory.size()) {
        gamePanel.getPlayer().setCurrentShield(inventory.get(shieldSlot));
      }
      gamePanel.getPlayer().getAttack();
      gamePanel.getPlayer().getDefense();
      gamePanel.getPlayer().getAttackImage();

      // OBJECTS ON MAP
      for (String mapName : gamePanel.getTileManager().getGameMaps().keySet()) {
        List<String> objectNames = dataStorage.getMapObjectNames().get(mapName);
        List<String> lootNames = dataStorage.getMapObjectLootNames().get(mapName);
        List<Integer> worldXList = dataStorage.getMapObjectWorldX().get(mapName);
        List<Integer> worldYList = dataStorage.getMapObjectWorldY().get(mapName);
        List<Boolean> openedList = dataStorage.getMapObjectOpened().get(mapName);

        if (Objects.isNull(objectNames)) continue;

        List<InteractiveObject> objectsOnMap = gamePanel.getWorld().objects(mapName);
        if (Objects.isNull(objectsOnMap)) {
          continue;
        }
        objectsOnMap.clear();

        for (int name = 0; name < objectNames.size(); name++) {
          String storedName = objectNames.get(name);
          if (StringUtils.equals(storedName, NA)) {
            continue;
          }
          int worldX = worldXList.get(name);
          int worldY = worldYList.get(name);

          InteractiveObject entity = gamePanel.getGameEntityFactory().getWorldObject(storedName);
          if (Objects.isNull(entity)) {
            // Legacy: pre-split saves stored dropped loot (coins, potions, ...) as map objects.
            var droppedItem = gamePanel.getGameEntityFactory().getItem(storedName);
            if (Objects.nonNull(droppedItem)) {
              var itemDrop = new ItemDrop(gamePanel, droppedItem).setWorldPosition(worldX, worldY);
              itemDrop.setTemporaryObject(true);
              gamePanel.getWorld().seedItemDrop(mapName, itemDrop);
            } else {
              log.warn("Skipping unknown map object from save: {}", storedName);
            }
            continue;
          }
          entity.setWorldX(worldX);
          entity.setWorldY(worldY);

          if (entity instanceof Chest chest && Objects.nonNull(lootNames.get(name))) {
            var loot = gamePanel.getGameEntityFactory().getItem(lootNames.get(name));
            if (Objects.nonNull(loot)) {
              chest.setLoot(loot);
            }
          }

          entity.setOpened(openedList.get(name));
          if (entity.isOpened() && Objects.nonNull(entity.getMainImage2())) {
            entity.setMainImage(entity.getMainImage2());
          }
          objectsOnMap.add(entity);
        }

      }

      // Rebuild dropped items from the additive save bucket (old saves: section absent -> no drops).
      gamePanel.getWorld().clearTemporaryItems();
      var dropMaps = dataStorage.getMapItemDropNames();
      if (Objects.nonNull(dropMaps)) {
        for (var mapName : dropMaps.keySet()) {
          List<String> dropNames = dataStorage.getMapItemDropNames().get(mapName);
          List<Integer> dropX = dataStorage.getMapItemDropWorldX().get(mapName);
          List<Integer> dropY = dataStorage.getMapItemDropWorldY().get(mapName);
          if (Objects.isNull(dropNames) || Objects.isNull(dropX) || Objects.isNull(dropY)) {
            continue;
          }
          for (int i = 0; i < dropNames.size(); i++) {
            var item = gamePanel.getGameEntityFactory().getItem(dropNames.get(i));
            if (Objects.isNull(item)) {
              log.warn("Skipping unknown item drop from save: {}", dropNames.get(i));
              continue;
            }
            var itemDrop = new ItemDrop(gamePanel, item).setWorldPosition(dropX.get(i), dropY.get(i));
            itemDrop.setTemporaryObject(true);
            gamePanel.getWorld().seedItemDrop(mapName, itemDrop);
          }
        }
      }
    }).onFailure(error -> log.error("Error loading data", error));
  }
}
