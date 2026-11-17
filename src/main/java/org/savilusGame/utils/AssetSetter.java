package org.savilusGame.utils;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_FIRST_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.INTERIOR_MAP;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.interactiveObjects.BigRock;
import org.savilusGame.entity.item.Item;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.entity.interactiveTile.DestructibleWall;
import org.savilusGame.entity.interactiveTile.InteractiveDryTree;
import org.savilusGame.entity.interactiveTile.InteractiveTile;
import org.savilusGame.entity.interactiveTile.MetalPlate;
import org.savilusGame.entity.items.BlueHeart;
import org.savilusGame.entity.items.BronzeCoin;
import org.savilusGame.entity.items.Chest;
import org.savilusGame.entity.items.Door;
import org.savilusGame.entity.items.IronDoor;
import org.savilusGame.entity.items.Key;
import org.savilusGame.entity.items.Lantern;
import org.savilusGame.entity.items.ManaCrystal;
import org.savilusGame.entity.items.RedPotion;
import org.savilusGame.entity.monster.Bat;
import org.savilusGame.entity.monster.GreenSlime;
import org.savilusGame.entity.monster.Orc;
import org.savilusGame.entity.monster.RedSlime;
import org.savilusGame.entity.monster.SkeletonLord;
import org.savilusGame.entity.npc.MerchantNPC;
import org.savilusGame.entity.npc.OldManNPC;
import org.savilusGame.entity.shield.BlueShield;
import org.savilusGame.entity.weapon.Axe;
import org.savilusGame.entity.weapon.PickAxe;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;

