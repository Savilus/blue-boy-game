package org.savilusGame.ai;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.savilusGame.service.PathfindingService;

class PathFinderTest {
  private final PathfindingService finder = new PathFinder();

  @Test
  void shortestRouteGoesAroundTheWallAndIncludesOnlyAdjacentWalkableSteps() {
    NavigationGrid grid = (x, y) -> x >= 0 && x < 5 && y >= 0 && y < 4 && (x != 2 || y == 3);
    Node start = new Node(0, 0);
    Node goal = new Node(4, 0);
    List<Node> steps = finder.search(grid, start, goal).orElseThrow();
    assertEquals(10, steps.size());
    Node previous = start;
    for (Node step : steps) {
      assertEquals(1, previous.distanceTo(step));
      assertTrue(grid.isWalkable(step.column(), step.row()));
      previous = step;
    }
    assertEquals(goal, steps.getLast());
    assertThrows(UnsupportedOperationException.class, steps::clear);
  }

  @Test
  void arrivalIsNotConfusedWithNoRouteOrAnInvalidEndpoint() {
    NavigationGrid grid = (x, y) -> (x == 0 || x == 2) && y == 0;
    Node start = new Node(0, 0);
    assertEquals(List.of(), finder.search(grid, start, start).orElseThrow());
    assertTrue(finder.search(grid, start, new Node(2, 0)).isEmpty());
    assertTrue(finder.search(grid, start, new Node(1, 0)).isEmpty());
    assertTrue(finder.search(grid, new Node(-1, 0), start).isEmpty());
  }

  @Test
  void longRoutesAreNotTruncatedOrOverwrittenByAnotherSearch() {
    NavigationGrid corridor = (x, y) -> x >= 0 && x <= 180 && y == 0;
    List<Node> east = finder.search(corridor, new Node(0, 0), new Node(180, 0)).orElseThrow();
    List<Node> west = finder.search(corridor, new Node(180, 0), new Node(0, 0)).orElseThrow();
    assertEquals(180, east.size());
    assertEquals(new Node(1, 0), east.getFirst());
    assertEquals(new Node(180, 0), east.getLast());
    assertEquals(new Node(179, 0), west.getFirst());
  }
}
