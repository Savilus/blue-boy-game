package org.savilusGame.service;

import java.util.List;

import org.savilusGame.ai.Node;

/**
 * Abstraction over the A* path finding facility.
 */
public interface PathfindingService {

  /**
   * Prepare the node grid for a path search from the given start cell to the given goal cell.
   */
  void setNodes(int startCol, int startRow, int goalCol, int goalRow);

  /** Execute the search. */
  boolean search();

  /** The ordered list of nodes of the last computed path (first element = next step). */
  List<Node> getPathList();
}
