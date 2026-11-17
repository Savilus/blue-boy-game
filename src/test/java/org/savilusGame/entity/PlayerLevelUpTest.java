package org.savilusGame.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.savilusGame.enums.GameState.DIALOG_STATE;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;

/**
 * Level-up arithmetic at the smallest seam that reproduces it: {@link Player#gainExperience(int)}.
 * Audio is stubbed out so the test stays headless and deterministic.
 */
class PlayerLevelUpTest {

  private static final class SilentGamePanel extends GamePanel {
    @Override
    public void playSE(String soundId) {
    }
  }

  private SilentGamePanel panel;
  private Player player;

  @BeforeEach
  void setUp() {
    panel = new SilentGamePanel();
    player = panel.getPlayer();
    player.setLevel(1);
    player.setExp(0);
    player.setNextLevelExp(5);
    player.setMaxLife(6);
    player.setStrength(0);
    player.setDexterity(1);
  }

  @Test
  @DisplayName("experience below the threshold only accumulates")
  void experienceBelowThresholdDoesNotLevelUp() {
    player.gainExperience(4);

    assertEquals(1, player.getLevel());
    assertEquals(4, player.getExp());
    assertEquals(5, player.getNextLevelExp());
    assertEquals(6, player.getMaxLife());
  }

  @Test
  @DisplayName("overflow experience is carried into the next level instead of being discarded")
  void overflowExperienceIsKept() {
    player.gainExperience(12);

    assertEquals(2, player.getLevel());
    assertEquals(7, player.getExp(), "12 exp minus the 5 exp first level");
    assertEquals(10, player.getNextLevelExp(), "the curve itself is unchanged");
    assertEquals(8, player.getMaxLife());
    assertEquals(1, player.getStrength());
    assertEquals(2, player.getDexterity());
  }

  @Test
  @DisplayName("a single reward can grant several levels")
  void oneRewardCanGrantSeveralLevels() {
    player.gainExperience(15);

    assertEquals(3, player.getLevel(), "5 exp + 10 exp thresholds");
    assertEquals(0, player.getExp());
    assertEquals(15, player.getNextLevelExp());
    assertEquals(10, player.getMaxLife());
    assertEquals(2, player.getStrength());
    assertEquals(3, player.getDexterity());
  }

  @Test
  void nonPositiveRewardsChangeNothing() {
    player.gainExperience(0);
    player.gainExperience(-5);

    assertEquals(1, player.getLevel());
    assertEquals(0, player.getExp());
  }

  @Test
  @DisplayName("gaining a level opens the level-up dialogue once")
  void levelUpOpensTheDialogue() {
    player.gainExperience(15);

    assertEquals(DIALOG_STATE, panel.getGameState());
    assertEquals(player, panel.getUi().getDialogueObject());
  }
}
