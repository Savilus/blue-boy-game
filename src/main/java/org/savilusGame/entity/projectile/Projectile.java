package org.savilusGame.entity.projectile;

import static org.savilusGame.utils.CollisionDetector.INIT_INDEX;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.Direction;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public abstract class Projectile extends Actor {

  private final int SPRITE_CHANGE_INTERVAL = 12;
  private Actor owner;
  private int useCost;

  protected Projectile(GamePanel gamePanel) {
    super(gamePanel);
  }

  public abstract void substractResource(Actor user);

  public abstract boolean haveResource(Actor user);

  public void set(int worldX, int worldY, Direction direction, boolean alive, Actor owner) {
    this.worldX = worldX;
    this.worldY = worldY;
    this.direction = direction;
    this.alive = alive;
    this.owner = owner;
    this.currentLife = this.maxLife;
  }

  @Override
  public void update() {

    if (owner == gamePanel.getPlayer()) {
      int monsterIndex = gamePanel.getCollisionDetector().checkEntity(this, gamePanel.getWorld().currentMonsters());
      if (monsterIndex != INIT_INDEX) {
        var monsters = gamePanel.getWorld().currentMonsters();
        if (monsters == null || monsterIndex < 0 || monsterIndex >= monsters.size() || monsters.get(monsterIndex) == null) {
          log.warn("Ignoring invalid monster collision index {}", monsterIndex);
          return;
        }
        var struck = monsters.get(monsterIndex);
        gamePanel.getPlayer().damageMonster(this, monsterIndex, Math.max(1, attack * (gamePanel.getPlayer().getLevel() / 2)), knockBackPower);
        generateParticle(owner.getProjectile(), struck.getWorldX(), struck.getWorldY());
        alive = false;
      }
    } else {
      boolean contactPlayer = gamePanel.getCollisionDetector().checkPlayer(this);
      if (!gamePanel.getPlayer().isInvincible() && contactPlayer) {
        damagePlayer(attackValue);
        generateParticle(owner.getProjectile(), owner.getProjectile().getWorldX(), owner.getProjectile().getWorldY());
        alive = false;
      }
    }
    switch (getDirection()) {
      case UP -> worldY -= speed;
      case DOWN -> worldY += speed;
      case LEFT -> worldX -= speed;
      case RIGHT -> worldX += speed;
    }

    currentLife--;
    if (currentLife <= 0) {
      alive = false;
    }

    spriteCounter++;
    if (spriteCounter > SPRITE_CHANGE_INTERVAL) {
      spriteNum = (spriteNum == 1) ? 2 : 1;
      spriteCounter = 0;
    }
  }
}
