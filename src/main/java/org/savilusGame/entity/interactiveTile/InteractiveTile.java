package org.savilusGame.entity.interactiveTile;

import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.service.Destructible;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InteractiveTile extends InteractiveObject implements Destructible {

  private boolean destructible = false;

  public InteractiveTile(GamePanel gamePanel) {
    super(gamePanel);
    invincibleTime = 20;
  }

  @Override
  public boolean isDestructible() {
    return destructible;
  }

  public boolean isCorrectItem(Actor actor) {
    return false;
  }

  public void playSoundEffect() {
  }

  public InteractiveTile getDestroyedForm() {
    return null;
  }


}
