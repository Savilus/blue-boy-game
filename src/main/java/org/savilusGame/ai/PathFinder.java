package org.savilusGame.ai;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;

import org.savilusGame.service.PathfindingService;

/** Stateless A*: only visited cells have search records. No shared frontier or step limit. */
public final class PathFinder implements PathfindingService {
  private static final Comparator<Visit> BY_ESTIMATE = Comparator.comparingInt(Visit::estimate)
      .thenComparing(Comparator.comparingInt(Visit::distance).reversed())
      .thenComparingInt(visit -> visit.cell().row())
      .thenComparingInt(visit -> visit.cell().column());

  @Override
  public Optional<List<Node>> search(NavigationGrid grid, Node start, Node goal) {
    if (!grid.isWalkable(start.column(), start.row()) || !grid.isWalkable(goal.column(), goal.row())) {
      return Optional.empty();
    }
    var frontier = new PriorityQueue<>(BY_ESTIMATE);
    Map<Node, Visit> best = new HashMap<>();
    var first = new Visit(start, null, 0, start.distanceTo(goal));
    best.put(start, first);
    frontier.add(first);

    while (!frontier.isEmpty()) {
      Visit current = frontier.remove();
      if (best.get(current.cell()) != current) continue; // superseded by a shorter route
      if (current.cell().equals(goal)) return Optional.of(stepsTo(current));

      int column = current.cell().column();
      int row = current.cell().row();
      visit(grid, column, row - 1, goal, current, best, frontier);
      visit(grid, column - 1, row, goal, current, best, frontier);
      visit(grid, column, row + 1, goal, current, best, frontier);
      visit(grid, column + 1, row, goal, current, best, frontier);
    }
    return Optional.empty();
  }

  private static void visit(NavigationGrid grid, int column, int row, Node goal, Visit parent,
                            Map<Node, Visit> best, PriorityQueue<Visit> frontier) {
    if (!grid.isWalkable(column, row)) return;
    var cell = new Node(column, row);
    int distance = parent.distance() + 1;
    Visit previous = best.get(cell);
    if (previous != null && previous.distance() <= distance) return;

    var next = new Visit(cell, parent, distance, distance + cell.distanceTo(goal));
    best.put(cell, next);
    frontier.add(next);
  }

  private static List<Node> stepsTo(Visit last) {
    var steps = new ArrayDeque<Node>();
    for (Visit visit = last; visit.parent() != null; visit = visit.parent()) {
      steps.addFirst(visit.cell());
    }
    return List.copyOf(steps);
  }

  private record Visit(Node cell, Visit parent, int distance, int estimate) {}
}
