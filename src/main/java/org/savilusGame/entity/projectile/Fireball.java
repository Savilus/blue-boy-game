package org.savilusGame.entity.projectile;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_DOWN1;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_DOWN2;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_LEFT1;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_LEFT2;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_RIGHT1;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_RIGHT2;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_UP1;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_UP2;

import java.awt.*;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.GameObject;

public class Fireball extends Projectile {

  public Fireball(GamePanel gamePanel) {
    super(gamePanel);
    name = GameObject.FIREBALL.getName();
    speed = 5;
    maxLife = 80;
    currentLife = maxLife;
    attack = 1;
    alive = false;
    knockBackPower = 5;
    setUseCost(1);
    getImage();
  }

  private void getImage() {
    up1 = SpriteLoader.load(FIREBALL_UP1, TILE_SIZE, TILE_SIZE);
    up2 = SpriteLoader.load(FIREBALL_UP2, TILE_SIZE, TILE_SIZE);
    down1 = SpriteLoader.load(FIREBALL_DOWN1, TILE_SIZE, TILE_SIZE);
    down2 = SpriteLoader.load(FIREBALL_DOWN2, TILE_SIZE, TILE_SIZE);
    left1 = SpriteLoader.load(FIREBALL_LEFT1, TILE_SIZE, TILE_SIZE);
    left2 = SpriteLoader.load(FIREBALL_LEFT2, TILE_SIZE, TILE_SIZE);
    right1 = SpriteLoader.load(FIREBALL_RIGHT1, TILE_SIZE, TILE_SIZE);
    right2 = SpriteLoader.load(FIREBALL_RIGHT2, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public Color getParticleColor() {
    return new Color(240, 50, 0);
  }

  @Override
  public int getParticleSize() {
    return 10;  // 6 pixels
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
  public void substractResource(Actor user) {
    user.setMana(user.getMana() - getUseCost());
  }

  @Override
  public boolean haveResource(Actor user) {
    return user.getMana() >= getUseCost();
  }
}
