package org.savilusGame.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_FIRST_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.items.Chest;
import org.savilusGame.entity.items.IronDoor;
import org.savilusGame.entity.monster.SkeletonLord;
import org.savilusGame.enums.GameObject;

class SaveLoadTest {
  @TempDir Path directory;
  GamePanel panel;
  SaveLoad saveLoad;
  Path json;
  Path legacy;

  @BeforeEach
  void setUp() {
    panel = new GamePanel();
    panel.getAssetSetter().setObject();
    panel.getAssetSetter().setInteractiveTiles();
    panel.getAssetSetter().setMonster();
    panel.getAssetSetter().setNPC();
    panel.getAssetSetter().setProjectile();
    panel.getAssetSetter().setInteractiveObjects();
    json = directory.resolve("save.json");
    legacy = directory.resolve("save.dat");
    saveLoad = new SaveLoad(panel, json, legacy);
  }

  @Test
  void roundTripRestoresEveryMapAndWorldFlags() {
    var world = panel.getWorld();
    int mainObjects = world.objects(MAIN_MAP).size();
    int mainDrops = world.items(MAIN_MAP).size();
    world.objects(MAIN_MAP).removeFirst();
    world.items(MAIN_MAP).removeFirst();
    Chest chest = (Chest) world.objects(DUNGEON_FIRST_FLOR).getFirst();
    chest.setOpened(true);
    String chestLoot = chest.getLoot().getId();
    world.interactiveTiles(MAIN_MAP).set(0, null);
    world.setSkeletonLordDefeated(true);
    boolean[][] events = panel.getEventHandler().completedEvents(DUNGEON_SECOND_FLOR);
    events[25][27] = true;
    panel.getEventHandler().restoreCompletedEvents(DUNGEON_SECOND_FLOR, events);
    world.switchMap(DUNGEON_SECOND_FLOR);
    panel.getPlayer().setWorldPosition(22 * TILE_SIZE, 30 * TILE_SIZE);

    assertTrue(saveLoad.save());
    world.objects(MAIN_MAP).clear();
    world.items(MAIN_MAP).clear();
    world.setSkeletonLordDefeated(false);
    events[25][27] = false;
    panel.getEventHandler().restoreCompletedEvents(DUNGEON_SECOND_FLOR, events);
    panel.getPlayer().setWorldPosition(TILE_SIZE, TILE_SIZE);
    world.switchMap(MAIN_MAP);

    assertTrue(saveLoad.load());
    assertEquals(mainObjects - 1, world.objects(MAIN_MAP).size());
    assertEquals(mainDrops - 1, world.items(MAIN_MAP).size());
    Chest restoredChest = (Chest) world.objects(DUNGEON_FIRST_FLOR).getFirst();
    assertTrue(restoredChest.isOpened());
    assertEquals(chestLoot, restoredChest.getLoot().getId());
    assertNull(world.interactiveTiles(MAIN_MAP).getFirst());
    assertEquals(DUNGEON_SECOND_FLOR, world.getCurrentMap());
    assertEquals(22 * TILE_SIZE, panel.getPlayer().getWorldX());
    assertEquals(30 * TILE_SIZE, panel.getPlayer().getWorldY());
    assertTrue(world.isSkeletonLordDefeated());
    assertTrue(world.monsters(DUNGEON_SECOND_FLOR).stream().noneMatch(SkeletonLord.class::isInstance));
    assertTrue(panel.getEventHandler().completedEvents(DUNGEON_SECOND_FLOR)[25][27]);
    assertFalse(panel.getEventHandler().completedEvents(MAIN_MAP)[25][27]);
  }

  @Test
  void corruptSaveDoesNotChangeCurrentState() throws Exception {
    int life = panel.getPlayer().getCurrentLife();
    int objects = panel.getWorld().objects(MAIN_MAP).size();
    Files.writeString(json, "{\"schemaVersion\":1}");

    assertFalse(saveLoad.load());
    assertEquals(life, panel.getPlayer().getCurrentLife());
    assertEquals(objects, panel.getWorld().objects(MAIN_MAP).size());
  }

  @Test
  void failedSaveReportsFailureWithoutReplacingExistingData() throws Exception {
    Path blockingFile = directory.resolve("not-a-directory");
    Files.writeString(blockingFile, "preserve");
    SaveLoad blocked = new SaveLoad(panel, blockingFile.resolve("save.json"), legacy);

    assertFalse(blocked.save());
    assertEquals("preserve", Files.readString(blockingFile));
    assertFalse(panel.getUi().getMessages().isEmpty());
  }

