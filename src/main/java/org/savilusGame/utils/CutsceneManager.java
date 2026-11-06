package org.savilusGame.utils;

import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.Main.GAME_TITLE;
import static org.savilusGame.config.GameEntityNameFactory.DOOR_OPEN;
import static org.savilusGame.config.GameEntityNameFactory.FANFARE;
import static org.savilusGame.config.GameEntityNameFactory.FINAL_BATTLE;
import static org.savilusGame.config.GameEntityNameFactory.OUTSIDE_MUSIC;
import static org.savilusGame.enums.GameState.PLAY_STATE;

import java.awt.*;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.savilusGame.GamePanel;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.InteractiveObject;
import org.savilusGame.entity.PlayerDummy;
import org.savilusGame.entity.items.BlueHeart;
import org.savilusGame.entity.items.IronDoor;
import org.savilusGame.enums.MonsterType;
import org.savilusGame.utils.text.TextManager;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CutsceneManager {
  static final String CLOSING_CREDITS = "closingCredits";
  static final String CLOSING_CREDITS_FINAL_TEXT = "finalBoyText";
  static final String CUTSCENE_DIALOGUE = "cutsceneDialogue";
  static final String END_CREDITS_TEXT = "endCredits";
  static final int FIVE_SECONDS = 300;

  final GamePanel gamePanel;
  Graphics2D graphics2D;
  int sceneNum;
  int scenePhase;
  int counter = 0;
  float alpha = 0f;
  int y;

  //scene number
  final int NA = 0;
  final int skeletonLord = 1;
  final int ending = 2;

  public CutsceneManager(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
  }

  public void draw(Graphics2D graphics2D) {
    this.graphics2D = graphics2D;

    switch (sceneNum) {
      case skeletonLord -> skeletonLordScene();
      case ending -> endingScene();
    }
  }

  private void skeletonLordScene() {
    var mapNpcs = gamePanel.getWorld().currentNpc();

    if (scenePhase == 0) {
      gamePanel.getWorld().setBossBattleOn(true);

      InteractiveObject ironDoor = new IronDoor(gamePanel);
      ironDoor.setWorldX(TILE_SIZE * 25);
      ironDoor.setWorldY(TILE_SIZE * 28);
      ironDoor.setTemporaryObject(true);
      gamePanel.getWorld().currentObjects().add(ironDoor);
      gamePanel.playSE(DOOR_OPEN);

      PlayerDummy dummy = new PlayerDummy(gamePanel);
      dummy.setWorldX(gamePanel.getPlayer().getWorldX());
      dummy.setWorldY(gamePanel.getPlayer().getWorldY());
      dummy.setDirection(gamePanel.getPlayer().getDirection());

      gamePanel.getWorld().currentNpc().add(dummy);
      gamePanel.getPlayer().setDrawing(false);
      scenePhase++;
    }

    if (scenePhase == 1) {
      gamePanel.getPlayer().setWorldY(gamePanel.getPlayer().getWorldY() - 2);
      if (gamePanel.getPlayer().getWorldY() < TILE_SIZE * 16) {
        scenePhase++;
      }
    }

    if (scenePhase == 2) {
      var monsters = gamePanel.getWorld().currentMonsters();
      for (Actor monster : monsters) {
        if (Objects.nonNull(monster) &&
            StringUtils.equalsIgnoreCase(monster.getName(), MonsterType.SKELETON_LORD.getName())) {
          monster.setSleep(false);
          gamePanel.getUi().setDialogueObject(monster);
          scenePhase++;
          break;
        }
      }
    }

    if (scenePhase == 3) {
      var monsters = gamePanel.getWorld().currentMonsters();
      for (Actor monster : monsters) {
        if (Objects.nonNull(monster) &&
            StringUtils.equalsIgnoreCase(monster.getName(), MonsterType.SKELETON_LORD.getName())) {
          monster.startDialogue(monster, CUTSCENE_DIALOGUE);
          gamePanel.getUi().drawDialogueScreen();
          break;
        }
      }
    }

    if (scenePhase == 4) {
      for (int i = 0; i < mapNpcs.size(); i++) {
        if (Objects.nonNull(mapNpcs.get(i)) && mapNpcs.get(i) instanceof PlayerDummy) {
          gamePanel.getPlayer().setWorldX(mapNpcs.get(i).getWorldX());
          gamePanel.getPlayer().setWorldY(mapNpcs.get(i).getWorldY());
          mapNpcs.remove(i);
          break;
        }
      }

      gamePanel.getPlayer().setDrawing(true);
      sceneNum = NA;
      scenePhase = 0;
      gamePanel.setGameState(PLAY_STATE);
      gamePanel.stopBGM();
      gamePanel.playBGM(FINAL_BATTLE);
    }
  }

  private void endingScene() {
    var blueHeart = new BlueHeart(gamePanel);
    if (scenePhase == 0) {
      gamePanel.stopBGM();
      blueHeart.startDialogue(blueHeart, CUTSCENE_DIALOGUE);
      scenePhase++;
    }
    if (scenePhase == 1) {
      gamePanel.getUi().drawDialogueScreen();
    }
    if (scenePhase == 2) {
      gamePanel.playSE(FANFARE);
      scenePhase++;
    }
    if (scenePhase == 3) {
      if (counterReachedTargetTime(FIVE_SECONDS)) {
        scenePhase++;
      }
    }
    if (scenePhase == 4) {
      alpha += 0.005F;
      if (alpha > 1F) {
        alpha = 1F;
      }
      drawBlackBackground(alpha);
      if (alpha == 1F) {
        scenePhase++;
      }
    }
    if (scenePhase == 5) {
      drawBlackBackground(1F);
      drawString(
          alpha,
          38F,
          200,
          TextManager.getAllDialoguesForTarget(CLOSING_CREDITS).get(CLOSING_CREDITS_FINAL_TEXT).getFirst(),
          70);

      if (counterReachedTargetTime(FIVE_SECONDS * 2)) {
        gamePanel.playBGM(OUTSIDE_MUSIC);
        scenePhase++;
      }
    }
    if (scenePhase == 6) {
      drawBlackBackground(1F);
      y = gamePanel.getScreenHeight() / 2;
      drawString(1f, 120f, y / 2, GAME_TITLE, 40);
      if (counterReachedTargetTime(FIVE_SECONDS)) {
        scenePhase++;
      }
    }
    if (scenePhase == 7) {
      drawBlackBackground(1F);
      drawString(
          1F,
          38F,
          gamePanel.getScreenHeight() / 2,
          TextManager.getAllDialoguesForTarget(CLOSING_CREDITS).get(END_CREDITS_TEXT).getFirst(),
          40);

      if (counterReachedTargetTime(FIVE_SECONDS)) {
        scenePhase++;
      }
    }
    if (scenePhase == 8) {
      drawBlackBackground(1F);
      y--;
      drawString(
          1F,
          38F,
          y,
          TextManager.getAllDialoguesForTarget(CLOSING_CREDITS).get(END_CREDITS_TEXT).getFirst(),
          40);
    }
  }

  private boolean counterReachedTargetTime(int target) {
    if (++counter > target) {
      counter = 0;
      return true;
    }
    return false;
  }

  private void drawBlackBackground(float alpha) {
    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    graphics2D.setColor(Color.BLACK);
    graphics2D.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1F));
  }

  private void drawString(float alpha, float fontSize, int y, String text, int lineHeight) {
    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
    graphics2D.setColor(Color.WHITE);
    graphics2D.setFont(graphics2D.getFont().deriveFont(fontSize));

    for (String line : text.split("\n")) {
      int x = gamePanel.getUi().getXForCenteredText(line);
      graphics2D.drawString(line, x, y);
      y += lineHeight;
    }
    graphics2D.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1F));
  }
}

