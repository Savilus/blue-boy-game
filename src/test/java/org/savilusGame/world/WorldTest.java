package org.savilusGame.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.entity.items.RedPotion;
import org.savilusGame.enums.Direction;

/**
 * Headless tests for the extracted {@link World} (Phase 1, extended for the Phase 2 entity split).
 */
class WorldTest {

  /** Minimal concrete actor for map bookkeeping (all hooks have safe defaults). */
  static class TestActor extends Actor {
    TestActor(GamePanel gamePanel) {
      super(gamePanel);
      this.direction = Direction.DOWN;
      this.speed = 4;
      this.defaultSpeed = 4;
    }
  }

  /** Minimal concrete world object for map bookkeeping. */
  static class TestObject extends InteractiveObject {
    TestObject(GamePanel gamePanel) {
      super(gamePanel);
    }
  }

  GamePanel panel;
  World world;

  @BeforeEach
  void setUp() {
    panel = new GamePanel();
    world = new World(panel.getTileManager(), "MAP_A");
  }

  @Test
  @DisplayName("current map accessors follow switchMap()")
  void currentMapAccessorsFollowSwitchMap() {
    TestObject objectA = new TestObject(panel);
    TestObject objectB = new TestObject(panel);
    TestActor monsterA = new TestActor(panel);

    Map<String, List<InteractiveObject>> objects = new HashMap<>();
    objects.put("MAP_A", new ArrayList<>(List.of(objectA)));
    objects.put("MAP_B", new ArrayList<>(List.of(objectB)));
    world.setObjects(objects);
    world.spawnMonster("MAP_A", monsterA);

    assertEquals("MAP_A", world.getCurrentMap());
    assertEquals(List.of(objectA), world.currentObjects());
    assertEquals(List.of(monsterA), world.currentMonsters());
    assertNull(world.currentNpc()); // never seeded -> null, like the legacy map access
    assertNull(world.currentItems());

    world.switchMap("MAP_B");
    assertEquals("MAP_B", world.getCurrentMap());
    assertEquals(List.of(objectB), world.currentObjects());
    assertNull(world.currentMonsters()); // monsters only exist on MAP_A

    world.switchMap("MAP_A");
    assertEquals(List.of(objectA), world.currentObjects());
  }

  @Test
  @DisplayName("entitiesOn aggregates objects, items, NPCs and monsters of a map")
  void entitiesOnAggregatesAllCollisionBodyKinds() {
    TestObject object = new TestObject(panel);
    TestActor npc = new TestActor(panel);
    TestActor monster = new TestActor(panel);
    ItemDrop itemDrop = new ItemDrop(panel, new RedPotion(panel)).setWorldPosition(10, 20);

    world.setObjects(Map.of("MAP_A", new ArrayList<>(List.of(object))));
    world.setNpcs(Map.of("MAP_A", new ArrayList<>(List.of(npc))));
    world.spawnMonster("MAP_A", monster);
    world.seedItemDrop("MAP_A", itemDrop);

    List<CollisionBody> all = world.entitiesOn("MAP_A");
    assertEquals(4, all.size());
    assertTrue(all.contains(object));
    assertTrue(all.contains(npc));
    assertTrue(all.contains(monster));
    assertTrue(all.contains(itemDrop));
  }

  @Test
  @DisplayName("dropItem spawns a temporary item drop on the current map")
  void dropItemSpawnsTemporaryDropOnCurrentMap() {
    world.dropItem(new RedPotion(panel), 123, 456);

    List<ItemDrop> items = world.currentItems();
    assertNotNull(items);
    assertEquals(1, items.size());
    assertEquals(123, items.get(0).getWorldX());
    assertEquals(456, items.get(0).getWorldY());
    assertTrue(items.get(0).isTemporaryObject());
  }

  @Test
  @DisplayName("seeded item drops are not temporary and survive removeTemporaryObjects")
  void seededDropsSurviveCleanup() {
    ItemDrop seeded = new ItemDrop(panel, new RedPotion(panel)).setWorldPosition(1, 2);
    world.seedItemDrop("MAP_A", seeded);

    world.removeTemporaryObjects();

    assertEquals(List.of(seeded), world.currentItems());
  }

  @Test
  @DisplayName("clearTemporaryItems removes only runtime drops, seeded drops stay")
  void clearTemporaryItemsKeepsSeeded() {
    ItemDrop seeded = new ItemDrop(panel, new RedPotion(panel)).setWorldPosition(1, 2);
    world.seedItemDrop("MAP_A", seeded);
    world.dropItem(new RedPotion(panel), 3, 4);

    world.clearTemporaryItems();

    assertEquals(List.of(seeded), world.currentItems());
  }

  @Test
  @DisplayName("removeEntity removes the actor from every actor list of every map")
  void removeEntityScansAllMaps() {
    TestActor shared = new TestActor(panel);
    world.setNpcs(Map.of("MAP_A", new ArrayList<>(List.of(shared))));
    world.setMonsters(Map.of("MAP_B", new ArrayList<>(List.of(shared))));

    assertTrue(world.removeEntity(shared));
    assertEquals(0, world.npcs("MAP_A").size());
    assertEquals(0, world.monsters("MAP_B").size());
    assertFalse(world.removeEntity(shared)); // already gone everywhere
  }

  @Test
  @DisplayName("removeObject removes the object from every object list of every map")
  void removeObjectScansAllMaps() {
    TestObject shared = new TestObject(panel);
    world.setObjects(Map.of(
        "MAP_A", new ArrayList<>(List.of(shared)),
        "MAP_B", new ArrayList<>(List.of(shared))));

    assertTrue(world.removeObject(shared));
    assertEquals(0, world.objects("MAP_A").size());
    assertEquals(0, world.objects("MAP_B").size());
    assertFalse(world.removeObject(shared));
  }

  @Test
  @DisplayName("removeTemporaryObjects drops only temporary objects, actors and items from all maps")
  void removeTemporaryObjectsDropsOnlyTemporaryItems() {
    TestObject temporaryObject = new TestObject(panel);
    temporaryObject.setTemporaryObject(true);
    TestObject permanentObject = new TestObject(panel);
    world.setObjects(Map.of(
        "MAP_A", new ArrayList<>(List.of(temporaryObject, permanentObject))));

    TestActor temporaryActor = new TestActor(panel);
    temporaryActor.setTemporaryObject(true);
    TestActor permanentActor = new TestActor(panel);
    world.setNpcs(Map.of("MAP_A", new ArrayList<>(List.of(temporaryActor, permanentActor))));

    world.dropItem(new RedPotion(panel), 5, 6);

    world.removeTemporaryObjects();

    assertEquals(List.of(permanentObject), world.objects("MAP_A"));
    assertEquals(List.of(permanentActor), world.npcs("MAP_A"));
    assertTrue(world.currentItems() == null || world.currentItems().isEmpty());
  }

  @Test
  @DisplayName("boss/event flags persist across map switches")
  void bossFlagsPersistAcrossMapSwitch() {
    assertFalse(world.isBossBattleOn());
    assertFalse(world.isSkeletonLordDefeated());

    world.setBossBattleOn(true);
    world.setSkeletonLordDefeated(true);

    world.switchMap("MAP_B");
    assertTrue(world.isBossBattleOn());
    assertTrue(world.isSkeletonLordDefeated());

    world.switchMap("MAP_A");
    assertTrue(world.isBossBattleOn());
    assertTrue(world.isSkeletonLordDefeated());
  }
}
