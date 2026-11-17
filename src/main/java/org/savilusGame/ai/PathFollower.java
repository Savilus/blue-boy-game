package org.savilusGame.ai;

import java.util.List;
import java.util.Objects;

import org.savilusGame.service.PathfindingService;

/** One actor's cursor. A changed goal, changed map topology or off-route displacement replans;
 * staying on a tile (including a failed search) does not. */
public final class PathFollower {
  private final PathfindingService finder;
  private NavigationGrid grid;
  private Node position;
  private Node goal;
  private List<Node> remaining = List.of();

  public PathFollower(PathfindingService finder) {
    this.finder = Objects.requireNonNull(finder);
  }

  public List<Node> remaining() {
    return remaining;
  }

  public Node nextStep(NavigationGrid currentGrid, int column, int row, int goalColumn, int goalRow) {
    Objects.requireNonNull(currentGrid);
    if (grid == currentGrid && goal.isAt(goalColumn, goalRow)) {
      if (position.isAt(column, row)) return next();
      if (!remaining.isEmpty() && remaining.getFirst().isAt(column, row)) {
        position = remaining.getFirst();
        remaining = remaining.subList(1, remaining.size());
        return next();
      }
    }
    var start = new Node(column, row);
    var target = new Node(goalColumn, goalRow);
    remaining = finder.search(currentGrid, start, target).orElse(List.of());
    position = start;
    goal = target;
    grid = currentGrid;
    return next();
  }

  private Node next() {
    return remaining.isEmpty() ? null : remaining.getFirst();
  }
}