@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AssetSetter {

  GamePanel gamePanel;

  public void setObject() {
    Map<String, List<InteractiveObject>> mapsObjects = new HashMap<>();
    Map<String, List<ItemDrop>> mapsItems = new HashMap<>();

    mapsObjects.put(MAIN_MAP, new ArrayList<>(List.of(
        new Door(gamePanel).setWorldPosition(TILE_SIZE * 14, TILE_SIZE * 28),
        new Door(gamePanel).setWorldPosition(TILE_SIZE * 12, TILE_SIZE * 12),
        new Chest(gamePanel).setLoot(new Lantern(gamePanel)).setWorldPosition(TILE_SIZE * 19, TILE_SIZE * 41),
        new Chest(gamePanel).setLoot(new Key(gamePanel)).setWorldPosition(TILE_SIZE * 33, TILE_SIZE * 7),
        new Chest(gamePanel).setLoot(new Axe(gamePanel)).setWorldPosition(TILE_SIZE * 37, TILE_SIZE * 43)
    )));
    mapsItems.put(MAIN_MAP, new ArrayList<>(List.of(
        drop(new BronzeCoin(gamePanel), 25, 23),
        drop(new BronzeCoin(gamePanel), 21, 19),
        drop(new BlueShield(gamePanel), 39, 19),
        drop(new RedPotion(gamePanel), 13, 25),
        drop(new RedPotion(gamePanel), 23, 7),
        drop(new ManaCrystal(gamePanel), 31, 42),
        drop(new ManaCrystal(gamePanel), 26, 36),
        drop(new RedPotion(gamePanel), 38, 29)
    )));

    mapsObjects.put(DUNGEON_FIRST_FLOR, new ArrayList<>(List.of(
        new Chest(gamePanel).setLoot(new PickAxe(gamePanel)).setWorldPosition(TILE_SIZE * 40, TILE_SIZE * 41),
        new Chest(gamePanel).setLoot(new RedPotion(gamePanel)).setWorldPosition(TILE_SIZE * 13, TILE_SIZE * 16),
        new Chest(gamePanel).setLoot(new RedPotion(gamePanel)).setWorldPosition(TILE_SIZE * 26, TILE_SIZE * 34),
        new Chest(gamePanel).setLoot(new RedPotion(gamePanel)).setWorldPosition(TILE_SIZE * 27, TILE_SIZE * 15),
        new IronDoor(gamePanel).setWorldPosition(TILE_SIZE * 18, TILE_SIZE * 23)
    )));
    mapsItems.put(DUNGEON_FIRST_FLOR, new ArrayList<>());

    mapsObjects.put(DUNGEON_SECOND_FLOR, new ArrayList<>(List.of(
        new IronDoor(gamePanel).setWorldPosition(TILE_SIZE * 25, TILE_SIZE * 15)
    )));
    mapsItems.put(DUNGEON_SECOND_FLOR, new ArrayList<>(List.of(
        drop(new BlueHeart(gamePanel), 25, 8)
    )));

    mapsObjects.put(INTERIOR_MAP, new ArrayList<>());
    mapsItems.put(INTERIOR_MAP, new ArrayList<>());

    gamePanel.getWorld().setObjects(mapsObjects);
    gamePanel.getWorld().setItems(mapsItems);
  }

  private ItemDrop drop(Item item, int col, int row) {
    return new ItemDrop(gamePanel, item).setWorldPosition(TILE_SIZE * col, TILE_SIZE * row);
  }


  public void setProjectile() {
    gamePanel.getWorld().setProjectiles(Map.of(
        MAIN_MAP, new ArrayList<>(),
        INTERIOR_MAP, new ArrayList<>(),
        DUNGEON_FIRST_FLOR, new ArrayList<>(),
        DUNGEON_SECOND_FLOR, new ArrayList<>()
    ));
  }

  public void setNPC() {
    gamePanel.getWorld().setNpcs(new HashMap<>());
    gamePanel.getWorld().mapsNpc().put(MAIN_MAP, new ArrayList<>(List.of(
        new OldManNPC(gamePanel).setWorldPosition(TILE_SIZE * 23, TILE_SIZE * 13)
    )));
    gamePanel.getWorld().mapsNpc().put(INTERIOR_MAP, new ArrayList<>(List.of(
        new MerchantNPC(gamePanel).setWorldPosition(TILE_SIZE * 12, TILE_SIZE * 7)
    )));
    gamePanel.getWorld().mapsNpc().put(DUNGEON_SECOND_FLOR, new ArrayList<>());
  }


  public void setInteractiveObjects() {
    if (Objects.isNull(gamePanel.getWorld().mapsNpc())) {
      gamePanel.getWorld().setNpcs(new HashMap<>());
    }
    gamePanel.getWorld().mapsNpc().put(DUNGEON_FIRST_FLOR, new ArrayList<>(List.of(
        new BigRock(gamePanel).setWorldPosition(TILE_SIZE * 20, TILE_SIZE * 25),
        new BigRock(gamePanel).setWorldPosition(TILE_SIZE * 11, TILE_SIZE * 18),
        new BigRock(gamePanel).setWorldPosition(TILE_SIZE * 23, TILE_SIZE * 15)
    )));
  }

  public void setMonster() {
    gamePanel.getWorld().setMonsters(Map.of(
        MAIN_MAP, new ArrayList<>(List.of(
            new GreenSlime(gamePanel).setWorldPosition(TILE_SIZE * 21, TILE_SIZE * 38),
            new GreenSlime(gamePanel).setWorldPosition(TILE_SIZE * 23, TILE_SIZE * 42),
            new GreenSlime(gamePanel).setWorldPosition(TILE_SIZE * 24, TILE_SIZE * 37),
            new GreenSlime(gamePanel).setWorldPosition(TILE_SIZE * 34, TILE_SIZE * 42),
            new GreenSlime(gamePanel).setWorldPosition(TILE_SIZE * 38, TILE_SIZE * 42),
            new GreenSlime(gamePanel).setWorldPosition(TILE_SIZE * 33, TILE_SIZE * 38),
            new RedSlime(gamePanel).setWorldPosition(TILE_SIZE * 34, TILE_SIZE * 11),
            new RedSlime(gamePanel).setWorldPosition(TILE_SIZE * 37, TILE_SIZE * 7),
            new RedSlime(gamePanel).setWorldPosition(TILE_SIZE * 40, TILE_SIZE * 11),
            new RedSlime(gamePanel).setWorldPosition(TILE_SIZE * 36, TILE_SIZE * 22),
            new RedSlime(gamePanel).setWorldPosition(TILE_SIZE * 36, TILE_SIZE * 17),
            new Orc(gamePanel).setWorldPosition(TILE_SIZE * 12, TILE_SIZE * 31)
        )),
        DUNGEON_FIRST_FLOR, new ArrayList<>(List.of(
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 34, TILE_SIZE * 39),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 38, TILE_SIZE * 24),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 37, TILE_SIZE * 25),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 28, TILE_SIZE * 11),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 10, TILE_SIZE * 20),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 17, TILE_SIZE * 37),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 15, TILE_SIZE * 26),
            new Bat(gamePanel).setWorldPosition(TILE_SIZE * 34, TILE_SIZE * 40)
        )),
        DUNGEON_SECOND_FLOR, new ArrayList<>(List.of(
            new SkeletonLord(gamePanel).setWorldPosition(TILE_SIZE * 23, TILE_SIZE * 16)
        ))
    ));
  }

  public void setInteractiveTiles() {
    Map<String, List<InteractiveTile>> interactiveTiles = new HashMap<>();

    interactiveTiles.put(
        MAIN_MAP, new ArrayList<>(List.of(
            new InteractiveDryTree(gamePanel, 27, 12),
            new InteractiveDryTree(gamePanel, 28, 12),
            new InteractiveDryTree(gamePanel, 29, 12),
            new InteractiveDryTree(gamePanel, 30, 12),
            new InteractiveDryTree(gamePanel, 31, 12),
            new InteractiveDryTree(gamePanel, 32, 12),
            new InteractiveDryTree(gamePanel, 33, 12),
            new InteractiveDryTree(gamePanel, 18, 40),
            new InteractiveDryTree(gamePanel, 17, 40),
            new InteractiveDryTree(gamePanel, 16, 40),
            new InteractiveDryTree(gamePanel, 35, 29),
            new InteractiveDryTree(gamePanel, 36, 29),
            new InteractiveDryTree(gamePanel, 36, 31),
            new InteractiveDryTree(gamePanel, 37, 31),
            new InteractiveDryTree(gamePanel, 30, 21),
            new InteractiveDryTree(gamePanel, 31, 21),
            new InteractiveDryTree(gamePanel, 32, 21),
            new InteractiveDryTree(gamePanel, 36, 30),
            new InteractiveDryTree(gamePanel, 15, 40),
            new InteractiveDryTree(gamePanel, 14, 40),
            new InteractiveDryTree(gamePanel, 13, 40)
        ))
    );

    interactiveTiles.put(DUNGEON_FIRST_FLOR, new ArrayList<>(List.of(
            new DestructibleWall(gamePanel, 18, 30),
            new DestructibleWall(gamePanel, 17, 31),
            new DestructibleWall(gamePanel, 17, 32),
            new DestructibleWall(gamePanel, 17, 34),
            new DestructibleWall(gamePanel, 18, 34),
            new DestructibleWall(gamePanel, 18, 33),
            new DestructibleWall(gamePanel, 10, 22),
            new DestructibleWall(gamePanel, 10, 24),
            new DestructibleWall(gamePanel, 38, 18),
            new DestructibleWall(gamePanel, 38, 19),
            new DestructibleWall(gamePanel, 38, 20),
            new DestructibleWall(gamePanel, 38, 21),
            new DestructibleWall(gamePanel, 18, 13),
            new DestructibleWall(gamePanel, 18, 14),
            new DestructibleWall(gamePanel, 22, 28),
            new DestructibleWall(gamePanel, 30, 28),
            new DestructibleWall(gamePanel, 32, 28),
            new MetalPlate(gamePanel, 20, 22),
            new MetalPlate(gamePanel, 8, 17),
            new MetalPlate(gamePanel, 39, 31)
        ))
    );
    interactiveTiles.put(INTERIOR_MAP, new ArrayList<>());
    gamePanel.getWorld().setInteractiveTiles(interactiveTiles);
  }
}
