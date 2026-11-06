package org.savilusGame.service;

/**
 * Abstraction over background music and sound effect playback.
 * <p>Implementations must be usable without any Swing/AWT dependency so they can
 * be substituted with recording fakes in unit tests.
 */
public interface AudioService {

  /**
   * Play (or switch to) the background music identified by {@code soundId} and loop it.
   *
   * @param soundId the sound resource identifier
   */
  void playBGM(String soundId);

  /** Stop the currently playing background music, if any. */
  void stopBGM();

  /**
   * Play a one-shot sound effect identified by {@code soundId}.
   *
   * @param soundId the sound resource identifier
   */
  void playSE(String soundId);
}
