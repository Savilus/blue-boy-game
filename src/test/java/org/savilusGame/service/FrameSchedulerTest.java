package org.savilusGame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The loop must wait between frames and must not replay frames it missed while stalled. */
class FrameSchedulerTest {

  private static final int FPS = 60;
  private static final long FRAME_NANOS = 1_000_000_000L / FPS;

  @Test
  @DisplayName("the first frame runs immediately")
  void firstFrameIsDueImmediately() {
    FrameScheduler scheduler = new FrameScheduler(FPS, 0L);

    assertEquals(0L, scheduler.idleNanos(0L));
  }

  @Test
  @DisplayName("a frame slot lasts 1/FPS seconds")
  void frameSlotLastsOneSixtiethOfASecond() {
    assertEquals(FRAME_NANOS, new FrameScheduler(FPS, 0L).frameDurationNanos());
  }

  @Test
  @DisplayName("the loop waits for the rest of the slot before the next frame")
  void theLoopWaitsForTheRestOfTheSlot() {
    FrameScheduler scheduler = new FrameScheduler(FPS, 0L);
    scheduler.advance(0L);

    assertEquals(FRAME_NANOS, scheduler.idleNanos(0L), "a full slot is left before the next frame");
    assertEquals(FRAME_NANOS / 2, scheduler.idleNanos(FRAME_NANOS / 2));
    assertEquals(1L, scheduler.idleNanos(FRAME_NANOS - 1), "one nanosecond is still left");
    assertEquals(0L, scheduler.idleNanos(FRAME_NANOS), "the frame is due, so waiting is over");
    assertEquals(0L, scheduler.idleNanos(2 * FRAME_NANOS), "a late frame never gets a negative wait");
  }

  @Test
  @DisplayName("a stall does not turn into a burst of catch-up frames")
  void aStalledLoopDoesNotBurst() {
    FrameScheduler scheduler = new FrameScheduler(FPS, 0L);

    scheduler.advance(4 * FRAME_NANOS);

    assertEquals(FRAME_NANOS, scheduler.idleNanos(4 * FRAME_NANOS),
        "the deadline restarts one frame ahead instead of replaying the four missed frames");
  }

  @Test
  @DisplayName("a slightly late frame keeps the original rhythm")
  void aSlightlyLateFrameKeepsTheRhythm() {
    FrameScheduler scheduler = new FrameScheduler(FPS, 0L);
    scheduler.advance(0L);

    scheduler.advance(FRAME_NANOS + 5);

    assertEquals(FRAME_NANOS - 5, scheduler.idleNanos(FRAME_NANOS + 5));
  }

  @Test
  @DisplayName("a non-positive frame rate is rejected")
  void nonPositiveFrameRateIsRejected() {
    assertThrows(IllegalArgumentException.class, () -> new FrameScheduler(0, 0L));
    assertThrows(IllegalArgumentException.class, () -> new FrameScheduler(-60, 0L));
  }
}
