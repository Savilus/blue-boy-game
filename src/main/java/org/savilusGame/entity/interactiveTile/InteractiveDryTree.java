package org.savilusGame.entity.interactiveTile;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DESTROY_TREE;
import static org.savilusGame.config.GameEntityNameFactory.DRY_TREE;
import static org.savilusGame.enums.WorldGameTypes.AXE;

import java.awt.*;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.utils.SpriteLoader;

public class InteractiveDryTree extends InteractiveTile {

  public InteractiveDryTree(GamePanel gamePanel, int col, int row) {
    super(gamePanel);
    currentLife = 2;
    this.worldX = TILE_SIZE * col;
    this.worldY = TILE_SIZE * row;

    mainImage = SpriteLoader.load(DRY_TREE, TILE_SIZE, TILE_SIZE);
    setDestructible(true);
  }

  @Override
  public Color getParticleColor() {
    return new Color(65, 50, 30);
  }

  @Override
  public int getParticleSize() {
    return 6;  // 6 pixels
  }

  @Override
  public int getParticleSpeed() {
    return 1;
  }

  @Override
  public int getParticleMaxLife() {
    return 20;
  }

  @Override
  public void playSoundEffect() {
    gamePanel.playSE(DESTROY_TREE);
  }

  @Override
  public InteractiveTile getDestroyedForm() {
    return new Trunk(gamePanel, worldX / TILE_SIZE, worldY / TILE_SIZE);
  }

  @Override
  public boolean isCorrectItem(Actor actor) {
    return actor.getCurrentWeapon().getType() == AXE;
  }
}
