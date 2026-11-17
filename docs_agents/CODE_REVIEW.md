# Code Review of current bugs to fix

## How to read this report

Each finding has:

- A position, a title and a status: `### 1. Title — 🔴 OPEN`
  or `### 1. Title — 🟢 FIXED`. Red/green indicators supplement the text;
  severity is independent of status and appears below the title when assigned.
- **Where** — file and line (historical review at `0511ef2`; selected findings
  reassessed against `20277f2` and the current worktree as noted below).
- **What / Why it matters** — the concrete failure mode or cost.
- **Suggested fix** — the approach I would take, biased toward the fact that this project will keep growing.

`OPEN` includes findings awaiting verification. Mark `FIXED` only with a
change reference, resolution and verification evidence as required by
`docs_agents/PROJECT_RULES.md`. After each completed iteration, move verified fixes
to the bottom FIXED list and renumber OPEN and FIXED separately from 1.

## OPEN

<a id="finding-20"></a>

### 1. `TileManager.draw()` tests 2,500 tiles per frame instead of computing the visible window — 🔴 OPEN
**Severity:** `P1`

**Where:** `tile/TileManager.java:135-185`

```java
for (int row = 0; row < worldRows; row++)
  for (int col = 0; col < worldCols; col++) { int tileNum = map[col][row]; ... visibility check ... }
```

