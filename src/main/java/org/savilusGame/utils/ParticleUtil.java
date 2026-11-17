package org.savilusGame.utils;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Particle;
import org.savilusGame.service.ParticleSpec;

import lombok.experimental.UtilityClass;

/** Spawns hit-burst particle effects. */
@UtilityClass
public class ParticleUtil {

  private static final int[][] DIRECTIONS = {
      {-2, -1}, {2, -1}, {-2, 1}, {2, 1}
  };

  /**
   * Four diagonal particles around {@code worldX},{@code worldY} using the supplied sprite spec.
   * Particle positions derive from the passed coordinates (the struck/target position).
   */
  public static void burst(GamePanel gamePanel, int worldX, int worldY, ParticleSpec spec) {
    for (int[] direction : DIRECTIONS) {
      var particle = new Particle(gamePanel, worldX, worldY, spec.getParticleColor(), spec.getParticleSize(),
          spec.getParticleSpeed(), spec.getParticleMaxLife(), direction[0], direction[1]);
      gamePanel.getWorld().particleList().add(particle);
    }
  }
}
