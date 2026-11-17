package org.savilusGame.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.savilusGame.service.PathfindingService;

class PathFollowerTest {
  private final NavigationGrid lane = (x, y) -> x >= 0 && x <= 4 && y == 0;
  private final AtomicInteger searches = new AtomicInteger();
  private final PathfindingService finder = (grid, start, goal) -> {
    searches.incrementAndGet();
    return new PathFinder().search(grid, start, goal);
  };
  private final PathFollower follower = new PathFollower(finder);

  @Test
  void manyTicksAndTwoAgentsAdvanceIndependentCursorsWithoutRepeatingSearches() {
    var other = new PathFollower(finder);
    for (int tick = 0; tick < 60; tick++) {
      assertEquals(new Node(1, 0), follower.nextStep(lane, 0, 0, 3, 0));
      assertEquals(new Node(3, 0), other.nextStep(lane, 4, 0, 2, 0));
    }
    assertEquals(new Node(2, 0), follower.nextStep(lane, 1, 0, 3, 0));
    assertEquals(List.of(new Node(2, 0), new Node(3, 0)), follower.remaining());
    assertEquals(List.of(new Node(3, 0), new Node(2, 0)), other.remaining());
    follower.nextStep(lane, 2, 0, 3, 0);
    assertNull(follower.nextStep(lane, 3, 0, 3, 0));
    assertTrue(follower.remaining().isEmpty());
    assertEquals(2, searches.get());
  }

  @Test
  void movingGoalDisplacementAndReplacementTerrainInvalidateEvenAnUnreachableRoute() {
    follower.nextStep(lane, 0, 0, 3, 0);
    follower.nextStep(lane, 0, 0, 4, 0);
    assertEquals(new Node(4, 0), follower.remaining().getLast());
    assertEquals(new Node(3, 0), follower.nextStep(lane, 2, 0, 4, 0));
    assertEquals(3, searches.get());
    NavigationGrid blocked = (x, y) -> lane.isWalkable(x, y) && x != 3;
    assertNull(follower.nextStep(blocked, 2, 0, 4, 0));
    assertNull(follower.nextStep(blocked, 2, 0, 4, 0));
    assertTrue(follower.remaining().isEmpty());
    assertEquals(4, searches.get());
    assertEquals(new Node(3, 0), follower.nextStep(lane, 2, 0, 4, 0));
    assertEquals(5, searches.get());
  }
}
