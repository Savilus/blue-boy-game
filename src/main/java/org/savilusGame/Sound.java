package org.savilusGame;

import static org.savilusGame.config.GameEntityNameFactory.BLOCKED;
import static org.savilusGame.config.GameEntityNameFactory.CHIP_WALL;
import static org.savilusGame.config.GameEntityNameFactory.COIN;
import static org.savilusGame.config.GameEntityNameFactory.CURSOR;
import static org.savilusGame.config.GameEntityNameFactory.DESTROY_TREE;
import static org.savilusGame.config.GameEntityNameFactory.DOOR_OPEN;
import static org.savilusGame.config.GameEntityNameFactory.DUNGEON_SONG;
import static org.savilusGame.config.GameEntityNameFactory.FANFARE;
import static org.savilusGame.config.GameEntityNameFactory.FINAL_BATTLE;
import static org.savilusGame.config.GameEntityNameFactory.FIREBALL_SOUND;
import static org.savilusGame.config.GameEntityNameFactory.GAME_OVER;
import static org.savilusGame.config.GameEntityNameFactory.HIT_MONSTER;
import static org.savilusGame.config.GameEntityNameFactory.LEVEL_UP;
import static org.savilusGame.config.GameEntityNameFactory.MERCHANT_SONG;
import static org.savilusGame.config.GameEntityNameFactory.OUTSIDE_MUSIC;
import static org.savilusGame.config.GameEntityNameFactory.PARRY;
import static org.savilusGame.config.GameEntityNameFactory.POWER_UP;
import static org.savilusGame.config.GameEntityNameFactory.RECEIVE_DAMAGE;
import static org.savilusGame.config.GameEntityNameFactory.SLEEP;
import static org.savilusGame.config.GameEntityNameFactory.SPEAK;
import static org.savilusGame.config.GameEntityNameFactory.STAIRS;
import static org.savilusGame.config.GameEntityNameFactory.SWING_WEAPON;
import static org.savilusGame.config.GameEntityNameFactory.UNLOCK;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;

import io.vavr.control.Try;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Sound {

  Clip clip;
  final Map<String, URL> soundMap = new HashMap<>(20);
  FloatControl floatControl;
  @Getter @Setter int volumeScale = 3;
  float volume;

  public Sound() {
    String[] sounds = {
        OUTSIDE_MUSIC, MERCHANT_SONG, DUNGEON_SONG, COIN, POWER_UP, UNLOCK,
        FANFARE, HIT_MONSTER, RECEIVE_DAMAGE, SWING_WEAPON, LEVEL_UP, CURSOR,
        FIREBALL_SOUND, DESTROY_TREE, GAME_OVER, STAIRS, SLEEP, BLOCKED,
        PARRY, SPEAK, CHIP_WALL, DOOR_OPEN, FINAL_BATTLE
    };

    for (String sound : sounds) {
      soundMap.put(sound, Objects.requireNonNull(getClass().getResource(sound)));
    }
  }

  protected void setFile(String sound) {
    Try.run(() -> {
      AudioInputStream audio = AudioSystem.getAudioInputStream(soundMap.get(sound));
      clip = AudioSystem.getClip();
      clip.open(audio);
      floatControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
      checkVolume();
    }).getOrElseThrow(e -> new IllegalArgumentException("Error during getting file: " + e.getMessage()));

  }

  protected void play() {
    clip.start();
  }

  protected void loop() {
    clip.loop(Clip.LOOP_CONTINUOUSLY);
  }

  protected void stop() {
    if (clip != null) {
      clip.stop();
    }
  }

  public void checkVolume() {
    switch (volumeScale) {
      case 0 -> volume = -80f;
      case 1 -> volume = -20f;
      case 2 -> volume = -12f;
      case 3 -> volume = -5f;
      case 4 -> volume = 1f;
      case 5 -> volume = 6f;
    }

    floatControl.setValue(volume);
  }
}
