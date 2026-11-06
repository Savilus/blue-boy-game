package org.savilusGame.entity;

import static org.savilusGame.GamePanel.TILE_SIZE;

import java.awt.*;

import org.savilusGame.GamePanel;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Particle extends Actor {

  final Color color;
  final int size;
  final int xd;
  int yd;

  public Particle(GamePanel gamePanel, int worldX, int worldY, Color color, int size, int speed, int maxLife, int xd, int yd) {
    super(gamePanel);
    this.color = color;
    this.size = size;
    this.speed = speed;
    this.maxLife = maxLife;
    this.xd = xd;
    this.yd = yd;

    currentLife = maxLife;
    int offset = (TILE_SIZE / 2) - (size / 2);
    this.worldX = worldX + offset;
    this.worldY = worldY + offset;
  }

  @Override
  public void update() {
    currentLife--;

    if (currentLife < maxLife / 3) {
      yd++;
    }
    worldX += xd * speed;
    worldY += yd * speed;
    if (currentLife == 0)
      alive = false;

  }

  @Override
  public void draw(Graphics2D graphics2D) {
    int screenX = worldX - gamePanel.getPlayer().worldX + gamePanel.getPlayer().getScreenX();
    int screenY = worldY - gamePanel.getPlayer().worldY + gamePanel.getPlayer().getScreenY();

    graphics2D.setColor(color);
    graphics2D.fillRect(screenX, screenY, size, size);
  }
}
