package org.savilusGame.service;

import java.awt.Graphics2D;

/** A world sprite participating in the stable back-to-front draw order. */
public interface DepthDrawable {
  int getWorldY();
  void draw(Graphics2D graphics);
}
