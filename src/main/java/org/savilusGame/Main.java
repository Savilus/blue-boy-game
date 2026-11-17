package org.savilusGame;

import javax.swing.*;

public class Main {

  public static final String GAME_TITLE = "Blue Boy";

  public static JFrame window;

  static void main() {
    window = new JFrame();
    window.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    window.setResizable(false);
    window.setTitle(GAME_TITLE);

    GamePanel gamePanel = new GamePanel();
    window.add(gamePanel);
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