  @Test
  void invalidWorldStateDoesNotReplacePreviousSave() throws Exception {
    assertTrue(saveLoad.save());
    String previousSave = Files.readString(json);
    panel.getWorld().currentObjects().add(new InteractiveObject(panel) {});

    assertFalse(saveLoad.save());
    assertEquals(previousSave, Files.readString(json));
  }

  @Test
  void unknownInventoryItemDoesNotShiftSavedEquipmentSlots() throws Exception {
    assertTrue(saveLoad.save());
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = (ObjectNode) mapper.readTree(json.toFile());
    ((ObjectNode) root.path("player").path("inventory").get(0)).put("id", "UNKNOWN_ITEM");
    mapper.writeValue(json.toFile(), root);

    assertTrue(saveLoad.load());
    assertEquals(GameObject.NORMAL_SWORD.name(), panel.getPlayer().getCurrentWeapon().getId());
    assertEquals(GameObject.WOOD_SHIELD.name(), panel.getPlayer().getCurrentShield().getId());
    assertTrue(panel.getPlayer().getInventory().contains(panel.getPlayer().getCurrentWeapon()));
    assertTrue(panel.getPlayer().getInventory().contains(panel.getPlayer().getCurrentShield()));
  }

  @Test
  void missingMapKeepsItsCurrentState() throws Exception {
    assertTrue(saveLoad.save());
    int objects = panel.getWorld().objects(DUNGEON_FIRST_FLOR).size();
    ObjectMapper mapper = new ObjectMapper();
    ObjectNode root = (ObjectNode) mapper.readTree(json.toFile());
    ((ObjectNode) root.path("maps")).remove(DUNGEON_FIRST_FLOR);
    mapper.writeValue(json.toFile(), root);

    assertTrue(saveLoad.load());
    assertEquals(objects, panel.getWorld().objects(DUNGEON_FIRST_FLOR).size());
  }

  @Test
  void temporaryBossDoorIsNotRestoredAsPermanent() {
    var objects = panel.getWorld().objects(DUNGEON_SECOND_FLOR);
    int originalCount = objects.size();
    var temporaryDoor = new IronDoor(panel);
    temporaryDoor.setWorldPosition(25 * TILE_SIZE, 28 * TILE_SIZE);
    temporaryDoor.setTemporaryObject(true);
    objects.add(temporaryDoor);
    panel.getWorld().setBossBattleOn(true);

    assertTrue(saveLoad.save());
    assertTrue(saveLoad.load());
    assertFalse(panel.getWorld().isBossBattleOn());
    assertEquals(originalCount, panel.getWorld().objects(DUNGEON_SECOND_FLOR).size());
    assertTrue(panel.getWorld().monsters(DUNGEON_SECOND_FLOR).stream().anyMatch(SkeletonLord.class::isInstance));
  }

  @Test
  void importsLegacySaveAndWritesVersionedJson() throws Exception {
    DataStorage old = new DataStorage();
    var player = panel.getPlayer();
    old.setLevel(player.getLevel());
    old.setMaxLife(player.getMaxLife());
    old.setLife(player.getCurrentLife());
    old.setMaxMana(player.getMaxMana());
    old.setMana(player.getMana());
    old.setStrength(player.getStrength());
    old.setDexterity(player.getDexterity());
    old.setExp(player.getExp());
    old.setNextLevelExp(player.getNextLevelExp());
    old.setMoney(player.getMoney());
    old.getItemNames().add(GameObject.NORMAL_SWORD.getName());
    old.getItemNames().add(GameObject.WOOD_SHIELD.getName());
    old.getItemAmounts().add(1);
    old.getItemAmounts().add(1);
    old.setCurrentWeaponSlot(0);
    old.setCurrentShieldSlot(1);
    try (ObjectOutputStream stream = new ObjectOutputStream(Files.newOutputStream(legacy))) {
      stream.writeObject(old);
    }

    assertTrue(saveLoad.load());
    assertTrue(Files.exists(legacy), "Migration must not delete the original save");
    assertTrue(Files.exists(json));
    assertEquals(GameObject.NORMAL_SWORD.name(), panel.getPlayer().getCurrentWeapon().getId());
    assertInstanceOf(ObjectNode.class, new ObjectMapper().readTree(Files.readString(json)));
  }
}
