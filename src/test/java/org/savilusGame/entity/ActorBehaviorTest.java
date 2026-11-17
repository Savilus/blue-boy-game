package org.savilusGame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.items.BlueHeart;
import org.savilusGame.enums.Direction;
import org.savilusGame.enums.GameObject;
import org.savilusGame.utils.CollisionDetector;

class ActorBehaviorTest {

  private static final class TestGamePanel extends GamePanel {
    @Override
    public void playSE(String soundId) {
    }
  }

  private static final class TestActor extends Actor {
    TestActor(GamePanel gamePanel) {
      super(gamePanel);
    }

    void attackNow() {
      attacking();
    }
  }

  private static final class RecordingCollisionDetector extends CollisionDetector {
    private int worldXAtPlayerCheck;

    RecordingCollisionDetector(GamePanel gamePanel) {
      super(gamePanel);
    }

    @Override
    public boolean checkPlayer(CollisionBody entity) {
      worldXAtPlayerCheck = entity.getWorldX();
      return false;
    }
  }

  private TestGamePanel panel;

  @BeforeEach
  void setUp() {
    panel = new TestGamePanel();
  }

  @Test
  @DisplayName("rightward attack offset uses the attack area width")
  void rightwardAttackUsesAttackAreaWidth() {
    RecordingCollisionDetector detector = new RecordingCollisionDetector(panel);
    panel.setCollisionDetector(detector);

    TestActor actor = new TestActor(panel);
    actor.setMonster(true);
    actor.setWorldPosition(100, 100);
    actor.setDirection(Direction.RIGHT);
    actor.setAttackArea(new Rectangle(0, 0, 31, 7));
    actor.setFirstAttackMotionDuration(0);
    actor.setSecondAttackMotionDuration(2);

    actor.attackNow();

    assertEquals(131, detector.worldXAtPlayerCheck);
    assertEquals(100, actor.getWorldX(), "Attack resolution must restore the actor position");
  }

  @Test
  @DisplayName("entity center is derived from its solid area without requiring sprites")
  void centerUsesSolidArea() {
    TestActor actor = new TestActor(panel);
    actor.setWorldPosition(300, 400);
    actor.setSolidArea(new Rectangle(7, 11, 30, 12));

    assertEquals(322, actor.getCenterX());
    assertEquals(417, actor.getCenterY());
  }

  @Test
  @DisplayName("monster contact applies damage once through the shared damage pipeline")
  void monsterContactUsesSharedDamagePipeline() {
    Player player = panel.getPlayer();
    player.setCurrentLife(6);
    player.setDefense(2);
    player.setInvincible(false);

    TestActor monster = new TestActor(panel);
    monster.setAttack(5);
    monster.setDirection(Direction.LEFT);
    panel.getWorld().mapsMonsters().put(
        panel.getWorld().getCurrentMap(),
        new ArrayList<>(List.of(monster))
    );

    player.detectMonsterContact(0);

    assertEquals(3, player.getCurrentLife());
    assertTrue(player.isInvincible());
    assertTrue(player.isTransparent());
  }

  @Test
  @DisplayName("monster contact can be parried through the shared damage pipeline")
  void monsterContactCanBeParried() {
    Player player = panel.getPlayer();
    player.setCurrentLife(6);
    player.setInvincible(false);
    player.setGuarding(true);
    player.setDirection(Direction.RIGHT);
    player.setGuardCounter(0);

    TestActor monster = new TestActor(panel);
    monster.setAttack(5);
    monster.setDirection(Direction.LEFT);
    panel.getWorld().mapsMonsters().put(
        panel.getWorld().getCurrentMap(),
        new ArrayList<>(List.of(monster))
    );

    player.detectMonsterContact(0);

    assertEquals(6, player.getCurrentLife());
    assertTrue(player.isInvincible());
    assertTrue(monster.isOffBalance());
  }

  @Test
  void staleCollisionIndicesDoNotDamagePlayerOrThrow() {
    Player player = panel.getPlayer();
    int life = player.getCurrentLife();

    player.detectMonsterContact(0);
    player.detectMonsterContact(-2);
    player.damageMonster(player, 0, 5, 0);
    player.damageProjectile(0);
    player.damageInteractiveTile(0);

    assertEquals(life, player.getCurrentLife());
  }

  @Test
  @DisplayName("factory resolves the blue heart as a blue heart")
  void factoryCreatesBlueHeart() {
      assertInstanceOf(BlueHeart.class, panel.getGameEntityFactory().getItem(GameObject.BLUE_HEART.name()));
  }

  @Test
  void obtainingItemUsesStableIdentityRatherThanDisplayName() {
    BlueHeart heart = new BlueHeart(panel);
    heart.setName("translated heart");

    assertTrue(panel.getPlayer().canObtainItem(heart));
    assertTrue(panel.getPlayer().getInventory().stream()
        .anyMatch(item -> item.getId().equals(GameObject.BLUE_HEART.name())));
  }
}
