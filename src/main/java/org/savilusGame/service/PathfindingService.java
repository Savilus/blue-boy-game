package org.savilusGame.service;

import java.util.List;
import java.util.Optional;

import org.savilusGame.ai.NavigationGrid;
import org.savilusGame.ai.Node;

@FunctionalInterface
public interface PathfindingService {

  /** Shortest four-way route, excluding start and including goal. An empty route means arrived;
   * an empty Optional means unreachable. The returned steps are immutable and belong to this search. */
  Optional<List<Node>> search(NavigationGrid grid, Node start, Node goal);
}
