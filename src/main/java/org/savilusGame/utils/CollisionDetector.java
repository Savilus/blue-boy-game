package org.savilusGame.utils;

import static org.savilusGame.GamePanel.TILE_SIZE;


import java.awt.*;
import java.util.List;
import java.util.Objects;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.CollisionBody;
import org.savilusGame.entity.Player;
import org.savilusGame.entity.item.ItemDrop;
import org.savilusGame.enums.Direction;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CollisionDetector {
  public final static int INIT_INDEX = 999;

  private final GamePanel gamePanel;

  public void checkTile(CollisionBody gameEntity) {
    int entityLeftWorldX = getXSolidArea(gameEntity);
    int entityRightWorldX = getXSolidArea(gameEntity) + gameEntity.getSolidArea().width;
    int entityTopWorldY = getYSolidArea(gameEntity);
    int entityBottomWorldY = getYSolidArea(gameEntity) + gameEntity.getSolidArea().height;

    int entityLeftCol = entityLeftWorldX / TILE_SIZE;
    int entityRightCol = entityRightWorldX / TILE_SIZE;
    int entityTopRow = entityTopWorldY / TILE_SIZE;
    int entityBottomRow = entityBottomWorldY / TILE_SIZE;

    int tileNum1, tileNum2;

    // Use a temporal direction when it's being knock backed
    Direction direction = gameEntity.getDirection();
    if (gameEntity.isKnockBack()) {
      direction = gameEntity.getKnockBackDirection();
    }
    var currentMapTiles = gamePanel.getTileManager().getGameMaps().get(gamePanel.getWorld().getCurrentMap());
    switch (direction) {
      case UP -> {
        entityTopRow = (entityTopWorldY - gameEntity.getSpeed()) / TILE_SIZE;
        tileNum1 = currentMapTiles[entityLeftCol][entityTopRow];
        tileNum2 = currentMapTiles[entityRightCol][entityTopRow];
        if (gamePanel.getTileManager().getTile()[tileNum1].collision() || gamePanel.getTileManager().getTile()[tileNum2].collision()) {
          gameEntity.setCollisionOn(true);
        }
      }
      case DOWN -> {
        entityBottomRow = (entityBottomWorldY + gameEntity.getSpeed()) / TILE_SIZE;
        tileNum1 = currentMapTiles[entityLeftCol][entityBottomRow];
        tileNum2 = currentMapTiles[entityRightCol][entityBottomRow];
        if (gamePanel.getTileManager().getTile()[tileNum1].collision() || gamePanel.getTileManager().getTile()[tileNum2].collision()) {
          gameEntity.setCollisionOn(true);
        }
      }
      case LEFT -> {
        entityLeftCol = (entityLeftWorldX - gameEntity.getSpeed()) / TILE_SIZE;
        tileNum1 = currentMapTiles[entityLeftCol][entityTopRow];
        tileNum2 = currentMapTiles[entityLeftCol][entityBottomRow];
        if (gamePanel.getTileManager().getTile()[tileNum1].collision() || gamePanel.getTileManager().getTile()[tileNum2].collision()) {
          gameEntity.setCollisionOn(true);
        }
      }
      case RIGHT -> {
        entityRightCol = (entityRightWorldX + gameEntity.getSpeed()) / TILE_SIZE;
        tileNum1 = currentMapTiles[entityRightCol][entityTopRow];
        tileNum2 = currentMapTiles[entityRightCol][entityBottomRow];
        if (gamePanel.getTileManager().getTile()[tileNum1].collision() || gamePanel.getTileManager().getTile()[tileNum2].collision()) {
          gameEntity.setCollisionOn(true);
        }
      }
    }
  }

  public int checkObject(CollisionBody gameEntity, boolean player) {
    if (Objects.isNull(gamePanel.getWorld().currentObjects()))
      return INIT_INDEX;

    int index = INIT_INDEX;

    Direction direction = gameEntity.getDirection();
    if (gameEntity.isKnockBack()) {
      direction = gameEntity.getKnockBackDirection();
    }

    var objects = gamePanel.getWorld().currentObjects();

    for (int i = 0; i < objects.size(); i++) {
      var mapObject = objects.get(i);
      if (Objects.nonNull(mapObject)) {
        getSolidArea(gameEntity).x = getXSolidArea(gameEntity);
        getSolidArea(gameEntity).y = getYSolidArea(gameEntity);

        mapObject.getSolidArea().x = mapObject.getWorldX() + mapObject.getSolidArea().x;
        mapObject.getSolidArea().y = mapObject.getWorldY() + mapObject.getSolidArea().y;

        checkGameEntityCollision(gameEntity, direction);
        if (getSolidArea(gameEntity).intersects(mapObject.getSolidArea())) {
          if (mapObject.isCollision()) {
            gameEntity.setCollisionOn(true);
          }
          if (player) {
            index = i;
          }
        }
        getSolidArea(gameEntity).x = gameEntity.getSolidAreaDefaultX();
        getSolidArea(gameEntity).y = gameEntity.getSolidAreaDefaultY();
        mapObject.getSolidArea().x = mapObject.getSolidAreaDefaultX();
        mapObject.getSolidArea().y = mapObject.getSolidAreaDefaultY();
      }
    }
    return index;
  }

  // NPC OR MONSTER COLLISION
  public int checkEntity(CollisionBody gameEntity, List<? extends CollisionBody> target) {
    if (Objects.isNull(target) || target.isEmpty())
      return INIT_INDEX;
    int index = INIT_INDEX;

    // Use a temporal direction when it's being knock backed
    Direction direction = gameEntity.getDirection();
    if (gameEntity.isKnockBack()) {
      direction = gameEntity.getKnockBackDirection();
    }

    for (int targetIndex = 0; targetIndex < target.size(); targetIndex++) {
      var currentTarget = target.get(targetIndex);
      if (Objects.nonNull(currentTarget)) {
        //get entity's solid area position
        getSolidArea(gameEntity).x = getXSolidArea(gameEntity);
        getSolidArea(gameEntity).y = getYSolidArea(gameEntity);
        // get target solid area position
        currentTarget.getSolidArea().x = currentTarget.getWorldX() + currentTarget.getSolidArea().x;
        currentTarget.getSolidArea().y = currentTarget.getWorldY() + currentTarget.getSolidArea().y;
        checkGameEntityCollision(gameEntity, direction);
        if (getSolidArea(gameEntity).intersects(currentTarget.getSolidArea()) && currentTarget != gameEntity) {
          gameEntity.setCollisionOn(true);
          index = targetIndex;
        }

        getSolidArea(gameEntity).x = gameEntity.getSolidAreaDefaultX();
        getSolidArea(gameEntity).y = gameEntity.getSolidAreaDefaultY();
        currentTarget.getSolidArea().x = currentTarget.getSolidAreaDefaultX();
        currentTarget.getSolidArea().y = currentTarget.getSolidAreaDefaultY();
      }
    }

    return index;
  }

  public boolean checkPlayer(CollisionBody gameEntity) {
    boolean contactPlayer = false;
    //get entity's solid area position
    getSolidArea(gameEntity).x = getXSolidArea(gameEntity);
    getSolidArea(gameEntity).y = getYSolidArea(gameEntity);
    // get player solid area position
    getPlayer().getSolidArea().x = getPlayer().getWorldX() + getPlayer().getSolidArea().x;
    getPlayer().getSolidArea().y = getPlayer().getWorldY() + getPlayer().getSolidArea().y;

    checkGameEntityCollision(gameEntity, gameEntity.getDirection());
    if (getSolidArea(gameEntity).intersects(getPlayer().getSolidArea())) {
      gameEntity.setCollisionOn(true);
      contactPlayer = true;
    }
    getSolidArea(gameEntity).x = gameEntity.getSolidAreaDefaultX();
    getSolidArea(gameEntity).y = gameEntity.getSolidAreaDefaultY();
    getPlayer().getSolidArea().x = getPlayer().getSolidAreaDefaultX();
    getPlayer().getSolidArea().y = getPlayer().getSolidAreaDefaultY();

    return contactPlayer;
  }


  /**
   * Overlap check against the dropped items of the current map. Items are never solid,
   * so the movement collision flag is left untouched; only the first hit index is returned.
   */
  public int checkItems(CollisionBody gameEntity, List<ItemDrop> items) {
    if (Objects.isNull(items) || items.isEmpty()) {
      return INIT_INDEX;
    }
    int index = INIT_INDEX;
    Direction direction = gameEntity.getDirection();
    if (gameEntity.isKnockBack()) {
      direction = gameEntity.getKnockBackDirection();
    }

    for (int itemIndex = 0; itemIndex < items.size(); itemIndex++) {
      var itemDrop = items.get(itemIndex);
      if (Objects.nonNull(itemDrop)) {
        getSolidArea(gameEntity).x = getXSolidArea(gameEntity);
        getSolidArea(gameEntity).y = getYSolidArea(gameEntity);

        itemDrop.getSolidArea().x = itemDrop.getWorldX() + itemDrop.getSolidArea().x;
        itemDrop.getSolidArea().y = itemDrop.getWorldY() + itemDrop.getSolidArea().y;

        checkGameEntityCollision(gameEntity, direction);
        if (getSolidArea(gameEntity).intersects(itemDrop.getSolidArea())) {
          index = itemIndex;
        }

        getSolidArea(gameEntity).x = gameEntity.getSolidAreaDefaultX();
        getSolidArea(gameEntity).y = gameEntity.getSolidAreaDefaultY();
        itemDrop.getSolidArea().x = itemDrop.getSolidAreaDefaultX();
        itemDrop.getSolidArea().y = itemDrop.getSolidAreaDefaultY();
      }
    }
    return index;
  }

  private Player getPlayer() {
    return gamePanel.getPlayer();
  }

  private Rectangle getSolidArea(CollisionBody gameEntity) {
    return gameEntity.getSolidArea();
  }

  private int getYSolidArea(CollisionBody gameEntity){
   return gameEntity.getWorldY() + gameEntity.getSolidArea().y;
  }

  private int getXSolidArea(CollisionBody gameEntity){
    return gameEntity.getWorldX() + gameEntity.getSolidArea().x;
  }

  private void checkGameEntityCollision(CollisionBody gameEntity, Direction direction) {
    switch (direction) {
      case UP -> getSolidArea(gameEntity).y -= gameEntity.getSpeed();
      case DOWN -> getSolidArea(gameEntity).y += gameEntity.getSpeed();
      case LEFT -> getSolidArea(gameEntity).x -= gameEntity.getSpeed();
      case RIGHT -> getSolidArea(gameEntity).x += gameEntity.getSpeed();
    }
  }
}
