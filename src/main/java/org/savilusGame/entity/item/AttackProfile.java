package org.savilusGame.entity.item;

/**
 * Attack geometry and timing of a melee weapon.
 * <p>Kept separate from the physics body: {@code width/height} form the swing rectangle,
 * {@code firstMotionDuration/secondMotionDuration} are the sprite tick durations of the two
 * attack frames.
 */
public record AttackProfile(int width, int height, int firstMotionDuration, int secondMotionDuration) {
}
