package org.savilusGame.service;

import java.awt.Graphics2D;

/**
 * Read-only rendering context passed to drawable entities.
 * <p>Carries the target graphics and the camera offset (world-to-screen translation).
 * Entity draw methods must only read state; they must not mutate game state.
 */
public interface RenderContext {

  /** The graphics target for this frame. */
  Graphics2D graphics();

  /** Horizontal camera offset in pixels (screenX = worldX - cameraX). */
  int cameraX();

  /** Vertical camera offset in pixels (screenY = worldY - cameraY). */
  int cameraY();
}
