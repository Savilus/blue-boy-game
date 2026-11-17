package org.savilusGame.ai;

/** A tile coordinate, not a mutable A* work node. */
public record Node(int column, int row) {

  boolean isAt(int column, int row) {
    return this.column == column && this.row == row;
  }

  int distanceTo(Node other) {
    return Math.abs(column - other.column) + Math.abs(row - other.row);
  }
}
