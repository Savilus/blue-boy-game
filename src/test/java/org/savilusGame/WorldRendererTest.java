package org.savilusGame;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.interactiveTile.InteractiveTile;

class WorldRendererTest {
  @Test
  void reusesTheQueueWithoutKeepingRemovedSpritesOrChangingEqualDepthOrder() {
    var panel = new GamePanel();
    var world = panel.getWorld();
    var drawn = new ArrayList<String>();
    class Sprite extends Actor {
      private final String label;
      Sprite(String label, int depth) { super(panel); this.label = label; worldY = depth; }
      @Override public void draw(Graphics2D graphics) { drawn.add(label); }
    }
    var player = new Sprite("player", 2);
    var npc = new Sprite("npc", 1);
    var object = new InteractiveObject(panel) {
      @Override public void draw(Graphics2D graphics) { drawn.add("object"); }
    };
    object.setWorldY(2);
    var tile = new InteractiveTile(panel) {
      @Override public void draw(Graphics2D graphics) { drawn.add("tile"); }
    };
    world.setInteractiveTiles(java.util.Map.of(world.getCurrentMap(), List.of(tile)));
    world.currentObjects().add(object);
    world.currentNpc().add(null);
    world.currentNpc().add(npc);
    world.currentMonsters().add(new Sprite("monster", 2));
    var renderer = new WorldRenderer();
    var graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    try {
      renderer.draw(graphics, world, player);
      assertEquals(List.of("tile", "npc", "player", "object", "monster"), drawn);
      world.currentObjects().clear();
      npc.setWorldY(3);
      drawn.clear();
      renderer.draw(graphics, world, player);
      assertEquals(List.of("tile", "player", "monster", "npc"), drawn);
    } finally {
      graphics.dispose();
    }
  }
}
