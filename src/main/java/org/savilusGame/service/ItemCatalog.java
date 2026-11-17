package org.savilusGame.service;

import org.savilusGame.entity.item.Item;

/**
 * Catalog of items (weapons, shields, potions, pick-ups...) by their stored (localized) name
 * or raw {@code GameObject} enum constant.
 */
public interface ItemCatalog {

  /** Create a fresh item bound to {@code name}, or {@code null} when the name is unknown. */
  Item getItem(String name);
}
