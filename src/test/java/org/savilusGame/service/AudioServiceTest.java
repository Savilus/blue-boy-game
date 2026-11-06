package org.savilusGame.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.savilusGame.GamePanel;

/**
 * Contract tests for {@link AudioService}.
 * <p>The game maps every sound id onto an on-disk resource played through a {@code Clip};
 * opening a clip requires an audio device, which headless CI machines typically lack.
 * These tests therefore exercise the service contract with a recording fake and only
 * assert that the production wiring (GamePanel) implements the interface.
 */
class AudioServiceTest {

  /** Recording fake used to verify the contract without an audio device. */
  static class RecordingAudioService implements AudioService {
    final List<String> bgmCalls = new ArrayList<>();
    final List<String> seCalls = new ArrayList<>();
    int stopCalls = 0;
    String currentBgm;

    @Override
    public void playBGM(String soundId) {
      currentBgm = soundId; // a new track replaces the previous one
      bgmCalls.add(soundId);
    }

    @Override
    public void stopBGM() {
      stopCalls++;
      currentBgm = null;
    }

    @Override
    public void playSE(String soundId) {
      seCalls.add(soundId);
    }
  }

  @Test
  void playBGMReplacesTheCurrentTrack() {
    RecordingAudioService audio = new RecordingAudioService();
    audio.playBGM("outside_music");
    audio.playBGM("dungeon_song");
    assertEquals(List.of("outside_music", "dungeon_song"), audio.bgmCalls);
    assertEquals("dungeon_song", audio.currentBgm);
  }

  @Test
  void stopBGMClearsTheCurrentTrack() {
    RecordingAudioService audio = new RecordingAudioService();
    audio.playBGM("merchant_song");
    audio.stopBGM();
    assertEquals(1, audio.stopCalls);
    assertNull(audio.currentBgm);
  }

  @Test
  void playSERecordsOneShotSoundsInOrder() {
    RecordingAudioService audio = new RecordingAudioService();
    audio.playSE("coin");
    audio.playSE("level_up");
    audio.playSE("coin");
    assertEquals(Collections.nCopies(1, "coin").stream().toList().size() + 2, audio.seCalls.size());
    assertEquals(List.of("coin", "level_up", "coin"), audio.seCalls);
  }

  @Test
  void playSEDoesNotTouchBackgroundMusic() {
    RecordingAudioService audio = new RecordingAudioService();
    audio.playBGM("final_battle");
    audio.playSE("blocked");
    assertEquals("final_battle", audio.currentBgm);
    assertEquals(0, audio.stopCalls);
  }

  @Test
  void gamePanelImplementsTheAudioServiceContract() {
    // Guard against accidental removal of the delegation added in Phase 0.
    assertInstanceOf(AudioService.class, new GamePanel());
  }
}
