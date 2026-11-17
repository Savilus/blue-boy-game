package org.savilusGame;

import static org.savilusGame.config.GameEntityNameFactory.MARU_MONICA_FONT;
import static org.savilusGame.config.GameEntityNameFactory.PURISA_BOLD_FONT;
import static org.savilusGame.config.GameEntityNameFactory.SPEAK;
import static org.savilusGame.enums.DayState.DAY;
import static org.savilusGame.enums.GameState.CUTSCENE_STATE;
import static org.savilusGame.enums.GameState.DIALOG_STATE;
import static org.savilusGame.enums.GameState.PLAY_STATE;
import static org.savilusGame.enums.GameState.TITLE_STATE;
import static org.savilusGame.enums.SubState.CONTROL_OPTIONS;
import static org.savilusGame.enums.SubState.DEFAULT;
import static org.savilusGame.enums.SubState.FULL_SCREEN_OPTION;
import static org.savilusGame.enums.SubState.MAIN_SETTINGS;
import static org.savilusGame.enums.SubState.MENU;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.BRONZE_COIN;
import static org.savilusGame.config.GameEntityNameFactory.HEART_BLANK;
import static org.savilusGame.config.GameEntityNameFactory.HEART_FULL;
import static org.savilusGame.config.GameEntityNameFactory.HEART_HALF;
import static org.savilusGame.config.GameEntityNameFactory.MANA_CRYSTAL_BLANK;
import static org.savilusGame.config.GameEntityNameFactory.MANA_CRYSTAL_FULL;
import static org.savilusGame.enums.SubState.NPC_INVENTORY;
import static org.savilusGame.enums.SubState.PLAYER_INVENTORY;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.savilusGame.entity.Actor;
import org.savilusGame.entity.item.Item;
import org.savilusGame.service.DialogueSpeaker;
import org.savilusGame.utils.SpriteLoader;
import org.savilusGame.enums.SubState;
import org.savilusGame.environment.Lighting;
import org.savilusGame.utils.text.TextManager;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UI {
  static final String MERCHANT_COME_AGAIN_DIALOGUE_KEY = "merchantNpcComeAgain";
  static final String MERCHANT_NO_MONEY_DIALOGUE_KEY = "merchantNpcNotEnoughMoney";
  static final String MERCHANT_TO_MUCH_ITEMS_DIALOGUE_KEY = "merchantNpcTooMuchItems";
  static final String MERCHANT_TRADE_DIALOGUE_KEY = "merchantNpcTradeQuestion";
  static final String MERCHANT_CANNOT_SELL_KEY = "merchantNpcCannotSellItem";
  static final String CURSOR_SELECTOR = ">";
  static final String PLAYER_LIFE_FORMAT = "%s / %s";
  static final String CHARACTER_CONTROL = "WASD";
  static final String CONFIRM = "Enter";
  static final String PROJECTILE_KEY = "F";
  static final String CHARACTER_KEY = "C";
  static final String PAUSE_KEY = "P";
  static final String MAP_KEY = "X/M";
  static final String MAP = "map";
  static final String CLOSE_KEY = "ESC";
  static final String SPACE_KEY = "SPACE";
  static final String DEFAULT_FONT = "SansSerif";
  static final String MENU_OPTIONS = "menuOptions";
  static final String DIALOGUES = "dialogues";
  static final String SETTINGS = "settings";
  static final String UI_MESSAGES = "uiMessages";
  static final String GAME_STATS = "gameStats";
  static final String NEW_GAME = "newGame";
  static final String LOAD_GAME = "loadGame";
  static final String QUIT = "quit";
  static final String END_GAME_QUESTION = "endGameQuestion";
  static final String FULL_SCREEN_NOTIFICATION = "fullScreenNotification";
  static final String PAUSED = "paused";
  static final String COIN = "coin";
  static final String TITLE = "title";
  static final String END_GAME = "endGame";
  static final String LIFE = "life";
  static final String DEXTERITY = "dexterity";
  static final String WEAPON = "weapon";
  static final String LEVEL = "level";
  static final String SHIELD = "shield";
  static final String EXP = "exp";
  static final String STRENGTH = "strength";
  static final String GAME_OVER = "gameOver";
  static final String RETRY = "retry";
  static final String OPTIONS = "options";
  static final String ATTACK = "attack";
  static final String NEXT_LEVEL = "nextLevel";
  static final String DEFENSE = "defense";
  static final String FULL_SCREEN = "fullScreen";
  static final String YES = "yes";
  static final String BACK = "back";
  static final String MUSIC = "music";
  static final String SOUND_EFFECT = "soundEffect";
  static final String NO = "no";
  static final String MOVE = "move";
  static final String SHOOT_CAST = "shootCast";
  static final String PARRY = "parry";
  static final String CONFIRM_ATTACK = "confirmAttack";
  static final String CHARACTER_SCREEN = "characterScreen";
  static final String CONTROL = "control";
  static final String CONTROLS = "controls";
  static final String PAUSE = "pause";
  static final String BUY = "buy";
  static final String SHOP = "shop";
  static final String SELL = "sell";
  static final String LEAVE = "leave";
  static final String ESCAPE_BACK = "escapeBack";
  static final String YOUR_COIN = "yourCoin";

  final BufferedImage heartFull, heartHalf, heartBlank, manaCrystalFull, manaCrystalBlank, coin;
  final GamePanel gamePanel;
  Graphics2D graphics2D;
  final Font maruMonica, purisaBoldFont;
  final ArrayList<String> messages = new ArrayList<>();
  final ArrayList<Integer> messageCounter = new ArrayList<>();
  DialogueSpeaker dialogueObject;

  int commandNum = 0;
  SubState subState = DEFAULT;
  int playerSlotCol = 0;
  int playerSlotRow = 0;
  int npcSlotCol = 0;
  int npcSlotRow = 0;
  int transitionCounter = 0;
  int characterIndex = 0;
  String currentDialogue = StringUtils.EMPTY;
  final StringBuilder combinedText = new StringBuilder();

  public UI(GamePanel gamePanel) {
    this.gamePanel = gamePanel;
    maruMonica = Try.of(() -> loadFont(MARU_MONICA_FONT))
        .recover(ignored -> getDefaultFont())
        .get();
    purisaBoldFont = Try.of(() -> loadFont(PURISA_BOLD_FONT))
        .recover(ignored -> getDefaultFont())
        .get();

    // HUD sprites
    heartFull = SpriteLoader.load(HEART_FULL, TILE_SIZE, TILE_SIZE);
    heartHalf = SpriteLoader.load(HEART_HALF, TILE_SIZE, TILE_SIZE);
    heartBlank = SpriteLoader.load(HEART_BLANK, TILE_SIZE, TILE_SIZE);
    manaCrystalFull = SpriteLoader.load(MANA_CRYSTAL_FULL, TILE_SIZE, TILE_SIZE);
    manaCrystalBlank = SpriteLoader.load(MANA_CRYSTAL_BLANK, TILE_SIZE, TILE_SIZE);
    coin = SpriteLoader.load(BRONZE_COIN, TILE_SIZE, TILE_SIZE);
  }

  public void addMessage(String text) {
    messages.add(text);
    messageCounter.add(0);
  }

  public void draw(Graphics2D graphics2D) {
    this.graphics2D = graphics2D;
    graphics2D.setFont(maruMonica);
    graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics2D.setColor(Color.WHITE);

    switch (gamePanel.getGameState()) {
      case TITLE_STATE -> drawTitleScreen();
      case PLAY_STATE -> {
        drawPlayerLife();
        drawMonsterLife();
        drawMessage();
      }
      case PAUSE_STATE -> {
        drawPlayerLife();
        drawPauseScreen();
      }
      case DIALOG_STATE -> drawDialogueScreen();
      case CHARACTER_STATE -> {
        drawCharacterScreen();
        drawInventory(gamePanel.getPlayer(), true);
      }
      case OPTIONS_STATE -> drawOptionsScreen();
      case GAME_OVER_STATE -> drawGameOverScreen();
      case TRANSITION_STATE -> drawTransition();
      case TRADE_STATE -> drawTradeScreen();
      case SLEEP_STATE -> drawSleepScreen();
    }
  }

  private void drawSleepScreen() {
    transitionCounter++;

    if (transitionCounter < 120) {
      adjustLightingFilterAlpha(0.01F, 1.0F);
    } else {
      adjustLightingFilterAlpha(-0.01F, 0F);

      if (gamePanel.getEnvironmentManager().getLighting().getFilterAlpha() <= 0F) {
        transitionCounter = 0;
        gamePanel.getEnvironmentManager().getLighting().setDayState(DAY);
        gamePanel.getEnvironmentManager().getLighting().setDayCounter(0);
        gamePanel.setGameState(PLAY_STATE);
        gamePanel.getPlayer().getImage();
      }
    }
  }

  private void adjustLightingFilterAlpha(float delta, float limit) {
    Lighting lighting = gamePanel.getEnvironmentManager().getLighting();
    lighting.setFilterAlpha(Math.max(0F, Math.min(1.0F, lighting.getFilterAlpha() + delta)));
  }

  private void drawGameOverScreen() {
    graphics2D.setColor(new Color(0, 0, 0, 150));
    graphics2D.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());

    // SHADOW
    graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD, 110F));
    graphics2D.setColor(Color.BLACK);
    int x = getXForCenteredText(TextManager.getUiText(UI_MESSAGES, GAME_OVER));
    int y = TILE_SIZE * 4;
    graphics2D.drawString(TextManager.getUiText(UI_MESSAGES, GAME_OVER), x, y);

    // MAIN
    graphics2D.setColor(Color.WHITE);
    graphics2D.drawString(TextManager.getUiText(UI_MESSAGES, GAME_OVER), x - 4, y - 4);

    // RETRY
    graphics2D.setFont(graphics2D.getFont().deriveFont(50F));
    x = getXForCenteredText(TextManager.getSettingText(MENU_OPTIONS, RETRY));
    y += TILE_SIZE * 4;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, RETRY), x, y);
    if (commandNum == 0)
      graphics2D.drawString(CURSOR_SELECTOR, x - 40, y);

    // BACK TO TITLE SCREEN
    x = getXForCenteredText(TextManager.getSettingText(MENU_OPTIONS, QUIT));
    y += 55;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, QUIT), x, y);
    if (commandNum == 1)
      graphics2D.drawString(CURSOR_SELECTOR, x - 40, y);
  }

  private void drawOptionsScreen() {
    graphics2D.setColor(Color.WHITE);
    graphics2D.setFont(graphics2D.getFont().deriveFont(32F));

    // SUB WINDOW
    int frameX = TILE_SIZE * 6;
    int frameY = TILE_SIZE;
    int frameWidth = TILE_SIZE * 8;
    int frameHeight = TILE_SIZE * 10;
    drawSubWindow(frameX, frameY, frameWidth, frameHeight);

    switch (subState) {
      case MAIN_SETTINGS -> optionsMainSettings(frameX, frameY);
      case FULL_SCREEN_OPTION -> optionsFullScreenNotification(frameX, frameY);
      case CONTROL_OPTIONS -> optionsControl(frameX, frameY);
      case END_GAME -> optionsEndGameConfirmation(frameX, frameY);
    }

    gamePanel.getKeyHandler().setEnterPressed(false);
  }

  private void drawTradeScreen() {
    switch (subState) {
      case DEFAULT -> tradeSelect();
      case NPC_INVENTORY -> tradeBuy();
      case PLAYER_INVENTORY -> tradeSell();
    }
    gamePanel.getKeyHandler().setEnterPressed(false);
  }

  private void tradeSelect() {
    dialogueObject.setDialogueSet(MERCHANT_TRADE_DIALOGUE_KEY);
    drawDialogueScreen();
    // DRAW WINDOW

    int x = TILE_SIZE * 15;
    int y = TILE_SIZE * 4;
    int width = TILE_SIZE * 3;
    int height = TILE_SIZE * 4;

    drawSubWindow(x, y, width, height);

    // DRAW TEXTS
    x += TILE_SIZE;
    y += TILE_SIZE;
    graphics2D.drawString(TextManager.getUiText(SHOP, BUY), x, y);
    if (commandNum == 0) {
      graphics2D.drawString(CURSOR_SELECTOR, x - 24, y);
      if (gamePanel.getKeyHandler().isEnterPressed())
        subState = NPC_INVENTORY;
    }
    y += TILE_SIZE;
    graphics2D.drawString(TextManager.getUiText(SHOP, SELL), x, y);
    if (commandNum == 1) {
      graphics2D.drawString(CURSOR_SELECTOR, x - 24, y);
      if (gamePanel.getKeyHandler().isEnterPressed())
        subState = PLAYER_INVENTORY;
    }
    y += TILE_SIZE;
    graphics2D.drawString(TextManager.getUiText(SHOP, LEAVE), x, y);
    if (commandNum == 2) {
      graphics2D.drawString(CURSOR_SELECTOR, x - 24, y);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        commandNum = 0;
        dialogueObject.startDialogue(dialogueObject, MERCHANT_COME_AGAIN_DIALOGUE_KEY);
      }
    }
  }

  private void tradeBuy() {
    drawInventory(gamePanel.getPlayer(), false);
    if (dialogueObject instanceof Actor tradingMerchant) {
      drawInventory(tradingMerchant, true);
    }

    // DRAW HINT WINDOW
    int x = TILE_SIZE * 2;
    int y = TILE_SIZE * 9;
    int width = TILE_SIZE * 6;
    int height = TILE_SIZE * 2;
    drawSubWindow(x, y, width, height);
    graphics2D.drawString(TextManager.getSettingText(CONTROLS, ESCAPE_BACK), x + 24, y + 60);

    // DRAW PLAYER COIN WINDOW
    x = TILE_SIZE * 12;
    drawSubWindow(x, y, width, height);
    graphics2D.drawString(String.format(TextManager.getUiText(UI_MESSAGES, YOUR_COIN), gamePanel.getPlayer().getMoney()), x + 24, y + 60);

    // DRAW PRICE WINDOW
    int itemIndex = getItemIndexFromInventory(npcSlotCol, npcSlotRow);
    if (!(dialogueObject instanceof Actor tradingMerchant)) {
      return;
    }
    if (itemIndex < tradingMerchant.getInventory().size()) {
      x = (int) (TILE_SIZE * 5.5);
      y = (int) (TILE_SIZE * 5.5);
      width = (int) (TILE_SIZE * 2.5);
      height = TILE_SIZE;
      drawSubWindow(x, y, width, height);
      graphics2D.drawImage(coin, x + 10, y + 8, 32, 32, null);

      int price = tradingMerchant.getInventory().get(itemIndex).getPrice();
      x = getXForAlignTextToRight(String.valueOf(price), TILE_SIZE * 8 - 20);
      graphics2D.drawString(String.valueOf(price), x, y + 34);

      // BUY ITEM
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        if (tradingMerchant.getInventory().get(itemIndex).getPrice() > gamePanel.getPlayer().getMoney()) {
          subState = DEFAULT;
          gamePanel.setGameState(DIALOG_STATE);
          dialogueObject.startDialogue(dialogueObject, MERCHANT_NO_MONEY_DIALOGUE_KEY);
        } else if (!gamePanel.getPlayer().canObtainItem(tradingMerchant.getInventory().get(itemIndex))) {
          subState = DEFAULT;
          dialogueObject.startDialogue(dialogueObject, MERCHANT_TO_MUCH_ITEMS_DIALOGUE_KEY);
        } else {
          gamePanel.getPlayer().setMoney(
              gamePanel.getPlayer().getMoney() - tradingMerchant.getInventory().get(itemIndex).getPrice()
          );
        }
      }
    }
  }

  private void tradeSell() {
    drawInventory(gamePanel.getPlayer(), true);

    // DRAW HINT WINDOW
    int x = TILE_SIZE * 2;
    int y = TILE_SIZE * 9;
    int width = TILE_SIZE * 6;
    int height = TILE_SIZE * 2;
    drawSubWindow(x, y, width, height);
    graphics2D.drawString(TextManager.getSettingText(CONTROLS, ESCAPE_BACK), x + 24, y + 60);

    // DRAW PLAYER COIN WINDOW
    x = TILE_SIZE * 12;
    drawSubWindow(x, y, width, height);
    graphics2D.drawString(String.format(TextManager.getUiText(UI_MESSAGES, YOUR_COIN), gamePanel.getPlayer().getMoney()), x + 24, y + 60);

    // DRAW PRICE WINDOW
    int itemIndex = getItemIndexFromInventory(playerSlotCol, playerSlotRow);
    if (itemIndex < gamePanel.getPlayer().getInventory().size()) {
      x = (int) (TILE_SIZE * 15.5);
      y = (int) (TILE_SIZE * 5.5);
      width = (int) (TILE_SIZE * 2.5);
      height = TILE_SIZE;
      drawSubWindow(x, y, width, height);
      graphics2D.drawImage(coin, x + 10, y + 8, 32, 32, null);

      int price = gamePanel.getPlayer().getInventory().get(itemIndex).getPrice() / 2;
      x = getXForAlignTextToRight(String.valueOf(price), TILE_SIZE * 18 - 20);
      graphics2D.drawString(String.valueOf(price), x, y + 34);

      // SELL ITEM
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        if (gamePanel.getPlayer().getInventory().get(itemIndex) == gamePanel.getPlayer().getCurrentWeapon()
            || gamePanel.getPlayer().getInventory().get(itemIndex) == gamePanel.getPlayer().getCurrentShield()) {
          commandNum = 0;
          subState = DEFAULT;
          dialogueObject.startDialogue(dialogueObject, MERCHANT_CANNOT_SELL_KEY);
        } else {
          if (gamePanel.getPlayer().getInventory().get(itemIndex).getStackCount() > 1){
            int currentAmount = gamePanel.getPlayer().getInventory().get(itemIndex).getStackCount();
            gamePanel.getPlayer().getInventory().get(itemIndex).setStackCount(currentAmount - 1);
          }
          else
            gamePanel.getPlayer().getInventory().remove(itemIndex);
          gamePanel.getPlayer().setMoney(gamePanel.getPlayer().getMoney() + price);
        }
      }
    }
  }

  private void optionsEndGameConfirmation(int frameX, int frameY) {
    int textX = frameX + TILE_SIZE;
    int textY = frameY + TILE_SIZE * 3;

    for (String line : TextManager.getUiText(UI_MESSAGES, END_GAME_QUESTION).split("\n")) {
      graphics2D.drawString(line, textX, textY);
      textY += 40;
    }

    // YES OPTION
    textX = getXForCenteredText(TextManager.getUiText(DIALOGUES, YES));
    textY += TILE_SIZE * 3;
    graphics2D.drawString(TextManager.getUiText(DIALOGUES, YES), textX, textY);
    if (commandNum == 0) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        gamePanel.stopBGM();
        subState = MENU;
        graphics2D.setColor(Color.BLACK);
        graphics2D.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
        gamePanel.setGameState(TITLE_STATE);
        gamePanel.resetGame(true);
      }
    }

    // NO OPTION
    textX = getXForCenteredText(TextManager.getUiText(DIALOGUES, NO));
    textY += TILE_SIZE;
    graphics2D.drawString(TextManager.getUiText(DIALOGUES, NO), textX, textY);
    if (commandNum == 1) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        subState = MAIN_SETTINGS;
        commandNum = 4;
      }
    }
  }

  private void optionsMainSettings(int frameX, int frameY) {
    int textX;
    int textY;

    // TITLE
    textX = getXForCenteredText(TextManager.getSettingText(MENU_OPTIONS, OPTIONS));
    textY = frameY + TILE_SIZE;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, OPTIONS), textX, textY);

    // FULL SCREEN ON/OFF
    textX = frameX + TILE_SIZE;
    textY += TILE_SIZE * 2;
    graphics2D.drawString(TextManager.getSettingText(SETTINGS, FULL_SCREEN), textX, textY);
    if (commandNum == 0) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        gamePanel.setFullScreenOn(!gamePanel.isFullScreenOn());
        subState = FULL_SCREEN_OPTION;
      }
    }

    // MUSIC
    textY += TILE_SIZE;
    graphics2D.drawString(TextManager.getSettingText(SETTINGS, MUSIC), textX, textY);
    if (commandNum == 1)
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);

    // SE
    textY += TILE_SIZE;
    graphics2D.drawString(TextManager.getSettingText(SETTINGS, SOUND_EFFECT), textX, textY);
    if (commandNum == 2)
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
    // CONTROL
    textY += TILE_SIZE;
    graphics2D.drawString(TextManager.getSettingText(CONTROLS, CONTROL), textX, textY);
    if (commandNum == 3) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        subState = CONTROL_OPTIONS;
        commandNum = 0;
      }
    }

    // END GAME
    textY += TILE_SIZE;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, END_GAME), textX, textY);
    if (commandNum == 4) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        subState = SubState.END_GAME;
        commandNum = 0;
      }

    }
    // BACK
    textY += TILE_SIZE * 2;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, BACK), textX, textY);
    if (commandNum == 5) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        gamePanel.setGameState(PLAY_STATE);
        commandNum = 0;
        subState = DEFAULT;
      }
    }

    // FULL SCREEN CHECK BOX
    textX = frameX + (int) (TILE_SIZE * 4.5);
    textY = frameY + TILE_SIZE * 2 + TILE_SIZE / 2;
    graphics2D.setStroke(new BasicStroke(2));
    graphics2D.drawRect(textX, textY, TILE_SIZE / 2, TILE_SIZE / 2);
    if (gamePanel.isFullScreenOn()) {
      graphics2D.fillRect(textX, textY, TILE_SIZE / 2, TILE_SIZE / 2);
    }

    // MUSIC VOLUME
    textY += TILE_SIZE;
    graphics2D.drawRect(textX, textY, 120, TILE_SIZE / 2); // 120/5 = 24 px
    int volumeWidth = 24 * gamePanel.getMusic().getVolumeScale();
    graphics2D.fillRect(textX, textY, volumeWidth, 24);

    // SE VOLUME
    textY += TILE_SIZE;
    graphics2D.drawRect(textX, textY, 120, TILE_SIZE / 2);
    volumeWidth = 24 * gamePanel.getSoundEffect().getVolumeScale();
    graphics2D.fillRect(textX, textY, volumeWidth, 24);

    gamePanel.getConfig().saveConfig();
  }

  public void optionsControl(int frameX, int frameY) {
    int textX = getXForCenteredText(TextManager.getSettingText(CONTROLS, CONTROL));
    int textY = frameY + TILE_SIZE;
    graphics2D.drawString(TextManager.getSettingText(CONTROLS, CONTROL), textX, textY);

    textX = frameX + TILE_SIZE;
    textY += TILE_SIZE;

    String[] optionsLabels = {
        TextManager.getSettingText(CONTROLS, MOVE),
        TextManager.getSettingText(CONTROLS, CONFIRM_ATTACK),
        TextManager.getSettingText(CONTROLS, SHOOT_CAST),
        TextManager.getSettingText(CONTROLS, PARRY),
        TextManager.getSettingText(CONTROLS, CHARACTER_SCREEN),
        TextManager.getSettingText(CONTROLS, PAUSE),
        TextManager.getSettingText(MENU_OPTIONS, OPTIONS),
        TextManager.getSettingText(CONTROLS, MAP)
    };

    for (String label : optionsLabels) {
      graphics2D.drawString(label, textX, textY);
      textY += TILE_SIZE - 10;
    }

    textX = frameX + TILE_SIZE * 6;
    textY = frameY + TILE_SIZE * 2;

    String[] controlLabels = {
        CHARACTER_CONTROL, CONFIRM, PROJECTILE_KEY,
        SPACE_KEY, CHARACTER_KEY, PAUSE_KEY, CLOSE_KEY, MAP_KEY
    };

    for (String key : controlLabels) {
      graphics2D.drawString(key, textX, textY);
      textY += TILE_SIZE - 10;
    }

    // BACK
    textX = frameX + TILE_SIZE;
    textY = frameY + TILE_SIZE * 9;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, BACK), textX, textY);

    if (commandNum == 0) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        subState = MAIN_SETTINGS;
        commandNum = 3;
      }
    }
  }


  private void drawInventory(Actor gameEntity, boolean cursor) {
    int frameX;
    int frameY = TILE_SIZE;
    int frameWidth = TILE_SIZE * 6;
    int frameHeight = TILE_SIZE * 5;
    int slotCol;
    int slotRow;

    if (gameEntity == gamePanel.getPlayer()) {
      frameX = TILE_SIZE * 12;
      slotCol = playerSlotCol;
      slotRow = playerSlotRow;
    } else {
      frameX = TILE_SIZE * 2;
      slotCol = npcSlotCol;
      slotRow = npcSlotRow;
    }

    // FRAME
    drawSubWindow(frameX, frameY, frameWidth, frameHeight);

    // SLOT
    final int slotXStart = frameX + 20;
    final int slotYStart = frameY + 20;
    int slotX = slotXStart;
    int slotY = slotYStart;
    int slotSize = TILE_SIZE + 3;


    // DRAW PLAYER'S ITEMS
    for (int inventoryItem = 0; inventoryItem < gameEntity.getInventory().size(); inventoryItem++) {

      // EQUIP CURSOR
      if (gameEntity.getInventory().get(inventoryItem) == gameEntity.getCurrentWeapon() ||
          gameEntity.getInventory().get(inventoryItem) == gameEntity.getCurrentShield() ||
          gameEntity.getInventory().get(inventoryItem) == gameEntity.getCurrentLightItem()) {
        graphics2D.setColor(new Color(240, 190, 90));
        graphics2D.fillRoundRect(slotX, slotY, TILE_SIZE, TILE_SIZE, 10, 10);
      }
      graphics2D.drawImage(gameEntity.getInventory().get(inventoryItem).getIcon(), slotX, slotY, null);

      // DISPLAY AMOUNT
      if (gameEntity == gamePanel.getPlayer() && gameEntity.getInventory().get(inventoryItem).getStackCount() > 1) {
        graphics2D.setFont(graphics2D.getFont().deriveFont(32f));
        int amountX;
        int amountY;
        amountX = getXForAlignTextToRight(String.valueOf(gameEntity.getInventory().get(inventoryItem).getStackCount()), slotX + 44);
        amountY = slotY + TILE_SIZE;

        //SHADOW
        graphics2D.setColor(new Color(60, 60, 60));
        graphics2D.drawString(String.valueOf(gameEntity.getInventory().get(inventoryItem).getStackCount()), amountX, amountY);
        // NUMBER
        graphics2D.setColor(Color.WHITE);
        graphics2D.drawString(String.valueOf(gameEntity.getInventory().get(inventoryItem).getStackCount()), amountX - 3, amountY - 3);
      }
      slotX += slotSize;

      if (inventoryItem == 4 || inventoryItem == 9 || inventoryItem == 14) {
        slotX = slotXStart;
        slotY += slotSize;
      }
    }

    //CURSOR
    if (cursor) {
      // DRAW CURSOR
      int cursorX = slotXStart + (slotSize * slotCol);
      int cursorY = slotYStart + (slotSize * slotRow);

      graphics2D.setColor(Color.WHITE);
      graphics2D.setStroke(new BasicStroke(3));
      graphics2D.drawRoundRect(cursorX, cursorY, TILE_SIZE, TILE_SIZE, 10, 10);

      // DESCRIPTION FRAME
      int descriptionFrameY = frameY + frameHeight;
      int descriptionFrameHeight = TILE_SIZE * 3;

      // DESCRIPTION TEXT
      int textX = frameX + 20;
      int textY = descriptionFrameY + TILE_SIZE;
      graphics2D.setFont(graphics2D.getFont().deriveFont(28F));

      int itemIndex = getItemIndexFromInventory(slotCol, slotRow);
      if (itemIndex < gameEntity.getInventory().size()) {
        drawSubWindow(frameX, descriptionFrameY, frameWidth, descriptionFrameHeight);
        for (String line : gameEntity.getInventory().get(itemIndex).getDescription().split("\n")) {
          graphics2D.drawString(line, textX, textY);
          textY += 32;
        }
      }
    }
  }

  public int getItemIndexFromInventory(int slotCol, int slotRow) {
    return slotCol + (slotRow * 5);
  }

  private void optionsFullScreenNotification(int frameX, int frameY) {
    int textX = frameX + TILE_SIZE;
    int textY = frameY + TILE_SIZE * 3;

    for (String line : TextManager.getUiText(UI_MESSAGES, FULL_SCREEN_NOTIFICATION).split("\n")) {
      graphics2D.drawString(line, textX, textY);
      textY += 40;
    }

    // BACK
    textY = frameY + TILE_SIZE * 9;
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, BACK), textX, textY);
    if (commandNum == 0) {
      graphics2D.drawString(CURSOR_SELECTOR, textX - 25, textY);
      if (gamePanel.getKeyHandler().isEnterPressed()) {
        subState = MAIN_SETTINGS;
      }
    }
  }

  private void drawMessage() {
    int messageX = TILE_SIZE;
    int messageY = TILE_SIZE * 4;

    graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD, 20F));

    for (int i = 0; i < messages.size(); i++) {
      if (Objects.nonNull(messages.get(i))) {
        graphics2D.setColor(Color.BLACK);
        graphics2D.drawString(messages.get(i), messageX + 2, messageY + 2);
        graphics2D.setColor(Color.WHITE);
        graphics2D.drawString(messages.get(i), messageX, messageY);

        int counter = messageCounter.get(i) + 1;
        messageCounter.set(i, counter);
        messageY += 50;

        if (messageCounter.get(i) > 180) {
          messages.remove(i);
          messageCounter.remove(i);
        }
      }
    }
  }

  private void drawCharacterScreen() {
    // CREATE A FRAME
    final int frameX = TILE_SIZE * 2;
    final int frameY = TILE_SIZE;
    final int frameWidth = TILE_SIZE * 6;
    final int frameHeight = TILE_SIZE * 10;

    // TEXT
    graphics2D.setColor(Color.WHITE);
    graphics2D.setFont(graphics2D.getFont().deriveFont(25F));
    drawSubWindow(frameX, frameY, frameWidth, frameHeight);

    int textX = frameX + 20;
    int textY = frameY + TILE_SIZE;
    final int lineHeight = 35;

    // NAMES
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, LEVEL), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, LIFE), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, STRENGTH), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, DEXTERITY), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, ATTACK), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, DEFENSE), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, EXP), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, NEXT_LEVEL), textX, textY);
    textY += lineHeight;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, COIN), textX, textY);
    textY += lineHeight + 20;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, WEAPON), textX, textY);
    textY += lineHeight + 15;
    graphics2D.drawString(TextManager.getUiText(GAME_STATS, SHIELD), textX, textY);

    // VALUES
    int tailX = (frameX + frameWidth) - 30;

    textY = frameY + TILE_SIZE;
    String value;

    value = String.valueOf(gamePanel.getPlayer().getLevel());
    textX = getXForAlignTextToRight(value, tailX);
    graphics2D.drawString(value, textX, textY);
    textY += lineHeight;

    String playerLife = String.format(PLAYER_LIFE_FORMAT, gamePanel.getPlayer().getCurrentLife(), gamePanel.getPlayer().getMaxLife());
    textX = getXForAlignTextToRight(playerLife, tailX);
    graphics2D.drawString(playerLife, textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getStrength()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getStrength()), textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getDexterity()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getDexterity()), textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getAttack()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getAttack()), textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getDefense()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getDefense()), textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getExp()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getExp()), textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getNextLevelExp()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getNextLevelExp()), textX, textY);
    textY += lineHeight;

    textX = getXForAlignTextToRight(String.valueOf(gamePanel.getPlayer().getMoney()), tailX);
    graphics2D.drawString(String.valueOf(gamePanel.getPlayer().getMoney()), textX, textY);
    textY += lineHeight;

    graphics2D.drawImage(gamePanel.getPlayer().getCurrentWeapon().getIcon(), tailX - TILE_SIZE, textY - 14, null);
    textY += TILE_SIZE;

    graphics2D.drawImage(gamePanel.getPlayer().getCurrentShield().getIcon(), tailX - TILE_SIZE, textY - 14, null);
  }

  private void drawPlayerLife() {
    int x = TILE_SIZE - 15;
    int y = TILE_SIZE / 2;
    int iconSize = 32;
    int i = 0;

    // DRAW MAX LIFE
    while (i < gamePanel.getPlayer().getMaxLife() / 2) {
      graphics2D.drawImage(heartBlank, x, y, iconSize, iconSize, null);
      i++;
      x += TILE_SIZE - 15;
    }

    x = TILE_SIZE - 15;
    i = 0;

    // DRAW CURRENT LIFE
    while (i < gamePanel.getPlayer().getCurrentLife()) {
      graphics2D.drawImage(heartHalf, x, y, iconSize, iconSize, null);
      i++;
      if (i < gamePanel.getPlayer().getCurrentLife()) {
        graphics2D.drawImage(heartFull, x, y, iconSize, iconSize, null);
      }
      i++;
      x += TILE_SIZE - 15;
    }

    // DRAW MAX MANA
    x = TILE_SIZE - 20;
    y = TILE_SIZE + 10;
    i = 0;
    while (i < gamePanel.getPlayer().getMaxMana()) {
      graphics2D.drawImage(manaCrystalBlank, x, y, iconSize, iconSize, null);
      i++;
      x += 15;
    }

    // DRAW MANA
    x = TILE_SIZE - 20;
    i = 0;
    while (i < gamePanel.getPlayer().getMana()) {
      graphics2D.drawImage(manaCrystalFull, x, y, iconSize, iconSize, null);
      i++;
      x += 15;
    }
  }

  public void drawMonsterLife() {
    var monsters = gamePanel.getWorld().currentMonsters();
    if (Objects.isNull(monsters)) return;

    for (Actor monster : monsters) {
      if (Objects.nonNull(monster) && monster.inCamera()) {
        boolean isBoss = monster.isBoss();
        double oneScaleLifeBar = (double) (TILE_SIZE * (isBoss ? 8 : 1)) / monster.getMaxLife();
        double hpBarValue = oneScaleLifeBar * monster.getCurrentLife();
        int barWidth = TILE_SIZE * (isBoss ? 8 : 1);
        int barHeight = isBoss ? 20 : 10;
        int x = isBoss ? (gamePanel.getScreenWidth() / 2 - TILE_SIZE * 4) : monster.getScreenX();
        int y = isBoss ? (TILE_SIZE * 10) : (monster.getScreenY() - 15);

        if (monster.isHpBarOn()) {
          graphics2D.setColor(new Color(35, 35, 35));
          graphics2D.fillRect(x - 1, y - 1, barWidth + 2, barHeight + 2);

          graphics2D.setColor(new Color(255, 0, 30));
          graphics2D.fillRect(x, y, (int) hpBarValue, barHeight);
        }

        if (isBoss) {
          graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD, 24F));
          graphics2D.setColor(Color.WHITE);
          graphics2D.drawString(monster.getName(), x + 4, y - 10);
        } else {
          monster.setHpBarCounter(monster.getHpBarCounter() + 1);
          if (monster.getHpBarCounter() > 600) {
            monster.setHpBarCounter(0);
            monster.setHpBarOn(false);
          }
        }
      }
    }
  }

  private void drawTitleScreen() {
    // CLEAR
    graphics2D.setColor(Color.BLACK);
    graphics2D.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());
    // TITLE NAME
    graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD, 96F));
    int x = getXForCenteredText(TextManager.getSettingText(MENU_OPTIONS, TITLE));
    int y = TILE_SIZE * 3;

    // SHADOW
    graphics2D.setColor(Color.GRAY);
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, TITLE), x + 5, y + 5);
    // MAIN COLOR
    graphics2D.setColor(Color.WHITE);
    graphics2D.drawString(TextManager.getSettingText(MENU_OPTIONS, TITLE), x, y);

    // BLUE BOY IMAGE
    x = gamePanel.getScreenWidth() / 2 - (TILE_SIZE * 2) / 2;
    y += TILE_SIZE * 2;
    graphics2D.drawImage(gamePanel.getPlayer().getDown1(), x, y, TILE_SIZE * 2, TILE_SIZE * 2, null);

    // MENU
    graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD, 48F));

    String[] menuOptions = {
        TextManager.getSettingText(MENU_OPTIONS, NEW_GAME),
        TextManager.getSettingText(MENU_OPTIONS, LOAD_GAME),
        TextManager.getSettingText(MENU_OPTIONS, QUIT)
    };

    y += TILE_SIZE * 3;

    for (int i = 0; i < menuOptions.length; i++) {
      x = getXForCenteredText(menuOptions[i]);
      graphics2D.drawString(menuOptions[i], x, y);

      if (commandNum == i) {
        graphics2D.drawString(CURSOR_SELECTOR, x - TILE_SIZE, y);
      }

      y += TILE_SIZE;
    }
  }

  public void drawDialogueScreen() {
    // WINDOW
    int x = TILE_SIZE * 3;
    int y = TILE_SIZE / 2;
    int width = gamePanel.getScreenWidth() - (TILE_SIZE * 6);
    int height = TILE_SIZE * 4;
    drawSubWindow(x, y, width, height);

    graphics2D.setFont(graphics2D.getFont().deriveFont(Font.PLAIN, 25F));
    x += TILE_SIZE;
    y += TILE_SIZE;

    List<String> dialoguesForSet = dialogueObject.getDialogues().get(dialogueObject.getDialogueSet());

    if (Objects.nonNull(dialoguesForSet) && !dialoguesForSet.isEmpty()) {
      if (dialogueObject.getDialogueIndex() >= dialoguesForSet.size()) {
        dialogueObject.setDialogueIndex(0);
      }

      String fullDialogue = dialoguesForSet.get(dialogueObject.getDialogueIndex());
      currentDialogue = fullDialogue;

      if (characterIndex < fullDialogue.length()) {
        combinedText.append(fullDialogue.charAt(characterIndex));
        currentDialogue = combinedText.toString();
        characterIndex++;
        gamePanel.playSE(SPEAK);
      }

      if (gamePanel.getKeyHandler().isEnterPressed() && (gamePanel.getGameState() == DIALOG_STATE
          || gamePanel.getGameState() == CUTSCENE_STATE)) {

        characterIndex = 0;
        combinedText.setLength(0);
        dialogueObject.setDialogueIndex(dialogueObject.getDialogueIndex() + 1);
        gamePanel.getKeyHandler().setEnterPressed(false);


        if (dialogueObject.getDialogueIndex() >= dialoguesForSet.size()) {
          dialogueObject.setDialogueIndex(0);
          if (gamePanel.getGameState() == CUTSCENE_STATE) {
            gamePanel.getCutsceneManager().setScenePhase(gamePanel.getCutsceneManager().getScenePhase() + 1);
          } else {
            gamePanel.setGameState(PLAY_STATE);
          }
        }
      }
    } else {
      dialogueObject.setDialogueIndex(0);
      if (gamePanel.getGameState() == DIALOG_STATE) {
        gamePanel.setGameState(PLAY_STATE);
      }
    }

    for (String line : currentDialogue.split("\n")) {
      graphics2D.drawString(line, x, y);
      y += 40;
    }
  }

  public int getXForCenteredText(String text) {
    int length = (int) graphics2D.getFontMetrics().getStringBounds(text, graphics2D).getWidth();
    return gamePanel.getScreenWidth() / 2 - length / 2;
  }

  private void drawSubWindow(int x, int y, int width, int height) {
    graphics2D.setColor(new Color(0, 0, 0, 220));
    graphics2D.fillRoundRect(x, y, width, height, 35, 35);

    graphics2D.setStroke(new BasicStroke(5));
    graphics2D.setColor(new Color(255, 255, 255));
    graphics2D.drawRoundRect(x + 5, y + 5, width - 10, height - 10, 25, 25);

  }

  private void drawTransition() {
    transitionCounter++;
    graphics2D.setColor(new Color(0, 0, 0, transitionCounter * 5));
    graphics2D.fillRect(0, 0, gamePanel.getScreenWidth(), gamePanel.getScreenHeight());

    if (transitionCounter == 50) {
      transitionCounter = 0;
      gamePanel.setGameState(PLAY_STATE);
      gamePanel.getWorld().switchMap(gamePanel.getEventHandler().getTempMap());
      gamePanel.getGameMap().createWorldMap();
      gamePanel.getPlayer().setWorldX(TILE_SIZE * gamePanel.getEventHandler().getTempCol());
      gamePanel.getPlayer().setWorldY(TILE_SIZE * gamePanel.getEventHandler().getTempRow());
      gamePanel.getEventHandler().setPreviousEventX(gamePanel.getPlayer().getWorldX());
      gamePanel.getEventHandler().setPreviousEventY(gamePanel.getPlayer().getWorldY());
      gamePanel.changeArea();
    }
  }

  private void drawPauseScreen() {
    graphics2D.setFont(graphics2D.getFont().deriveFont(Font.PLAIN, 80F));
    int x = getXForCenteredText(TextManager.getUiText(UI_MESSAGES, PAUSED));
    int y = gamePanel.getScreenHeight() / 2;

    graphics2D.drawString(TextManager.getUiText(UI_MESSAGES, PAUSED), x, y);
  }

  private int getXForAlignTextToRight(String text, int tailX) {
    int length = (int) graphics2D.getFontMetrics().getStringBounds(text, graphics2D).getWidth();
    return tailX - length;
  }

  private Font loadFont(String fontPath) throws Exception {
    try (InputStream fontInputStream = Objects.requireNonNull(getClass().getResourceAsStream(fontPath))) {
      return Font.createFont(Font.TRUETYPE_FONT, fontInputStream);
    }
  }

  private Font getDefaultFont() {
    log.info("Loading default system font due to an error.");
    return new Font(DEFAULT_FONT, Font.PLAIN, 12);
  }
}
