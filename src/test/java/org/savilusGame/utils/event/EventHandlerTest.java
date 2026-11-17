package org.savilusGame.utils.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.savilusGame.GamePanel.TILE_SIZE;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SECOND_FLOR;
import static org.savilusGame.config.GameEntityNameFactory.MAIN_MAP;
import static org.savilusGame.enums.GameState.CUTSCENE_STATE;
import static org.savilusGame.enums.GameState.TITLE_STATE;

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
}
