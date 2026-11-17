package org.savilusGame.ai;

/** Read-only snapshot of a finite map. Out-of-map cells are never walkable. */
@FunctionalInterface
public interface NavigationGrid {
  boolean isWalkable(int column, int row);
}