The viewport covers ~20×12 tiles; 96% of iterations are discarded by the `visibleMin/Max` test. Compute `startCol/endCol/startRow/endRow` from `playerWorld - playerScreen - TILE_SIZE` and iterate only that window. Same for the world-map pre-render (`GameMap.createWorldMap` renders the whole world each time it is called — fine occasionally, but it's called on every map switch (`UI.drawTransition`) in addition to once per construction; cache per map id).

<a id="finding-21"></a>

### 2. Per-frame allocation churn across the update/draw paths — 🔴 OPEN
**Severity:** `P1`

**Where / examples:**
- `GamePanel.java:274` — `gameObjects.sort(Comparator.comparingInt(GameEntity::getWorldY))` and the list rebuild each frame (~300 `add` + boxing + lambda captures per frame).
- `EventHandler.java:82` — `new HashMap<>()` + 4 lambdas **every `checkEvent()` call** (i.e., every frame while `canTouchEvent`).
- `EventHandler.hit()` — allocates a `Rectangle` for the player area and reads from `eventRect` per call (5 events × 60 fps).
- `GameEntity.checkCollision()` — `new ArrayList<>(Optional.ofNullable(...).orElse(emptyList()))` every call, then again per attack frame (`attacking()` → `checkEntity(... streams ... collect(toList()))` — a Stream + List allocation **every attack frame**).
- `Player.update()` — the same `Optional.ofNullable(...).stream().map(t -> (GameEntity) t).collect(...)` pattern for interactive tiles.
- `checkCollision()` / `update()` — the interactive-tiles list is *the same list* each call; the copy exists only to upcast `InteractiveTile`→`GameEntity`.
- `Lighting.draw()` — `TextManager.getUiText(...)` map lookups and `deriveFont(50F)` every frame even though the end-credits branch is almost never taken; `String dayStateDebug` computed unconditionally.
- `UI.*` — `graphics2D.setFont(graphics2D.getFont().deriveFont(...))` in ~30 places, per frame per draw call — each `deriveFont` allocates a new `Font`.
- `new Random()` at 9 call sites (`GameEntity.java:350,642,653,664,673`, `Bat/GreenSlime/RedSlime:55/60/66`, `OldManNPC:79`) — new object + seeds from a shared entropy source per call; also means "random" is not seedable for deterministic tests.
- `Comparator.comparingInt(GameEntity::getWorldY)` allocates a comparator each frame (cache a `static final Comparator`).

**Why it matters:** at 60 fps with a busy scene (10 monsters × pathfinding + collisions + particles + projectiles), the young generation is filled with garbage objects that do nothing; GC pressure shows up as micro-stutters exactly when the action is dense. None of these are hard to fix; doing them as part of the render-loop work ([FIXED 19](#finding-19)) amortises the cost.

**Fix:** hoist constants, reuse scratch `Rectangle`s (or better, remove the rect mutation entirely — see [OPEN 4](#finding-23)), replace copy+upcast streams with a pre-built `List<GameEntity>` view of interactive tiles (maintain it alongside `mapsInteractiveTiles`), cache fonts and comparators, switch to `ThreadLocalRandom` (or an injected `Random`).

<a id="finding-22"></a>

### 3. `PathFinder` is re-run *every frame* for every chasing monster, and is O(n²) — 🔴 OPEN
**Severity:** `P1`

**Where:** `ai/PathFinder.java` (`setNodes`, `findBestNode`, `openList.remove(currentNode)`), callers `GameEntity.searchPath()`

Three compounding issues:

1. **Full 2,500-node rescan per search per monster per frame.** `setNodes()` recomputes `getCost()` for all 2,500 cells and re-marks destructible interactive tiles solid **every time a monster searches** — and `searchPath()` is called from monster AI each update tick while `onPath`. With 3 chasing monsters that's ~7,500 nodes of setup + A\* bookkeeping per frame, before any movement logic.
2. **`openList` is an `ArrayList`** — `findBestNode()` is a linear scan and `remove(currentNode)` is O(n). A `PriorityQueue<Node>` ordered by `fCost` makes both O(log n). (For a 50×50 grid the constant factors hide it, but the moment you support larger worlds or more agents, this is the first wall you hit.)
3. **One shared `PathFinder` instance** for all agents (`gamePanel.getPathFinder()`), holding mutable search state (`openList`, `currentNode`, `pathList`…). It is single-threaded today, but it forces every caller to serialise through one object and makes the class untestable and unsafe to re-enter. Also `search()` silently truncates at `MAX_PATH_LENGTH = 100` — the map is 50×50 (diagonal ≈ 99), so long searches *are* near the cap; hitting it returns "no path" with no signal.

**Fix:**
- Split out an immutable **search** (`PathFinder.search(grid, start, goal) : List<Node>`) from per-agent reuse; make it static/instance-per-call.
- Maintain the solid-grid incrementally (mark tiles solid only when a destructible tile is placed/destroyed; a 2,500-cell boolean grid rebuild is trivial).
- Only recompute the path when the goal cell changed since the last search, or every N ticks (e.g., 30) — classic stagger. Cache the path on the entity (`currentPath`, `pathIndex`).
- Use a `PriorityQueue<Node>` (or binary heap) and a `Deque<Node>` for the reconstructed path (`pathList.addFirst` on an `ArrayList` is O(n) per add → O(n²) path rebuild; a proper `Deque`/linked list is O(n)).
- Delete `MAX_PATH_LENGTH` or make it explicit and logged; failure to find a path should be distinguishable from "already at goal".
- `PathFinder.getPathList()` returns `ArrayList<Node>` and callers use `getFirst()` — a Java 21 `SequencedCollection` call (see [OPEN 9](#finding-28)). Return an unmodifiable `List<Node>` or `Deque<Node>`.

<a id="finding-23"></a>

### 4. Collision detection mutates shared `Rectangle`s as scratch space — 🔴 OPEN
**Severity:** `P2`

**Where:** `CollisionDetector` (`checkObject`, `checkEntity`, `checkPlayer`, `checkGameEntityCollision`)

Every check: (1) writes world coords into the *entity's* `solidArea.x/y`, (2) writes into the *target's* `solidArea.x/y`, (3) optionally `y -= speed` on the entity's rect, (4) intersects, (5) restores the defaults. If anything throws between (1) and (5) — an NPE from a missing tile (`tile[num]` null), an AIOOBE, a bug in a future `checkX` — the entity's `solidArea` is left in **world coordinates** and every subsequent collision is nonsense, with no log line explaining why. It also mutates the *attacker's* rect during knockback using the caller's direction — subtle, and it forces the ugly "use knockBackDirection instead" branch in four places.

**Fix:** compute collision rectangles **locally** (`Rectangle entityRect = new Rectangle(x, y, w, h)` — a 4-field allocation is far cheaper than corruption, or reuse a per-call scratch rect), never write into `gameEntity.getSolidArea()`. The default-x/y restore dance disappears and `solidAreaDefaultX/Y` become unnecessary. This also makes the detector stateless and unit-testable.

<a id="finding-24"></a>

### 5. Resource loading has no cache: every entity construction re-reads and re-scales PNGs — 🔴 OPEN
**Severity:** `P2`

**Where:** `GameEntity.setup()` (ImageIO.read + `UtilityTool.scaleImage`), called from every constructor; `AssetSetter.setMonster()` on **every area change** and `resetGame()`; `UI` constructs `Heart`/`ManaCrystal`/`BronzeCoin` entities purely to steal `mainImage`; `Player.canObtainItem()` constructs a **new entity from disk** to read `stackable`; `SaveLoad.load()` constructs every inventory item and map object from disk

Each `setup()` does `ImageIO.read` (file open + PNG decode) then allocates a scaled `BufferedImage`. The same `sun.png` is decoded dozens of times per session. Map transitions hitch visibly; opening the inventory panel stutters; loading a save re-decodes hundreds of images serially on the UI thread.

**Fix:** a tiny **sprite cache** (Flyweight): `SpriteCache.get(path, width, height)` with a bounded map keyed by `path|width|height`. Combined with a static item-definition table (`ItemCatalog`: id → stackable/price/description key), `canObtainItem` and the UI HUD stop constructing entities entirely. This single change usually removes the largest per-frame and per-action disk traffic in Swing games.

<a id="finding-25"></a>

### 6. `drawToScreen()` uses `getGraphics()` instead of Swing's paint path — 🔴 OPEN
**Severity:** `P2`

**Where:** `GamePanel.java:207-212`

Calling `getGraphics()` per frame and then `dispose()` bypasses Swing's repaint machinery: it can tear on some platforms, it allocates a new `Graphics` per frame, and it ignores the component lifecycle (no paint coalescing, no correct behaviour when the OS asks for a repaint — e.g., window un-minimize shows black until the next tick). The double-buffer via `tempScreen` is right; the final blit should happen in `paintComponent`.

**Fix:** `@Override protected void paintComponent(Graphics g)` → `g.drawImage(tempScreen, 0, 0, screenWidthFull, screenHeightFull, null)`; call `repaint()` at the end of frame draw (or keep the active-rendering shape but only via `paintComponent`), and drop `getGraphics()`.

<a id="finding-26"></a>

### 7. Sorting and drawing contain scattered magic numbers — 🔴 OPEN
**Severity:** `P3`

`GamePanel.ORIGINAL_TITLE_SIZE` (typo, "TITLE" not "TILE"), `TILE_SIZE * 5` fudge in `inCamera()`, `KNOCK_BACK_TIME`/`SPRITE_CHANGE_TIME` scattered, `20` duplicated as inventory cap, `3000` day-length, `0.001F` filter ramp… Give them names and one home (`Constants` or the owning class) — a future tuning pass over "game feel" currently means grepping literals.

<a id="finding-27"></a>

### 8. `Lighting` re-renders the darkness filter only when told, but draws text lookups every frame — 🔴 OPEN
**Severity:** `P3`

`setLightSource()` correctly only runs on `lightUpdated` — good. But `draw()` does two `TextManager` map lookups + a `deriveFont(50F)` per frame for a debug string almost never shown (end-credits only). Guard the whole block behind the ending-scene condition and hoist the font.

<a id="finding-28"></a>

### 9. Lombok plugin version vs Gradle 8.5, and no Java toolchain pinned — 🔴 OPEN
**Severity:** `P1`

**Where:** `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`

- `io.freefair.lombok` **5.3.0** (Nov 2020, built for Gradle 6.x) paired with Gradle **8.5** is asking for trouble; you also redundantly declare `compileOnly`/`annotationProcessor` lombok yourself (good), so the plugin adds risk with little gain. **Drop `io.freefair.lombok`** or upgrade to ≥ 8.x.
- **No `java { toolchain { languageVersion = ... } }`** anywhere. The code uses `List.getFirst()` (`GameEntity.searchPath`, `GameMap`? and `EventHandler.handleInteriorMapEvents` `getFirst()`) — a **Java 21** `SequencedCollection` API. With no toolchain, the build compiles against whatever JDK the CI/dev box happens to have; on JDK 17 it fails, on JDK 21 it silently relies on unspecified defaults (source/target 21 with `-source`/`-target` mismatch warnings). Pin `languageVersion = JavaLanguageVersion.of(21)` and set `options.encoding = "UTF-8"`.
- `group = "org.example"`, `version = "1.0-SNAPSHOT"` — rename to the real coordinates (`org.savilusgame:blue-boy:<version>`) and start versioning properly; `save.dat`-style artifacts make releases untrackable.
- `tasks.test { useJUnitPlatform() }` is configured but there are **zero tests** (see [OPEN 45](#finding-64)) — the line is aspirational today.

<a id="finding-29"></a>

### 10. Outdated/vulnerable dependencies — 🔴 OPEN
**Severity:** `P1`

| Dependency | Updated version | Resolution |
|---|---|---|
| `org.apache.commons:commons-lang3` | 3.22.0 | Replaces 3.0; includes the fix for [CVE-2025-48924](https://github.com/advisories/GHSA-j288-q9x7-2f5v). |
| `com.fasterxml.jackson.core:jackson-databind` | 2.21.8 via Jackson BOM | Replaces 2.15.0 and aligns core/annotations; includes fixes for [CVE-2026-59888](https://github.com/advisories/GHSA-3pjw-73gf-8qr5), [CVE-2026-54515](https://github.com/advisories/GHSA-5jmj-h7xm-6q6v) and [CVE-2026-110879](https://github.com/FasterXML/jackson-databind/security/advisories/GHSA-m8xf-2cr7-77ph). |
| `org.yaml:snakeyaml` | 2.7 | Updated within the 2.x API line. |
| `org.slf4j:slf4j-api` / `ch.qos.logback:logback-classic` | 2.0.20 / 1.6.7 | Replaces Logback 1.4.12 and its vulnerable core; see [CVE-2024-12801](https://github.com/advisories/GHSA-6v67-2wr5-gvf4) and [release notes](https://logback.qos.ch/news.html). |
| `io.vavr:vavr` | 0.10.7 | Patch update without adopting the 1.x API. |
| `org.projectlombok:lombok` | 1.18.48 | Same version for compile-only API and annotation processor; supports JDK 25. |
| `org.junit:junit-bom` | 5.14.4 | Aligns Jupiter and Platform while keeping JUnit 5. |

**Partial resolution (2026-11-17):** dependency declarations updated in
`build.gradle.kts`. Maven Central POM availability and selected coordinates
were checked using Python `urllib.request`; OSV was queried through
`POST https://api.osv.dev/v1/querybatch`. No known advisory was returned for
the initially checked replacement versions. The developer subsequently reported
CVE-2026-110879 affecting Databind 2.21.7, missed by that OSV check.
The maintainer advisory confirms 2.21.8 as a fixed release; the BOM was
updated accordingly, with the published BOM mapping and artifact availability
checked against Maven Central. This is a point-in-time database check,
not a guarantee of vulnerability-free software or a scan of a Gradle-resolved
dependency graph. `java -version` failed with "Unable to locate a Java Runtime",
so compilation, tests and runtime compatibility remain unverified. Keep this
finding OPEN until the full build and resolved dependency graph are checked.

Also: **no `logback.xml`** on the classpath — logback's default configuration prints **DEBUG** and above to stdout (logback default). Every `log.debug` you ever add will spam players' consoles. Add `src/main/resources/logback.xml` (console pattern + rolling file under `user.home/.blue-boy/logs`, level INFO).

<a id="finding-30"></a>

### 11. No packaging: `java -jar` is impossible — 🔴 OPEN
**Severity:** `P2`

No `application`/`java` main-class manifest, no `Main-Class` in any jar task, no distribution (`installDist`, jpackage image, or even a fat jar). Given [FIXED 12](#finding-12), fixing resources is step one; packaging is step two. With Gradle, the `application` plugin + `installDist` + a `jpackage` step (or a simple launcher script) gets you a distributable in an afternoon.

<a id="finding-31"></a>

### 12. No static analysis / formatting enforcement — 🔴 OPEN
**Severity:** `P3`

`src/main/resources/code-style-formatting.xml` is an IDE config **inside the resources tree** (it gets copied into the jar). Checkstyle/SpotBugs/Error Prone are not wired into the build, so nothing stops regression of the very issues in this report. Add `checkstyle` (or Spotless) with that config moved to `config/checkstyle/`, run it in CI, and start with "warning only" to avoid a big-bang failure.

<a id="finding-32"></a>

### 13. The game thread dies silently on any exception; the window stays up frozen — 🔴 OPEN
**Severity:** `P0`

**Where:** `GamePanel.run()` — no `try/catch` around `update()`/`drawToTempScreen()`

Every P0/P1 bug above (missing map key → NPE, corrupt save, null tile…) propagates out of `run()`; the thread dies; the `JFrame` remains visible with its last frame painted. The player sees a frozen game, nothing in the console, nothing in a log (there is no logger on this path), and no way to report anything. 

**Fix:** wrap the loop body in `try { ... } catch (Throwable t) { log.error("Game loop crashed", t); showCrashDialog(t); break; }` and always call `setGameState(...)`-safe cleanup. Additionally, `while (Objects.nonNull(gameThread))` never terminates on its own — introduce a `running` flag, an interrupt path, and a shutdown hook (`window.addWindowListener(EXIT_ON_CLOSE → dispose everything)`, stop music/lines, flush config/save). Also funnel the whole "world frame" through the try so a single bad frame doesn't kill the session.

<a id="finding-33"></a>

### 14. Cross-thread visibility of game state (EDT vs game thread) — 🔴 OPEN
**Severity:** `P1`

**Where:** `utils/KeyHandler.java` (plain `boolean upPressed, …, enterPressed`), `GamePanel.gameState` (plain field, set from EDT, read from game thread), `TileManager.CURRENT_MAP` (`public static String`, mutated from game thread and read from EDT — e.g. `KeyHandler` map-reload, `UI.drawTransition`), `GameMap`/`UI` shared fields

Swing delivers key events on the **EDT**; the game loop reads the same fields from its own **game thread** with no `volatile`/synchronization. On x86 this usually appears to work; it is still a data race — updates can be cached, reordered, or lost, and any "worked on my machine" report in this area will be miserable to diagnose. `gameState` can also be half-observed (e.g., state switched to `OPTIONS_STATE` mid-iteration).

**Fix:** make the hand-off explicit and minimal — mark input flags `volatile` (they're single-writer/single-reader booleans), or better, push **input events** into a queue drained by the game thread; mark `gameState`, `CURRENT_MAP` as `volatile` or guard with the frame lock. Longer term this folds into the input-system work in [FIXED 14](#finding-14).

<a id="finding-34"></a>

### 15. `Sound`: clips leak native lines; one shared instance cannot overlap SFX — 🔴 OPEN
**Severity:** `P1`

**Where:** `Sound.java` (`setFile`, `play`, `loop`, `stop`), `GamePanel.soundEffect`

- `setFile()` does `AudioSystem.getClip(); clip.open()` **per call**. `playSoundEffect(...)` reuses one `Sound` instance; the previous `clip` reference is overwritten without `stop()`/`close()`. The old clip keeps its mixer line until GC finalizes it (and `Clip` holds native resources that are notoriously slow to finalize). Play enough sounds and you get `LineUnavailableException` / silent SFX — a classic "game loses sound after 20 minutes" bug.
- `stop()` (`clip.stop()`) NPEs if nothing was ever played (`clip == null`); `music.stop()` is called in `changeArea()` before any music may have been set in some flows.
- `checkVolume()` has no `default` in the `switch` — an out-of-range `volumeScale` leaves `floatControl.setValue(0f)` (0 dB = full scale) or, worse, an `IllegalArgumentException` if a corrupt config wrote a crazy value.
- The `FloatControl` cast assumes every mixer supports `MASTER_GAIN` (most do; some don't — the `Try` in `setFile` wraps it, but the error message is opaque).
- WAV assets are uncompressed PCM (41 MB for 4 tracks, 9–11 MB each) — fine for 15 s SFX, wrong for looped music (see [OPEN 36](#finding-55)).

**Fix:** cache one open `Clip` **per sound id** (`Map<SoundId, Clip>`), rewind with `setMicrosecondPosition(0)`, and `close()` music clips on switch. Use a small pool (or a dedicated `SoundEffect` player that opens-plays-closes per shot, since short clips decode fast). Null-guard `stop()`. Clamp `volumeScale` 0–5 at the setter. Convert WAV music to OGG/MP3 (`javax.sound` won't read OGG — either ship shorter/compressed WAV, use SPI (e.g., `vorbisspi`), or switch playback to `javax.sound` with MP3 via SPI — simplest: keep WAVs for SFX, transcode music to smaller sample rate/bitrate WAV or accept the SPI dependency).

<a id="finding-35"></a>

### 16. `draw()` mutates game state; render and update are not cleanly separated — 🔴 OPEN
**Severity:** `P1`

**Where:** `GamePanel.drawToTempScreen` (fine), but also:
- `GameEntity.draw()` → `dyingAnimation()` increments `dyingCounter` and flips `alive = false` — death progresses inside rendering, so a paused or skipped frame never finishes a death, and a slow frame advances death twice.
- `UI.drawOptionsScreen()` / `drawTradeScreen()` call `gamePanel.getKeyHandler().setEnterPressed(false)` — input consumption inside render.
- `UI.drawTransition()` switches `CURRENT_MAP` (a static!), teleports the player and creates a new world map — inside `draw`.
- `CutsceneManager.draw()` and `UI.drawSleepScreen()` advance counters and force `setGameState(PLAY_STATE)` from the render path.

**Fix:** move all counter/state advancement into `update()`. Keep `draw*` strictly side-effect-free (read state, paint). This is the same separation you already practice with `update()` vs `drawToTempScreen()` — extend it consistently. It pays off immediately (correct pause behaviour) and is a prerequisite for any future rendering on a separate thread.

<a id="finding-36"></a>

### 17. Knockback/invincibility timers and `invincibleCounter` duplicated with different constants — 🔴 OPEN
**Severity:** `P2`

`GameEntity.INVINCIBLE_TIME = 40` vs `Player.INVINCIBLE_TIME = 60`; `GameEntity.KNOCK_BACK_TIME = 5` vs inline `5` in `Player`; `SPRITE_CHANGE_TIME` vs `CHANGE_SPRITE_INTERVAL`. Two parallel implementations of the same mechanics (see [FIXED 15](#finding-15)) — same class of drift will hit any future status effect. **Fix:** single implementation, single constants table (or per-entity overridable fields with one code path).

<a id="finding-37"></a>

### 18. `getDetected()` uses the *player's* speed for any user entity — 🔴 OPEN
**Severity:** `P2`

**Where:** `GameEntity.getDetected` — `int playerSpeed = getPlayer().speed;`

Used by `Key.use()` with `gameEntity == player` today, but any future non-player caller (e.g., an NPC-chave or a spell) silently checks the wrong reach. **Fix:** `user.getSpeed()`.

<a id="finding-38"></a>

### 19. `INIT_INDEX = 999` sentinel — 🔴 OPEN
**Severity:** `P3`

Magic-number "not found" across 22 call sites. Use `-1` (unambiguous) or `OptionalInt`; fail loudly elsewhere (`if (idx == -1)`). Sentinels invite "forgot to check" bugs — [FIXED 6](#finding-6) is exactly that.

<a id="finding-39"></a>

### 20. `Objects.requireNonNull(getClass().getResourceAsStream(...))` inside `Try`, then `.getOrElseThrow(...)` — 🔴 OPEN
**Severity:** `P2`

`Sound`/`setup()` convert a *missing resource* (a build/packaging error) into a runtime exception with a decent message — that part is fine — but the same pattern in `YamlConfigLoader` (`RuntimeException("Config file not found...")`) bypasses the `Try` entirely and gives an inconsistent error model. Pick one strategy (unchecked `IllegalStateException` with the resource id, always with a message) and apply it uniformly; swallow-and-log is only acceptable for *optional* assets.

<a id="finding-40"></a>

### 21. `Window setUndecorated(true)` for fullscreen after `setResizable(false)` etc. — 🔴 OPEN
**Severity:** `P3`

Ordering in `Main.java` is okay-ish, but the fullscreen path is untestable and undecorated-only; consider `GraphicsDevice.setFullScreenWindow` (exclusive mode) or simply maximized-bordered; at minimum document why undecorated is required. Also `Main.window` is a **public static mutable** — pass the frame explicitly or keep a package-private holder.

<a id="finding-41"></a>

### 22. No save versioning; no migration path — 🔴 OPEN
**Severity:** `P1`

First field of the new save DTO must be `schemaVersion`. Even if you only ever ship one version, writing it now costs nothing and it future-proofs save-format fixes such as [FIXED 7](#finding-7) and [FIXED 10](#finding-8).

<a id="finding-42"></a>

### 23. Save writes only on two implicit triggers — 🔴 OPEN
**Severity:** `P1`

The game auto-saves at the healing pool (`EventHandler.healingPool()` → `setMonster()` + `save()`) and nowhere else (no save on quit/game-over retry). The title-screen "Load Game" entry point exists (`KeyHandler.titleState`) — with no save file, `load()` logs an error, then starts `PLAY_STATE` anyway (misleading UX: "Load Game" silently begins a fresh run instead of telling the player "no save found").

**Fix:** explicit save slot(s) (even a single `save.json`), save-on-quit prompt, "Load" disabled/greyed when no save exists, and an in-game "saved" toast so the player knows the healing-pool save happened.

<a id="finding-43"></a>

### 24. Interactive-tile save is index-based — 🔴 OPEN
**Severity:** `P2`

`SaveLoad` persists tile state as *parallel lists* aligned to the current `AssetSetter.setInteractiveTiles()` order. Reordering the list in code (trivial when adding a new destructible) corrupts every existing save silently — tiles resurrect or vanish with no error. **Fix:** persist by stable key: `mapId → list of {tileTypeId, col, row, currentLife, destroyed, invincible}` and rebuild by looking cells up, skipping unknown ids with a warning.

<a id="finding-44"></a>

### 25. Objects-on-map save is also index-based, and half-implemented — 🔴 OPEN
**Severity:** `P2`

`load()` clears the map list and re-adds only non-null entries; `NA`/`null` holes are simply dropped (the `objectsOnMap.add(null)` line is commented out). That's actually consistent today (lists are horizontal-compacted), but it still: (a) only covers the current map ([FIXED 7](#finding-7)), (b) assumes saved indexes match the freshly-`clear()`ed list, (c) loses chest *contents* for chests emptied this session (only `opened` is stored — fine, by design), and (d) doesn't store monster respawn state (acceptable design — respawning is a feature — but document it).

<a id="finding-45"></a>

### 26. `save.dat` and `gameConfig.txt` are committed to git — 🔴 OPEN
**Severity:** `P3`

See [OPEN 36](#finding-55).

<a id="finding-46"></a>

### 27. `GamePanel` is a god object; `GameEntity` is a god base class — 🔴 OPEN
**Severity:** `P1`

**Where:** `GamePanel.java` (entire), `entity/GameEntity.java` (759 lines, ~60 fields)

`GamePanel` simultaneously is: Swing component, game-loop owner, renderer, world state (`mapsObjects`, `mapsMonsters`, …), façade for every subsystem (`getPathFinder`, `getCollisionDetector`, `getEventHandler`, …), and the dependency everyone takes in their constructor. Every entity reaches the world **only** through it — that's 60+ call sites of `gamePanel.getX().getY()` and makes the engine untestable without a fully wired panel (impossible to unit test `Player` or `SkeletonLord` with a stub today).

`GameEntity` mixes: position/velocity, sprite sheets, attack animation state, parry windows, knockback, invincibility, inventory (`ArrayList<GameEntity> inventory` — every slime carries an `ArrayList`), mana/ammo/exp/level, dialogue state, loot, light radius, boss flags, `worldX/worldY`, a `type` enum, plus methods from `draw()` to `damagePlayer()` to `generateParticle()`. Items, projectiles, NPCs, tiles-as-entities and the player are all one class hierarchy with protected fields. This is the "Refs tutorial" shape — fine to *learn* with, expensive to *extend*.

**Fix (incremental, no big bang):**
1. Introduce a **`World`/`GameSession`** service object owned by `GamePanel` that owns the map-scoped entity lists, `CURRENT_MAP`, area, and boss flags — with methods (`entitiesOn(map)`, `spawnMonster(...)`, `dropLoot(...)`) instead of public mutable maps. `GamePanel` keeps the Swing/loop responsibilities only.
2. Split `GameEntity` along the axis it already secretly has: an **`Item`** family (stackable, price, description — no physics, no AI) and an **`Actor`** family (health, stats, movement, collision). Interactive tiles implement `Destructible`/`Interactable` interfaces rather than inheriting 60 fields. Keep the single `GameEntity` base *temporarily* and migrate one family at a time; the goal is that `new RedPotion()` doesn't drag in `attackArea`, `exp`, `knockBackPower`.
3. Constructor-inject **interfaces** (`AudioService`, `TextRepository`, `ItemCatalog`) rather than `GamePanel`. Even one interface (e.g., `EffectPlayer`) enables headless unit tests and breaks the worst coupling.
4. `TileManager` singleton (`getInstance`) → constructor injection like everything else; delete `public static String CURRENT_MAP` (see [OPEN 28](#finding-47)).

Do not attempt a full ECS rewrite — it's unjustified at this scale. The point is: **make the seams**; the game stays the same, the tests and the team start to flow.

<a id="finding-47"></a>

### 28. Global mutable state: `TileManager.CURRENT_MAP` (public static), `Main.window` (public static), `Progress` (public static boolean) — 🔴 OPEN
**Severity:** `P1`

**Where:** `tile/TileManager.java:56`, `Main.java:14`, `data/Progress.java`

`CURRENT_MAP` is read and written from `GamePanel`, `TileManager.draw`, `UI.drawTransition`, `CollisionDetector`, `EventHandler`, `PathFinder`, `GameEntity`, `AssetSetter`, `SaveLoad`, `KeyHandler` — a dozen classes reaching into one static cell. It prevents: multiple worlds, map previews, cutscene maps (PlayerDummy already teleports!), replay/testing, and safe TDD. These are all things this project *will* want.

**Fix:** pass the current map id through the world object ([OPEN 27](#finding-46)); `CURRENT_MAP` becomes `world.currentMapId()`. Delete `public static`. `Main.window` → inject into `GamePanel` (fullscreen needs screen size, not the window). `Progress` → save DTO ([FIXED 9](#finding-10)).

<a id="finding-48"></a>

### 29. Collections of entities are open to every class, null-keyed by map id — 🔴 OPEN
**Severity:** `P2`

`mapsObjects`/`mapsMonsters`/`mapsNpc`/`projectiles`/`mapsInteractiveTiles` are public fields (via `@Getter`) typed `Map<String, List<GameEntity>>`, populated piecemeal by `AssetSetter` with **inconsistent key coverage** (no INTERIOR_MAP for objects/monsters, no SECOND_FLOR for NPCs…), then null-checked at every call site with subtly different patterns. This is the structural cause of [FIXED 6](#finding-6). **Fix:** one registry class (`WorldRegistry`) built from an explicit `List<String> mapIds`, always fully populated; absence is impossible by construction — not by discipline.

<a id="finding-49"></a>

### 30. The `UI` class is a 1,067-line renderer+controller for ~12 screens — 🔴 OPEN
**Severity:** `P2`

Title, HUD, pause, character sheet, inventory (player & NPC), trade (buy/sell/select), options (settings/fullscreen/controls/end-game), dialogue, sleep transition, game-over, map, transition, debug — all in one class with a `Graphics2D graphics2D` field stashed on each `draw()`. Split into **screen classes** implementing e.g. `interface Screen { void draw(Graphics2D g, GamePanel p); }` with a `UiState`/`ScreenManager`; share `drawSubWindow`, fonts, and `getXForCenteredText` via a `UiKit`. This removes merge-conflict hell and makes each screen independently testable (at least construction-tested) — and it pairs naturally with replacing `GameState` switches scattered across `KeyHandler`/`UI`/`GamePanel` with a state-handler map (keep the enum; dispatch table instead of 10 switches).

<a id="finding-50"></a>

### 31. Assets are placed in Java code — 🔴 OPEN
**Severity:** `P2`

`AssetSetter` hard-codes ~80 entity placements (coordinates, loot chains) as `new X(gamePanel).setWorldPosition(TILE_SIZE * 25, TILE_SIZE * 23)` lines; `Lighting`, `EnvironmentManager`, day length… World design (spawn points, trigger volumes, puzzle wiring) buried in code means every level tweak is a recompile and review. For a game that will grow, move **entity placement and events into data** (JSON/YAML alongside the map files — e.g., `maps/worldV3.entities.json`: `{type, x, y, loot, …}`), keep a thin loader. This also fixes [OPEN 29](#finding-48) partially (placement defines the complete map set) and makes the a designer/mods workflow possible.

<a id="finding-51"></a>

### 32. Duplicated A\* demo package ships with the game — 🔴 OPEN
**Severity:** `P2`

**Where:** `src/main/java/aStarPathfindingAlgorithmExample/` (`Main`, `DemoPanel`, `KeyHandler`, `Node`)

A standalone A\* teaching demo, duplicating `org.savilusGame.ai.Node`, living in the production source set (compiled into the game jar, warns in every build, confuses new devs about what's real). **Fix:** delete it, or move to `examples/` as a separate source set / separate module with its own run config. Same for `maps/map01.txt`, `maps/world01.txt`, `maps/worldV2.txt` (unreferenced — `MAIN_MAP` is `/maps/worldV3.txt`), `objects/chest (OLD).png`, and the unused `world.tile.images.sand` YAML key (points at a non-existent `/tiles/sand.png` — 204 keys check found exactly one missing path, this one; please fix the file or delete the key).

<a id="finding-52"></a>

### 33. Package naming and structure — 🔴 OPEN
**Severity:** `P3`

`org.savilusGame` — packages should be lowercase (`org.savilusgame`); the split `aStarPathfindingAlgorithmExample` (default demo) vs `org.savilusGame.ai` shows the ad-hoc growth. Suggested target layout (packages only, move incrementally):

```
org.savilusgame/
  engine/       (loop, rendering, input, assets, audio)
  world/        (map, tiles, collision, pathfinding, events)
  entity/       (actor, item, projectile, npc, monster)
  ui/           (screens, kit, hud)
  data/         (save, settings, text, localization)
  game/         (GamePanel, wiring, config of THIS game)
```

<a id="finding-53"></a>

### 34. Concrete design-pattern notes (where patterns help, where they don't) — 🔴 OPEN
**Severity:** `P3`

- **Flyweight / cache**: sprites & fonts ([OPEN 5](#finding-24), [OPEN 2](#finding-21)).
- **Factory / registry**: `GameEntityFactory` is the right idea — make it data-driven (`Map<GameObject, Supplier<…>>` built once — it already is), keyed by stable id ([FIXED 8](#finding-9)), with duplicate-mapping validation at startup (it would have caught [FIXED 5](#finding-5)).
- **State pattern**: `GameState` + switches in 4 places → dispatch table or State objects when a third switch appears; today a table suffices.
- **Observer**: `EventHandler`/`checkEvent` is a poor-man's observer; a tiny `EventBus` (or direct calls) is fine at this scale — don't over-engineer.
- **Strategy**: monster AI (`setAction()` overrides) is already strategy-shaped; keep it.
- Avoid: full ECS, DI frameworks — the constructor wiring is fine; a hand-rolled `World`/`Services` record is enough.

<a id="finding-54"></a>

### 35. `save.dat` and `src/main/gameConfig.txt` are committed to the repo — 🔴 OPEN

At the reviewed revision, `git ls-files` confirmed both runtime/user artifacts were tracked. Remove them from tracking (`git rm --cached`) and add them to `.gitignore`. Their historical copies remain in Git history; acceptable for a save file, but note it.

<a id="finding-55"></a>

### 36. Large uncompressed audio and binary assets — 🔴 OPEN

41 MB of uncompressed WAV music (9–11 MB × 4) and 250+ PNGs — the repo is ~84 MB. Use Git LFS for binary assets, or (better long-term) move assets out of git to an artifact host and fetch at build time (`gradle` task). At minimum convert music to OGG + a compact decoder SPI, or lower the sample rate/bit depth — 44.1 kHz stereo PCM is wasteful for this soundtrack.

<a id="finding-56"></a>

### 37. Screenshot directory name contains a space — 🔴 OPEN

`game ss/` is referenced from README. Rename it to `` and update README links.

<a id="finding-57"></a>

### 38. `code-style-formatting.xml` lives in `src/main/resources` — 🔴 OPEN

Move the formatting configuration to `config/`.

<a id="finding-58"></a>

### 39. README is minimal — 🔴 OPEN

At the reviewed revision: no requirements (JDK 21), no build instructions, no controls list, no save location, no architecture overview, no contribution/license section. Recheck current coverage before adding missing documentation; document `./gradlew run` only after an application run task exists.

<a id="finding-59"></a>

### 40. No LICENSE file — 🔴 OPEN

Choose an appropriate license before public distribution or commercial release.

<a id="finding-60"></a>

### 41. No `.gitattributes` — 🔴 OPEN

Binary files (png/wav) have no merge strategy hints (`*.png binary`).

<a id="finding-61"></a>

### 42. Runtime artifacts are not ignored — 🔴 OPEN

`.gitignore` doesn't ignore `save.dat` / `gameConfig.txt` / crash dumps / logs.

<a id="finding-62"></a>

### 43. Lombok-generated code needs tooling metadata — 🔴 OPEN

`lombok.config` at root with `config.stopBubbling=true` — fine; add `lombok.addLombokGeneratedAnnotation = true` so coverage/linters behave later.

<a id="finding-63"></a>

### 44. Review and CI merge requirements are not established — 🔴 OPEN

Single squashed-looking history ("Create README.md") — fine for now; adopt PR-based flow with CI green requirement once CI exists ([OPEN 45](#finding-64)).

<a id="finding-64"></a>

### 45. Tests — 🔴 OPEN

JUnit tests now exist under `src/test`; the full suite passed with 87 tests on 2026-11-17. This finding remains open because line coverage has not been measured against the project's ≥85% target and additional seams still need focused tests, including `PathFinder` and collision behavior. The specific regressions now covered are recorded with their corresponding fixed findings.

<a id="finding-65"></a>

### 46. CI — 🔴 OPEN

GitHub Actions: `./gradlew build test` on push/PR, matrix JDK 21, plus checkstyle/spotless gate (warning-only initially) and a `save format` compat test.

<a id="finding-66"></a>

### 47. Packaging/distribution — 🔴 OPEN

`application` plugin or `jpackage` (win/lin/mac), icons, desktop entry; version injection into the title bar (`Main.GAME_TITLE` + version).

<a id="finding-67"></a>

### 48. Crash reporting / logging — 🔴 OPEN

`logback.xml`, rolling file log in `user.home/.blue-boy/logs`, and the crash dialog from [OPEN 13](#finding-32); without this, every player report is unreproducible.

<a id="finding-68"></a>

### 49. Settings validation — 🔴 OPEN

`Config` line-based format has no schema; corrupt values silently half-apply. Use `java.util.Properties` or JSON; validate range (volume 0–5, fullscreen bool); keep human-editable only if you document it.

<a id="finding-69"></a>

### 50. Input remapping & controller? — 🔴 OPEN

optional, but the Key Bindings refactor ([FIXED 14](#finding-14)) is the enabling step; don't build remapping on a raw `KeyListener`.

<a id="finding-70"></a>

### 51. Save UX — 🔴 OPEN

save slots or at least: explicit save on quit, "Load" disabled without a save, "saved" toast ([OPEN 23](#finding-42)).

<a id="finding-71"></a>

### 52. Localization framework — 🔴 OPEN

beyond keyed JSON you have today (the shape is right); formalize key namespacing (`ui.*`, `items.*`), validate at startup in all shipped languages, and keep ids out of display strings ([FIXED 8](#finding-9)).

<a id="finding-72"></a>

### 53. Accessibility/UX basics — 🔴 OPEN

remappable keys, volume 0 = mute semantics (currently 0 → −80 dB — fine), text speed toggle for dialogue, colourblind-safe heart/mana palette (hearts vs crystals are red/cyan — mostly OK).

<a id="finding-73"></a>

### 54. Documentation — 🔴 OPEN

Architecture README (the map in [OPEN 33](#finding-52)), build/run docs, asset pipeline note, save-format doc (even one page), CONTRIBUTING.

<a id="finding-74"></a>

### 55. License, issue templates, PR checklist — 🔴 OPEN

cheap, immediate.

<a id="finding-75"></a>

### 56. Artifact cleanup — 🔴 OPEN

delete/move the A\* demo ([OPEN 32](#finding-51)), prune unused maps/sprites ([OPEN 32](#finding-51)), document the tile-id ↔ image contract (`tile[]` index ≥ 10 — magic 10 at `TileManager.setUp`).

<a id="finding-76"></a>

### 57. Crash-safe shutdown — 🔴 OPEN

window listener: flush config/save, close audio lines, stop the loop thread ([OPEN 13](#finding-32)).

<a id="finding-77"></a>

### 58. (Optional, later) threading model doc — 🔴 OPEN

even if you stay single-threaded, write down EDT vs game-thread ownership rules now; it prevents an entire class of races as more devs join.

## FIXED

<a id="finding-1"></a>

### 1. Collision check on the RIGHT direction inspects the wrong column — 🟢 FIXED
**Severity:** `P0`

**Where:** `src/main/java/org/savilusGame/utils/CollisionDetector.java` (`checkTile`)

```java
case RIGHT -> {
    entityRightCol = Math.floorDiv(entityRightWorldX + gameEntity.getSpeed(), TILE_SIZE);
    if (isSolidTile(currentMapTiles, entityRightCol, entityTopRow)
        || isSolidTile(currentMapTiles, entityRightCol, entityBottomRow)) {
        gameEntity.setCollisionOn(true);
    }
}
```

Historical observation: at `20277f2`, the RIGHT branch already checks both corners on the leading column, so the reported wrong-column behavior was not reproducible in the current source. The branch now retains that behavior while tile lookups go through `isSolidTile`, which treats out-of-map coordinates as solid and reports invalid tile ids explicitly.

**Resolution in this worktree:** retain the leading-edge check:
```java
isSolidTile(currentMapTiles, entityRightCol, entityTopRow)
    || isSolidTile(currentMapTiles, entityRightCol, entityBottomRow)
```
Regression tests cover a blocked top-right corner and both positive and negative out-of-map boundaries.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified. The earlier agent attempt to start `./gradlew test --tests org.savilusGame.service.CollisionServiceTest --tests org.savilusGame.entity.ActorBehaviorTest` returned `Permission denied` (exit 126).

<a id="finding-2"></a>

### 2. Attack reach uses `attackArea.height` for horizontal offset — 🟢 FIXED
**Severity:** `P0`

**Where:** `src/main/java/org/savilusGame/entity/Actor.java` (`attacking()`)

```java
case RIGHT -> worldX += attackArea.width;
```

Historical observation: the reviewed source already offsets RIGHT attacks by `attackArea.width`; no production-code correction was needed. A regression test uses a non-square attack area to preserve the contract.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified. The earlier agent Gradle attempt returned `Permission denied` (exit 126).

<a id="finding-3"></a>

### 3. Entity center computed from sprite width/height, swapped — 🟢 FIXED
**Severity:** `P1`

**Where:** `src/main/java/org/savilusGame/entity/Actor.java` (`getCenterX`, `getCenterY`)

```java
int getCenterX() { return worldX + solidArea.x + solidArea.width / 2; }
int getCenterY() { return worldY + solidArea.y + solidArea.height / 2; }
```

Historical observation: the current implementation already derives both centers from the solid area, not sprite frames, so the reported defect was not present at `20277f2`. A regression test verifies the offset and dimensions without requiring sprite initialization.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified. The earlier agent Gradle attempt returned `Permission denied` (exit 126).

<a id="finding-4"></a>

### 4. Monster contact deals damage + 1 flat, bypassing the damage pipeline — 🟢 FIXED
**Severity:** `P1`

**Where:** `src/main/java/org/savilusGame/entity/Player.java` (`detectMonsterContact`)

```java
if (monsterIndex != INIT_INDEX) {
    Actor monster = gamePanel.getWorld().currentMonsters().get(monsterIndex);
    if (!monster.isDying()) {
        monster.damagePlayer(monster.getAttack());
    }
}
```

Historical observation: the current method did not contain the reported second flat subtraction, but it bypassed `Actor.damagePlayer(int)`. It now delegates contact damage to the colliding monster's shared pipeline, including guard/parry and invincibility handling. Headless regression tests cover single application and parry behavior.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified. The earlier agent Gradle attempt returned `Permission denied` (exit 126).

<a id="finding-5"></a>

### 5. `GameEntityFactory` maps **BLUE_HEART → BlueShield** — 🟢 FIXED
**Severity:** `P1`

**Where:** `src/main/java/org/savilusGame/entity/GameEntityFactory.java`

```java
itemMap.put(BLUE_HEART, () -> new BlueHeart(gamePanel));
```

Historical observation: the factory already maps `BLUE_HEART` to `BlueHeart` at `20277f2`; no production-code correction was needed. A regression test verifies the factory result for the localized Blue Heart name.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified. The earlier agent Gradle attempt returned `Permission denied` (exit 126).

<a id="finding-6"></a>

### 6. Save/load indexing assumes `mapsObjects` has every map — it doesn't — 🟢 FIXED
**Severity:** `P1`

**Where:** `GameEntity.java:315` (`getDetected`), `AssetSetter.java` (`setObject` creates keys `MAIN_MAP`, `DUNGEON_FIRST_FLOR`, `DUNGEON_SECOND_FLOR` — **no `INTERIOR_MAP`**)

`getDetected()` does `target.get(CURRENT_MAP).size()` with no null check. Using a Key (or any item whose `use()` calls `getDetected`) while inside `interior01.txt` dereferences `null` → `NullPointerException` on the Swing EDT (Key usage is triggered from `selectItem()` ← `KeyHandler` ← `keyPressed`). The exception is swallowed by Swing, `enterPressed` stays in a half-processed state, and the interaction just silently fails.

Same class of bug, different sites: `GamePanel.java:262` (`for (GameEntity projectile : projectiles.get(CURRENT_MAP))` — the one map-entity list *not* null-checked in `drawToTempScreen()`), and `Player.damageMonster`/`detectMonsterContact`/`damageInteractiveTile`, which index `mapsMonsters`/`mapsInteractiveTiles` by position after an `INIT_INDEX` sentinel check that is easy to defeat (any index ≥ 0 from an empty list, or a concurrent removal, corrupts assumptions).

**Fix:** (a) make the maps *complete* — build `mapsObjects`/`mapsMonsters`/`mapsNpc`/`projectiles`/`mapsInteractiveTiles` from a single declared list of map ids so every map is always present (even with an empty list), e.g. a `MapId` enum or a `World`/`GameSession` object; (b) replace `999` with `-1` or `OptionalInt` throughout; (c) null-check at the boundary anyway. The root fix is (a): "possibly missing key" is the defect, not the missing `if`.

**Resolution in the worktree based on `20277f2`:** `World` initializes map-scoped collections for all loaded maps and maintains them through reseeding and map switches. The collision sentinel is `-1`, indexed player/projectile interactions check their inputs and log stale indices, and `ObjectDetector` still tolerates an absent list. Regression tests were added in `WorldTest`, `CollisionServiceTest`, `ObjectDetectorTest`, and `ActorBehaviorTest`.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified.

<a id="finding-7"></a>

### 7. Saved data stores only the *current* map, and loses the boss flag — 🟢 FIXED
**Severity:** `P1`

**Where:** `SaveLoad.java:89-113`; `data/Progress.java` (entire file); `entity/monster/SkeletonLord.java:140`

`save()` serializes `mapsObjects.get(CURRENT_MAP)` only. Objects collected or removed in every other map (chests opened, coins picked up, the iron door removed after the rock puzzle, doors unlocked) silently reappear when the player reloads in a different map than the one they saved in. Complementary losses:

- `Progress.skeletonLordDefeated` is a `public static boolean` that is **never serialized**. After loading a save in which the boss was defeated, the flag resets to `false` → the Skeleton Lord respawns at his spawn point and the `EventHandler.skeletonLord()` cutscene can trigger again.
- `saved`: player stats, inventory, equipment, interactive tiles (correctly iterated across *all* maps — the object block is the odd one out), and only the current map's objects.
- `not saved`: current map id, player world position (load resets to the default spawn — see `Player.setDefaultPositions()` called from `load()`), boss defeat, event one-time flags (`EventRect.oneTimeEventDone`), chest contents for unopened chests in other maps.

**Fix:** serialize a `SaveGame` DTO that covers *all* maps (object name/x/y/opened/loot, interactive tile state — you already do this correctly), plus `currentMap`, player `worldX/worldY`, `skeletonLordDefeated`, and event one-time flags. Bump the format with a `schemaVersion` field so future migrations are possible (see [OPEN 22](#finding-41)).

**Resolution in the worktree based on `20277f2`:** the baseline already iterated object lists across maps, although other world state was still missing. Versioned saves include every map's objects, drops, interactive tiles and event flags, plus the active map, player position, inventory, equipment and boss defeat flag. Seeded and temporary drops are both saved so collected drops do not respawn; monster reseeding respects boss defeat. Temporary boss-battle doors are excluded because encounters restart on load. `SaveLoadTest` contains cross-map and temporary-door regression tests.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified.

<a id="finding-9"></a>

### 8. Persistent identity is the *localized display string* — 🟢 FIXED
**Severity:** `P1`

**Where:** `enums/GameObject.java` (`name = TextManager.getItemName(textKey)`), `SaveLoad` (stores `inventory.get(i).getName()` / object names), `GameEntityFactory.getGameEntity(String)` (linear `equalsIgnoreCase` scan), `BigRock.java:54,58,83`, `Door.java` (uses `GameObject.DOOR.name()` — the only enum-consistent one, and it's "consistent" by accident of `equalsIgnoreCase`)

Entities set `name` to the **display name from the JSON texts** (`"Woodcutter's Axe"`). That string is then: stored in the save file, used as the factory lookup key, and compared for puzzle logic (BigRock ↔ MetalPlate ↔ IronDoor). Consequences:

1. Editing a display string in `items.json` silently breaks loading every existing save (`getObjectFromName` throws `IllegalArgumentException` → whole `load()` aborts → see [FIXED 10](#finding-8)) and silently breaks puzzle detection (string compare no longer matches).
2. The game is effectively un-localizable: any translated build invalidates saves made in another language.
3. Save files are not robust even to refactors of the text files.

**Fix:** introduce a stable `id` per item (the `GameObject` enum constant name / `textKey` is ideal) and use *it* for persistence, factory dispatch, and puzzle comparisons. Keep `name` purely as a rendering concern. `getObjectFromName` should return `Optional<GameObject>` (or throw with the offending id) and the load path should tolerate unknown ids by skipping the item with a warning, not by aborting the whole load.

**Resolution in the worktree based on `20277f2`:** item and object saves use stable enum identities, the factory prefers ids and retains legacy-name lookup for migration, door detection and the rock/plate/door puzzle use object identities, and item pickup uses the item id. Unknown entries are logged; regression tests cover localization-independent lookup.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified.

<a id="finding-10"></a>

### 9. Boss defeat flag, and `EventRect.oneTimeEventDone`, are the only world-state outside the save — and they vanish — 🟢 FIXED
**Severity:** `P1`

**Where:** `data/Progress.java`, `utils/event/EventRect.java`, `EventHandler.java` (`eventRect` map only ever populated for `MAIN_MAP`)

Covered in [FIXED 7](#finding-7) for the boss flag. Additionally: `EventRect` state is per-session only, and the `eventRect` grid is only built for `MAIN_MAP` but `hit()` reads `eventRect.get(MAIN_MAP)` for events on *every* map (works today only because geometry is identical across maps — dungeon events like `hit(25, 27)` index the main-map array). If per-map rects ever diverge, dungeon events silently use main-map rects.

**Fix:** key `eventRect` by map id (`Map<String, EventRect[][]>`), persist one-time flags in the save, and delete `Progress.java` (its single field belongs in the save DTO).

**Resolution in the worktree based on `20277f2`:** event grids are allocated per loaded map, hit checks use the current map, and completed event positions and the boss flag are persisted and restored. Boss reseeding honors the restored flag; a new game clears it and the event flags.

**Verification:** IDE `Run Tests` on all project tests (no shell command): 45 passed, none failed or skipped; see `build/test-results/test/` (2026-11-17). The user reports manually exercising the changed functions; individual scenarios were not specified. The earlier agent Gradle attempt returned `Permission denied` (exit 126).

<a id="finding-8"></a>

### 10. Save format uses Java serialization without `serialVersionUID`, non-atomic, unclosed streams — 🟢 FIXED
**Severity:** `P1`

**Where:** `data/DataStorage.java` (no `serialVersionUID`), `SaveLoad.java:35-150`

- **No `serialVersionUID`** → any refactor of `DataStorage` (rename/reorder/type-change a field) invalidates every existing save with `InvalidClassException`. Java serialization also pins you to exactly this class at exactly this package path forever — a poor fit when the game is "going to be developed into a big one".
- **Non-atomic write**: `new FileOutputStream("save.dat")` truncates in place; a crash mid-write corrupts the only save the player has.
- **Streams never closed**: `outputStream`/`inputStream` are never closed or flushed in a `finally`/try-with-resources; on failure the buffer may never hit disk, and the handle leaks. `Try.run(...)` swallows the exception into a log line ("Error saving data") with no user-visible signal.
- **Load is all-or-nothing halfway**: `load()` applies player stats, then clears and rebuilds the inventory, then reads equipment slot indices (`getInventory().get(currentWeaponSlot)`) with no bounds check. A stale/corrupt save (e.g., written by an older build, or hand-edited) throws `IndexOutOfBoundsException` *after* `getInventory().clear()` and the factory loop — leaving the player with an empty inventory and unchanged stats on screen. The `Try` wrapper logs it; the game carries on in a corrupt state.
- **Path is CWD-relative** (`"save.dat"`): the save lands wherever the process was started — project root in development, nowhere sensible (or unwritable) when installed.

**Fix:** replace with versioned JSON (`jackson-databind` is already a dependency — use it), e.g. `save.json` containing a `schemaVersion`, written to a temp file then atomically moved (`Files.move(..., ATOMIC_MOVE)`), loaded defensively (validate ranges, tolerate missing keys, wrap equipment re-resolution in a fallback to the default sword/shield). Store it in a per-user data dir (`java.util.prefs`/`System.getProperty("user.home")/.blue-boy/` or the OS equivalent), not the working directory. If you must keep binary serialization short-term, add `serialVersionUID` and try-with-resources and atomic write — but JSON is the right target.

**Current worktree based on `20277f2`:** saves use versioned JSON in `~/.blue-boy/save.json`, closed streams and atomic same-directory replacement. Load validates and constructs the snapshot before applying it, preserves equipment slot identity when skipping unknown items, uses default equipment for invalid slots, and reports failures to the player and log. A filtered one-time importer attempts to read legacy `save.dat` without deleting it; incompatible Java-serialized versions cannot be recovered automatically. Tests cover round trips, corrupt data, fallback equipment, failure reporting and legacy import.

**Remaining (historical):** the final application phase could still leave live state partially changed if it failed.

**Resolution in the current worktree (reassessed 2026-11-17):** `SaveLoad.load()` captures a
`data/LiveStateBackup` after preparation and before `apply()` — per-map objects/drops/NPCs/monsters/interactive tiles,
completed events, current map, boss/event flags, event-tracker fields, player statistics and inventory, and the current
area. Any `RuntimeException` while applying the prepared save now restores that snapshot, reports the failure to the
player and returns `false`, so a failed load leaves the previous game state in place instead of a half-restored world.
Save handling itself is unchanged from the description above (versioned JSON, `~/.blue-boy/save.json`, atomic
same-directory replacement, legacy `save.dat` import that never deletes the original).

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `data/SaveLoad.java`, new `data/LiveStateBackup.java`,
`data/UserDataDirectory.java`, `SaveLoadTest.java`.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-11"></a>

### 11. Level-up discards overflow experience — 🟢 FIXED
**Severity:** `P1`

**Where:** `Player.java:387-403` (`checkLevelUp`) and `Player.java:217` (`setDefaultValues`)

```java
if (exp >= nextLevelExp) {
    level++;
    nextLevelExp = nextLevelExp + 5;
    exp = 0;          // ← overflow lost; only one level per check
    ...
}
```

A reward granting 12 exp at 5 exp/level should yield two levels and 2 remainder exp; it yields one level and 0 exp. Also `nextLevelExp` grows by a constant 5, which is a design smell but not a bug — leave the curve alone unless you intend to change it.

**Fix:** `while (exp >= nextLevelExp) { exp -= nextLevelExp; level++; nextLevelExp += 5; ... }`

**Resolution in the current worktree (reassessed 2026-11-17):** `Player.checkLevelUp()` now
subtracts each reached threshold in a loop, so overflow experience is kept and one reward can grant several levels;
the +5 curve and the +2 life bonus are unchanged and lifted into named constants (`NEXT_LEVEL_EXP_STEP`,
`LEVEL_UP_LIFE_BONUS`). Experience enters through a new package-private `Player.gainExperience(int)` (used by
`damageMonster`), which is the test seam; the level-up sound, state change and dialogue fire once per reward, after
the loop, not once per level.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `entity/Player.java`, new `PlayerLevelUpTest.java`.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-12"></a>

### 12. The game cannot run from anything but the project root, and cannot be distributed — 🟢 FIXED
**Severity:** `P0`

**Where:** `utils/text/TextManager.java:21-23`, `TextLoader.java` (`new File(folderPath)` + `Files.walk`), `config/Config.java:22` (`"src/main/gameConfig.txt"`), `SaveLoad.java:29` (`"save.dat"`), `Main.java`

Bundled texts are read via **filesystem paths relative to the CWD** (`src/main/resources/texts/...`) instead of the classpath. Consequences:

1. Run the jar from any other directory → `TextLoader` finds nothing → `dialogues`/`items`/`settingsAndUI` are **empty maps** → `TextManager.getUiText(...)` returns `null` → `null.get(...)` on the next line throws NPE → dialogues/UI/item names all break (or the game dies at startup inside enum init — see [FIXED 13](#finding-13)).
2. `gameConfig.txt` is *written into the source tree* (`src/main/gameConfig.txt` — a build artifact that was committed to git; see [OPEN 35](#finding-54)). Installed read-only, saving options fails silently.
3. Packaging as a single jar / jpackage / native image is impossible; this is the main blocker for actually shipping Blue Boy.

**Fix:**
- Load texts as **classpath resources** (`getResourceAsStream`) or, better, enumerate them at build time and bundle a single `texts.json`-style index. Fail-fast at startup with a clear message if a required key is missing (validate the three maps once at boot, not lazily at first draw).
- Put user state in a per-user directory (`${user.home}/.blue-boy/` or OS conventions): `save.json`, `config.properties`.
- Keep bundled defaults on the classpath; never write into `src/` at runtime.

**Resolution in the current worktree (reassessed 2026-11-17):** `TextLoader` now reads every
text file through `getResourceAsStream` using a bundled index (`src/main/resources/texts/index.json`); a missing,
unreadable or malformed file aborts start-up with the offending resource name, and `TextManager` fails fast on a
missing group/key instead of returning `null` into the next statement. Settings moved to
`~/.blue-boy/config.properties` (`Config`, written atomically and only when a value changed), saves to
`~/.blue-boy/save.json`, and the working directory is consulted only for the one-time legacy `save.dat` import. The
per-user directory is overridable with `-Dblueboy.dataDir=...`, and `UserDataDirectory` is the single place that
resolves it. Images, fonts, sounds, tile maps and `config.yml` already used the classpath and are unchanged.

**Remaining:** actual packaging (jar/jpackage) and the still-committed `src/main/gameConfig.txt` artefact are separate
findings ([OPEN 11](#finding-30), [OPEN 35](#finding-54), [OPEN 54](#finding-73)); class and working-directory
independence is what this change delivers.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `utils/text/TextLoader.java`, new
`src/main/resources/texts/index.json`, `utils/text/TextManager.java`, `config/Config.java`, new
`data/UserDataDirectory.java`, `data/SaveLoad.java`.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-13"></a>

### 13. `GameObject` enum does I/O inside its constructor — 🟢 FIXED
**Severity:** `P1`

**Where:** `enums/GameObject.java:52-56` (`GameObject(String textKey) { name = TextManager.getItemName(textKey); ... }`)

Enum class-init now depends on the filesystem ([FIXED 12](#finding-12)) and on `TextManager`'s static init. Any failure → `ExceptionInInitializerError` — the JVM kills the thread with an error that bypasses the `Try` wrappers and is extremely hard to diagnose from a player's report ("the game doesn't start on Linux"). It also makes the enum untestable in isolation and couples *every* class that references a `GameObject` to the whole text-loading stack.

**Fix:** store only `textKey` in the enum; resolve display names via `TextManager.getItemName(key)` at the call site (or via a cached lookup in a `ItemCatalog`). The enum becomes a pure constant holder — as enums should be.

**Resolution in the current worktree (reassessed 2026-11-17):** `GameObject` now holds only the
stable `textKey`; its constructor performs no lookups and the enum no longer imports `TextManager`. Every display
name is resolved at the call site through `TextManager.getItemName(gameObject.getTextKey())` (`Item`, `Chest`,
`Door`, `IronDoor`, `Fireball`, `Rock`, `BigRock`, `MetalPlate`). `getObjectFromName` (localized-name matching) was
removed from the enum; the legacy-save fallback that still has to accept localized names lives in
`GameEntityFactory.resolve`, next to the migration it serves. Identity everywhere remains the enum constant name.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `enums/GameObject.java`, `entity/item/Item.java`,
`entity/items/{Chest,Door,IronDoor}.java`, `entity/projectile/{Fireball,Rock}.java`,
`entity/interactiveObjects/BigRock.java`, `entity/interactiveTile/MetalPlate.java`, `entity/GameEntityFactory.java`.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-14"></a>

### 14. `KeyHandler`: cheats and `System.exit` in release input paths — 🟢 FIXED
**Severity:** `P2`

**Where:** `utils/KeyHandler.java` — `VK_G` (god mode), `VK_T` (debug text), `VK_R` (reload map/cheat), `VK_X`, `System.exit(0)` in `titleState`, `gameConfig.txt` machinery

None of this is gated behind a flag. For a game "developed into a big one", debug/cheat entry points should live behind a `devMode` property (`-Dblueboy.dev=true`) or a hidden settings toggle, not shipped unconditionally. Direct `System.exit` bypasses application cleanup unless it is registered as a JVM shutdown hook (music line cleanup, atomic save on exit — see [OPEN 57](#finding-76)).

**Fix:** central `DevMode` gate; route quit through `windowClosing`/a `GameController.quit()` that stops audio and (optionally) auto-saves; use Swing **Key Bindings** (`InputMap`/`ActionMap`) instead of a raw `KeyListener` so input works regardless of focus and is remappable — which also sets up future control rebinding (an obvious next feature for a game with WASD hard-coded in `KeyHandler`).

**Resolution in the current worktree (reassessed 2026-11-17):** a new `config.DevMode`
(release default: off, enabled with `-Dblueboy.dev=true`) is injected into `KeyHandler`; `VK_G`, `VK_T` and `VK_R` are
inert unless it is enabled. `System.exit` is gone from the input path: the title-menu quit calls the new
`GamePanel.quitGame()`, which stops music and sound effects, ends the game loop, closes the window and terminates the
JVM; the window's close button is wired to the same method (`DO_NOTHING_ON_CLOSE` + listener), so cleanup cannot be
skipped. `Sound.stop()` is null-safe, so quitting before any track played no longer throws. `VK_X` (minimap toggle) is
kept as a normal feature.

**Out of scope:** input remapping remains a separate future improvement tracked by [OPEN 54](#finding-73); it does not block this fix for release-mode cheats and quit cleanup.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — new `config/DevMode.java`, `utils/KeyHandler.java`,
`GamePanel.java`, `Main.java`, `Sound.java`.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-15"></a>

### 15. Duplicate knockback/damage implementation between `Player` and `GameEntity` — 🟢 FIXED
**Severity:** `P2`

**Where:** `Player.update()` (knockback branch, lines ~445-470) vs `GameEntity.update()` (knockback branch) — same constants (`KNOCK_BACK_TIME = 5` inline vs `KNOCK_BACK_TIME` field), same switch, slightly different collision treatment.

This already diverged once (`Player` uses literal `5`; base uses the constant; knockback direction handling differs subtly). **Fix:** delete the `Player` override path and let the base-class knockback run (it already calls `checkTile` + entity checks — the extra `checkObject` in the player path can be lifted into the base).

**Resolution in the current worktree (reassessed 2026-11-17):** the knockback step now exists
once, on `Actor`: `updateKnockBack()` (drift one step in the hit direction unless blocked, end after
`KNOCK_BACK_TIME` ticks or on the first collision, restore the normal speed) and `resolveWorldCollisions(boolean
forPlayer)` (tiles, world objects, NPCs, monsters, interactive tiles). `Actor.update()` runs `checkCollision()` — which
now delegates to `resolveWorldCollisions(false)` and keeps the player-contact damage — and then `updateKnockBack()`;
`Player.update()` runs `resolveWorldCollisions(true)` and the same `updateKnockBack()`. The literal `5` in the player
path is gone: `KNOCK_BACK_TIME` is the only duration. Behaviour is unchanged (same collision order, the player still
resolves object collisions with `forPlayer = true` so the reported index stays the player's, knockback still ends on
the first collision; `==` became `>=` only to stay correct if the counter is ever advanced twice). The null guard on
`currentInteractiveTiles()` is kept, because the removed player path had it.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `entity/Actor.java`, `entity/Player.java`,
`ActorBehaviorTest.java` (2 new tests: `knockBackUsesTheSharedStep`, `collisionEndsKnockBack`).

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-16"></a>

### 16. `door.name = GameObject.DOOR.name()` only works by accident — 🟢 FIXED
**Severity:** `P2`

**Where:** `entity/items/Door.java:23`

Every other entity uses the localized `getName()`; `Door` uses the enum constant name (`"DOOR"`). The Key-vs-Door check passes only because `getObjectFromName` compares with `equalsIgnoreCase`. Any tightening of identity handling ([FIXED 8](#finding-9)) breaks Key→Door silently. **Fix:** part of [FIXED 8](#finding-9) — compare stable ids everywhere.

**Resolution in the current worktree (reassessed 2026-11-17):** `Door` already takes its
display name from the localized text (`TextManager.getItemName(GameObject.DOOR.getTextKey())`), and every identity
decision about it is made on the stable `GameObject.DOOR` value, so the key no longer depends on any name. The one
remaining identity-by-display-string was in `CutsceneManager`: the boss was located by comparing the localized
`Actor.getName()` with `MonsterType.SKELETON_LORD.getName()` (two copies, one per scene phase). Both now use a single
`skeletonLord()` lookup that filters on the `SkeletonLord` type, so a translated or renamed boss still drives the
cutscene and a minion that happens to be called "Skeleton Lord" is not promoted into it.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `utils/CutsceneManager.java` (single `skeletonLord()`
lookup, name comparison and the `MonsterType`/`StringUtils` imports removed), `ObjectDetectorTest.java`
(`keyOpensTheDoorEvenWhenTheDoorIsRenamed`), new `CutsceneManagerTest` (type wins over display name in both
directions, and a map without the boss yields no boss).

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-17"></a>

### 17. Small but real: `getImage()` loads `BOY_UP1` twice — 🟢 FIXED
**Severity:** `P3`

**Where:** `Player.java:getImage()` — `up1 = setup(BOY_UP1, ...)` appears twice; harmless, but signals the asset paths are maintained by hand (see [OPEN 31](#finding-50)). Also `MerchantNPC` assigns `up*` the down-frames (deliberate), and `GameEntity.setImage`-style duplication across `AssetSetter`/`Player`/`NPC` constructors is heavy copy-paste.

**Resolution in the current worktree (reassessed 2026-11-17):** the duplicate `up1` assignment
is deleted, so `getImage()` loads each of the eight walking sprites exactly once. The removed line was behaviour
neutral (it loaded the same asset over the same field), so the new test guards the contract that the line obscured —
all eight sprites are loaded, are tile sized and are all different — rather than the line itself.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `entity/Player.java` (`getImage()`), new
`PlayerSpriteTest` (`everyWalkingSpriteIsLoaded`, `eachDirectionAndFrameUsesItsOwnImage`).

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-18"></a>

### 18. Inventory capacity has two sources of truth — 🟢 FIXED
**Severity:** `P3`

`WoodShield`/`BlueShield` and friends: `item.setAmount(...)` on fresh instances during load is fine, but `GameEntity.maxInventorySize = 20` duplicates `GamePanel.ALLOWED_ITEMS = 20` (unused, `GamePanel.java:53`). Keep one authoritative inventory limit.

**Resolution in the current worktree (reassessed 2026-11-17):** `Player` owns the inventory
geometry: `INVENTORY_COLUMNS = 5`, `INVENTORY_ROWS = 4` and `MAX_INVENTORY_SIZE = INVENTORY_COLUMNS *
INVENTORY_ROWS`. `GamePanel.ALLOWED_ITEMS` is deleted (it had no readers at all) and nothing else keeps a second
limit. `KeyHandler` navigation stops at `Player.INVENTORY_ROWS - 1` / `Player.INVENTORY_COLUMNS - 1` instead of the
literals `3`/`4`, and `UI` draws its row breaks and computes the slot index from `INVENTORY_COLUMNS` instead of the
literals `4`/`9`/`14`/`5`. Capacity, layout and cursor arithmetic can therefore no longer drift apart.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — `entity/Player.java`, `GamePanel.java`, `UI.java`,
`utils/KeyHandler.java`; tests `PlayerInventoryCapacityTest` (the limit is the grid size, exactly
`MAX_INVENTORY_SIZE` items are accepted and the next one is refused), `UIInventoryGridTest` (slot-to-index mapping,
including the last slot) and `KeyHandlerTest.inventoryCursorStopsAtTheLastSlot`.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.

<a id="finding-19"></a>

### 19. Game loop busy-spins at 100% CPU when idle — 🟢 FIXED
**Severity:** `P0`

**Where:** `GamePanel.java:379-401` (`run()`)

```java
while (Objects.nonNull(gameThread)) {
    currentTime = System.nanoTime();
    delta += (currentTime - lastTime) / drawInterval;
    ...
    if (delta >= 1) { update(); drawToTempScreen(); drawToScreen(); }
}
```

There is no `Thread.sleep`/`LockSupport.parkNanos` anywhere. When `delta < 1` the loop spins hot, consuming an entire core even on the title screen. It "works" on desktop but will throttle laptops, distort FPS-dependent timing under load, and trip antivirus/thermal alarms — and any hiccup makes it worse, because there is no clamp on `delta` either (pause the process for 2 s and it will execute ~120 update ticks in a burst — the classic spiral of death).

**Fix:**
```java
if (delta >= 1) {
    update(); drawToTempScreen(); drawToScreen();
    delta = Math.min(delta - 1, MAX_CATCHUP_UPDATES); // e.g. 5
} else {
    LockSupport.parkNanos(1_000_000); // or Thread.sleep(1)
}
```
(Or adopt the standard "render every frame, update at fixed step" shape with `System.currentTimeMillis` deadline scheduling.) Also stop printing `"FPS: " + drawCount` to stdout every second (`GamePanel.java:396`) — use the logger at debug level or an on-screen perf overlay.

**Resolution in the current worktree (reassessed 2026-11-17):** the loop now uses fixed-slot
deadline scheduling from `service/FrameScheduler`: one update+render pass per slot, then `LockSupport.parkNanos`
until the next slot is due, so an idle loop sleeps instead of spinning. The unclamped `delta` accumulator is gone;
a loop that fell behind a whole slot restarts its deadline from the current time, which is the clamp the finding
asked for — missed frames are dropped rather than replayed as a burst of ticks. The per-second `System.out.println`
is now `log.debug("FPS: {}", drawCount)`, so it is silent unless debug logging is enabled. Behaviour note: a stall
previously made the game catch up with several ticks at once; it now continues at 60 Hz without the surge.

**Change reference:** branch `arena/fc282c94-blue-boy-game` — new `service/FrameScheduler.java`, `GamePanel.java`
(`run()`, new `awaitNextFrame`, `@Slf4j`); new `FrameSchedulerTest` (first frame immediate, one-sixtieth-of-a-second
slots, waiting for the rest of a slot, no burst after a stall, a slightly late frame keeps the rhythm, non-positive
frame rate rejected). The loop body itself is not unit-testable without running the game; the pacing arithmetic it
delegates to is covered.

**Verification:** `.\gradlew.bat test` — `BUILD SUCCESSFUL` (2026-11-17; 87 tests passed, 0 failed, 0 skipped). The user confirmed starting the game and manually checking all ten reviewed flows; they worked as expected.
