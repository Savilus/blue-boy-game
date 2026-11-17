# Blue Boy


### Introduce
A simple 2D game written in Java. :video_game:

### Build

Install JDK 25 and configure your IDE's Gradle JVM to use it.
Build and run the tests using the Gradle wrapper:

```sh
./gradlew build
```

To run the full headless JUnit suite and measure line coverage on the same classpath:

```sh
./gradlew clean build jacocoTestReport
```

The HTML report is in `build/reports/jacoco/test/html/index.html`.

### Saved games and settings

The game stores mutable state in a per-user data directory, `~/.blue-boy/`
(`%USERPROFILE%\.blue-boy` on Windows). The location can be redirected for
portable installs or tests with the `-Dblueboy.dataDir=<directory>` system
property.

* `save.json` — versioned JSON save. Saving writes to a temporary file in the
  same directory and atomically replaces the previous save. If applying a
  loaded save fails, the game restores the state it had before the load and
  reports the failure.
* `config.properties` — full-screen flag and music/sound-effect volume. Written
  only when a value changes and replaced atomically; a missing or unreadable
  file keeps the defaults.

On first load, if no JSON save exists, the game attempts to import the old
`save.dat` from `~/.blue-boy/` (then from the process working directory) and
keeps the original file.
Legacy saves did not record the current map, player position, boss/event flags,
or collected authored drops, so those values cannot be recovered during import.
If an old serialized save is incompatible with the current `DataStorage` class,
loading fails and the original file remains untouched.
Regular monsters and NPCs, including movable rocks, respawn at their starting
positions on load; temporary boss-battle doors are not saved and an unfinished
boss encounter restarts.

### Development mode

Cheat/debug input (god mode, debug overlay, map reload, and path visualization)
is disabled by default. Set `dev.enabled: true` in
`src/main/resources/config.yml` to enable it. During play, **Ctrl** toggles
translucent red trails for **all** enemies currently tracking the player on
this map. Each trail shows that enemy’s remaining waypoints; a boss steering
directly shows its heading to the player instead. Wandering enemies, dead
enemies and NPCs have no trail. **R** reloads the terrain; the minimap,
full map and pursuit routes use the new layout.
Press **+** while playing to grant the player one level. New games start with
every inventory item; stackable items have 50 units each.

The game reads all
bundled texts, images, fonts, sounds and tile maps from the classpath, so it
runs from any working directory; `src/main/resources/texts/index.json` lists
the bundled text files and must list every file in that folder.

The game features such things as:
1) Character system: :runner:
    - character lvl and stats
    - life and mana
    - equipment and items
    - saving and loading progress
2) Interaction with the environment: :deciduous_tree:
    - talking to NPCs
    - destroying and moving objects
    - collecting items from chests
    - logic puzzles
    - interactive fields
3) Enemy and combat system: :hocho:
    - melee attack
    - blocking projectiles and attacks
    - parrying attacks
    - ranged attacks such as fireballs
    - boss battles
    - various types of enemies
    - main character pursuit system
    - mob drops
4) Shop system: :moneybag:
    - buy items
    - sell items
5) Day and night system :sunrise:
6) Different maps and minimap :earth_asia:
7) Sound effects and music :sound:
8) Main and in game menu with settings :wrench:

   


***

![projectile](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/projectile.png)
![interactive](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/interactive.png)
![characterStats](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/characterStats.png)
![options](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/options.png)
![shop](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/shop.png)
![minimap](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/minimap.png)
![nightAndDay](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/nightAndDay.png)
![dungeon](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/dungeon.png)
![bossfight](https://github.com/Savilus/blue-boy-game/blob/master/game%20ss/bossFight.png?raw=true)
