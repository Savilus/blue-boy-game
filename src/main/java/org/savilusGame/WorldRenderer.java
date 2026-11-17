package org.savilusGame;

import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.savilusGame.service.DepthDrawable;
import org.savilusGame.world.World;

/** Game-loop-owned scratch list. Equal depths retain insertion order across sprite families. */
final class WorldRenderer {
  private static final Comparator<DepthDrawable> BY_DEPTH = Comparator.comparingInt(DepthDrawable::getWorldY);

  private final ArrayList<DepthDrawable> sprites = new ArrayList<>();

  void draw(Graphics2D graphics, World world, DepthDrawable player) {
    for (var tile : world.currentInteractiveTiles()) {
      if (tile != null) tile.draw(graphics);
    }
    sprites.clear();
    sprites.add(player);
    addSprites(world.currentNpc());
    addSprites(world.currentObjects());
    addSprites(world.currentMonsters());
    addSprites(world.currentProjectiles());
    addSprites(world.particleList());
    addSprites(world.currentItems());
    sprites.sort(BY_DEPTH);
    for (int index = 0; index < sprites.size(); index++) sprites.get(index).draw(graphics);
  }

  private void addSprites(List<? extends DepthDrawable> source) {
    if (source == null) return;
    for (int index = 0; index < source.size(); index++) {
      var sprite = source.get(index);
      if (sprite != null) sprites.add(sprite);
    }
  }
}
