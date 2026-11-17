package org.savilusGame.service;

import org.savilusGame.enums.Direction;

/** Something that can be moved one step in a direction. */
public interface Movable {

  void move(Direction direction);
}
