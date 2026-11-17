package org.savilusGame.utils.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;
import static org.savilusGame.enums.GameState.CUTSCENE_STATE;
import static org.savilusGame.enums.GameState.TITLE_STATE;

import java.awt.Rectangle;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;

class EventHandlerTest {

  @Test
  void dungeonEventUsesItsOwnMapState() {
    GamePanel panel = new GamePanel();
    EventHandler handler = panel.getEventHandler();
    EventRect mainEvent = handler.getEventRect().get(MAIN_MAP)[25][27];
    EventRect dungeonEvent = handler.getEventRect().get(DUNGEON_SECOND_FLOR)[25][27];
    assertNotSame(mainEvent, dungeonEvent);

    panel.getWorld().switchMap(DUNGEON_SECOND_FLOR);
    panel.getPlayer().setWorldPosition(
        25 * TILE_SIZE + 23 - panel.getPlayer().getSolidArea().x,
        27 * TILE_SIZE + 23 - panel.getPlayer().getSolidArea().y
    );
    mainEvent.oneTimeEventDone = true;
    dungeonEvent.oneTimeEventDone = true;
    handler.checkEvent();
    assertEquals(TITLE_STATE, panel.getGameState());

    dungeonEvent.oneTimeEventDone = false;
    handler.checkEvent();
    assertEquals(CUTSCENE_STATE, panel.getGameState());
  }

  @Test
  void missedEventsDoNotResetOrMoveLocalRectangles() {
    var panel = new GamePanel();
    var event = panel.getEventHandler().getEventRect().get(MAIN_MAP)[10][39];
    event.setLocation(10, 14);
    var player = panel.getPlayer();
    player.setSolidArea(new Rectangle(1, 2, 12, 13));
    panel.getEventHandler().checkEvent();
    assertEquals(new Rectangle(1, 2, 12, 13), player.getSolidArea());
    assertEquals(new Rectangle(10, 14, 2, 2), event);
    assertEquals(TITLE_STATE, panel.getGameState());
  }
}
