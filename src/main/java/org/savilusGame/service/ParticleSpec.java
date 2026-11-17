package org.savilusGame.service;

import java.awt.Color;

/** Visual spec of the particles bursting off an actor or object when struck. */
public interface ParticleSpec {

  Color getParticleColor();

  int getParticleSize();

  int getParticleSpeed();

  int getParticleMaxLife();
}
