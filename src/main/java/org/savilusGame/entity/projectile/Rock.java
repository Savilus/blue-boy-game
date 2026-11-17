package org.savilusGame.entity.projectile;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.ROCK_ATTACK;

import java.awt.*;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.text.TextManager;

public class Rock extends Projectile {

  public Rock(GamePanel gamePanel) {
    super(gamePanel);
    name = TextManager.getItemName(GameObject.ROCK.getTextKey());
    speed = 7;
    maxLife = 50;
    currentLife = maxLife;
    attack = 3;
    alive = false;
    setUseCost(1);
    getImage();
  }

  private void getImage() {
    up1 = down1 = left1 = right1 = sprites.get(ROCK_ATTACK, TILE_SIZE, TILE_SIZE);
    up2 = down2 = left2 = right2 = sprites.get(ROCK_ATTACK, TILE_SIZE, TILE_SIZE);
  }

  @Override
  public Color getParticleColor() {
    return new Color(40, 50, 0);
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
    user.setAmmo(user.getAmmo() - getUseCost());
  }

  @Override
  public boolean haveResource(Actor user) {
    return user.getAmmo() >= getUseCost();
  }

}
