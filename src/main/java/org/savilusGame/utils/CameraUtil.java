package org.savilusGame.utils;

import org.savilusGame.GamePanel;

import lombok.experimental.UtilityClass;

/** World-to-screen projection helpers (camera follows the player). */
@UtilityClass
public class CameraUtil {

  public static int screenX(GamePanel gamePanel, int worldX) {
    return worldX - gamePanel.getPlayer().getWorldX() + gamePanel.getPlayer().getScreenX();
  }

  public static int screenY(GamePanel gamePanel, int worldY) {
    return worldY - gamePanel.getPlayer().getWorldY() + gamePanel.getPlayer().getScreenY();
  }

  /** Whether a world-space rectangle around the entity is visible on screen. */
  public static boolean isOnScreen(GamePanel gamePanel, int worldX, int worldY) {
    return worldX + org.savilusGame.GamePanel.TILE_SIZE * 5 > gamePanel.getPlayer().getWorldX() - gamePanel.getPlayer().getScreenX()
        && worldX - org.savilusGame.GamePanel.TILE_SIZE < gamePanel.getPlayer().getWorldX() + gamePanel.getPlayer().getScreenX()
        && worldY + org.savilusGame.GamePanel.TILE_SIZE * 5 > gamePanel.getPlayer().getWorldY() - gamePanel.getPlayer().getScreenY()
        && worldY - org.savilusGame.GamePanel.TILE_SIZE < gamePanel.getPlayer().getWorldY() + gamePanel.getPlayer().getScreenY();
  }
}
