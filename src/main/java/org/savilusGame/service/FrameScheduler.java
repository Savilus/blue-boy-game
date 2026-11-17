package org.savilusGame.service;

/**
 * Frame deadline arithmetic for the game loop, kept free of sleeping and drawing so the pacing can
 * be tested without a running game.
 * <p>Every frame pass uses one fixed-size slot: {@link #idleNanos(long)} says how long the loop may
 * wait before the next pass, and {@link #advance(long)} moves the deadline one slot ahead. A loop
 * that fell behind (a stall, a debugger breakpoint, a slow map load) restarts its deadline from the
 * current time instead of replaying the missed frames in a burst, so the simulation never catches up
 * with a burst of updates.
 * <p>Not thread-safe; used by the game loop thread only.
 */
public final class FrameScheduler {

  private static final long NANOS_PER_SECOND = 1_000_000_000L;

  private final long frameDurationNanos;
  private long nextFrameNanos;

  /**
   * @param targetFps frames per second the loop should run at; must be positive
   */
  public FrameScheduler(int targetFps) {
    this(targetFps, System.nanoTime());
  }

  FrameScheduler(int targetFps, long startNanos) {
    if (targetFps <= 0) {
      throw new IllegalArgumentException("targetFps must be positive, was " + targetFps);
    }
    this.frameDurationNanos = NANOS_PER_SECOND / targetFps;
    this.nextFrameNanos = startNanos;
  }

  /**
   * Nanoseconds the loop may wait before the next frame is due; {@code 0} means the frame is due
   * now and no waiting is allowed.
   */
  public long idleNanos(long nowNanos) {
    return Math.max(nextFrameNanos - nowNanos, 0L);
  }

  /**
   * Moves the deadline one frame ahead. When the loop is already past the new deadline, the
   * deadline restarts one frame from {@code nowNanos} instead of replaying the missed frames.
   */
  public void advance(long nowNanos) {
    nextFrameNanos += frameDurationNanos;
    if (nextFrameNanos < nowNanos) {
      nextFrameNanos = nowNanos + frameDurationNanos;
    }
  }

  /** Length of one frame slot in nanoseconds. */
  public long frameDurationNanos() {
    return frameDurationNanos;
  }
}
