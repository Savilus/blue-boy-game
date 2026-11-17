package org.savilusGame.enums;

import lombok.Getter;

/**
 * Stable identities of the game's items and world objects.
 * <p>The enum constant is the locale-independent id used by saves and by identity checks;
 * {@code textKey} addresses the display texts in {@link org.savilusGame.utils.text.TextManager}.
 * <p>The enum deliberately holds data only: loading it must never read text files, touch another
 * class's static initialiser or perform I/O — that is what made class initialisation failure
 * indistinguishable from a packaging problem. Display names are resolved through
 * {@code TextManager.getItemName(textKey)} at the call site.
 */
@Getter
public enum GameObject {
  // GAME ITEMS
  KEY("key"),
  DOOR("door"),
  IRON_DOOR("ironDoor"),
  BOOTS("boots"),
  HEART("heart"),
  CHEST("chest"),
  LANTERN("lantern"),
  TENT("tent"),
  RED_POTION("redPotion"),
  BRONZE_COIN("bronzeCoin"),
  MANA_CRYSTAL("manaCrystal"),
  BLUE_HEART("blueHeart"),

  // WEAPONS
  NORMAL_SWORD("normalSword"),
  AXE("axe"),
  PICKAXE("pickAxe"),

  // SHIELDS
  BLUE_SHIELD("blueShield"),
  WOOD_SHIELD("woodShield"),

  // Projectiles
  FIREBALL("fireball"),
  ROCK("rock"),

  // INTERACTIVE
  METAL_PLATE("metalPlate"),
  BIG_ROCK("bigRock");

  private final String textKey;

  GameObject(String textKey) {
    this.textKey = textKey;
  }
}
