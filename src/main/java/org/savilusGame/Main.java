package org.savilusGame;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.*;

public class Main {

  public static final String GAME_TITLE = "Blue Boy";

  public static JFrame window;

  static void main() {
    window = new JFrame();
    // Closing the window goes through the same cleanup path as "Quit" in the title menu.
    window.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    window.setResizable(false);
    window.setTitle(GAME_TITLE);

    GamePanel gamePanel = new GamePanel();
    window.add(gamePanel);
    window.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(WindowEvent event) {
        gamePanel.quitGame();
      }
    });
    gamePanel.getConfig().loadConfig();

    if (gamePanel.isFullScreenOn()) {
      window.setUndecorated(true);
    }

    window.pack();

    window.setLocationRelativeTo(null);
    window.setVisible(true);
    gamePanel.setupGame();
    gamePanel.startGameThread();
  }
}