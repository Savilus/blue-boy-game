# Code Review — Blue Boy (Java 2D Adventure)

**Prepared by:** External Java developer (contract review)
**Date:** 2026-11-06
**Audience:** The development team (Java developers working on this repository)
**Scope:** Full source tree at commit `0511ef2` — `src/main/java`, resources, build scripts, repository hygiene
**Deliverable type:** Technical review report, not a fix branch. Line references are accurate as of the commit above; the codebase moves fast, so treat line numbers as approximate once edits start landing.

---

## 0. Executive summary

Blue Boy is a genuinely impressive piece of work for a self-driven project: it has a fixed-timestep game loop, A\* pathfinding, a day/night lighting system, a quest/boss flow, save/load, a shop, and content-driven text — most tutorial-grade 2D games never get past three of those. The core is functional and clearly playable.

That said, the codebase is at an inflection point. It is structured as a classic "giant tutorial project": one god class (`GamePanel`), one ~760-line god superclass (`GameEntity`) carrying roughly 60 fields of unrelated concerns, static mutable globals, filesystem-relative resource loading, and no tests, no CI, and no packaging. Every one of those was a reasonable shortcut to get a game on screen; none of them will survive a team adding maps, mechanics, and save migrations for years.

The good news: almost everything below is incremental. The highest-risk items are a handful of concrete bugs (Section 3) and the resource-loading model (3.12 / 6.1), which blocks *any* distribution of the game. After those, the roadmap in Section 11 orders the rest by payoff.

**Headline verdict:** keep the architecture's bones, fix the bugs, make resources classpath-based, add tests and CI — then the game is genuinely production-shaped.

---

## 1. How to read this report

Each finding has:

- **Where** — file and line (as of `0511ef2`).
- **What / Why it matters** — the concrete failure mode or cost.
- **Suggested fix** — the approach I would take, biased toward the fact that this project will keep growing.

Severity legend:

| Level | Meaning |
|---|---|
| **P0 — Critical** | Wrong behaviour the player can hit today, or a defect that blocks distributing the game at all. |
| **P1 — High** | Likely defect, data loss, or silent corruption under normal play; or an architectural decision that gets exponentially more expensive to fix later. |
| **P2 — Medium** | Performance smell, maintainability hazard, or robustness gap; worth scheduling deliberately. |
| **P3 — Low / polish** | Hygiene, style, docs. |

---

## 2. Environment and verification note

This review was performed **statically** — by reading the code. The sandbox used for the review has no JDK on the PATH and its network egress is restricted (no Gradle distribution download), so the project could not be compiled or executed here. Please treat Section 5 (build issues) as "high-confidence static findings" and confirm by running `./gradlew build` on a workstation. Nothing in this report is speculative about *logic* bugs — those are read directly from the code — but I could not playtest.

---

## 3. Correctness defects (fix these first)

### 3.1 `P0` — Collision check on the RIGHT direction inspects the wrong column
**Where:** `src/main/java/org/savilusGame/utils/CollisionDetector.java:68-75` (`checkTile`, `case RIGHT`)

```java
case RIGHT -> {
    entityRightCol = (entityRightWorldX + gameEntity.getSpeed()) / TILE_SIZE;
    tileNum1 = currentMapTiles[entityLeftCol][entityTopRow];   // ← wrong column
    tileNum2 = currentMapTiles[entityRightCol][entityBottomRow];
    ...
}
```

Every other direction checks both solid corners of the *leading* edge (UP/DOWN check left+right at the new row; LEFT checks both rows at the new column). RIGHT checks the **left** (trailing) column top row and the new bottom row — an asymmetry that lets an entity clip into a wall on its right (top-right corner blocked but tileNum1 passes) and be blocked by a wall behind it while the path ahead is clear.

**Fix:** mirror LEFT exactly:
```java
tileNum1 = currentMapTiles[entityRightCol][entityTopRow];
tileNum2 = currentMapTiles[entityRightCol][entityBottomRow];
```
While you are in there: the repeated `getTileManager().getTile()[tileNum].collision()` chains are unreadable; extract `private boolean isSolidTile(int col, int row)` and bounds-check `col/row` (the current code trusts map indices; one out-of-range cell = `ArrayIndexOutOfBoundsException` on the game thread).

### 3.2 `P0` — Attack reach uses `attackArea.height` for horizontal offset
**Where:** `src/main/java/org/savilusGame/entity/GameEntity.java:413` (`attacking()`)

```java
case UP    -> worldY -= attackArea.height;
case DOWN  -> worldY += attackArea.height;
case LEFT  -> worldX -= attackArea.width;
case RIGHT -> worldX += attackArea.height;  // ← height, not width
```

For square attack areas (all current weapons use square areas — `NormalSword` uses 36×36) this is latent; the first non-square weapon or boss area will shift hit resolution vertically-sized but horizontally applied.

**Fix:** `worldX += attackArea.width;`

### 3.3 `P1` — Entity center computed from sprite width/height, swapped
**Where:** `GameEntity.java:455-459`

```java
int getCenterX() { return worldX + up1.getHeight() / 2; }
int getCenterY() { return worldY + up1.getWidth()  / 2; }
```

Height is used for X and width for Y, and both are derived from `up1` — the facing-up walk sprite — regardless of what the entity actually is (a boss with a 5×-scaled frame, a chest, a particle…). This silently distorts every distance/aim calculation (`getXDistance`, `moveTowardPlayer`, `checkIfShouldAttack`, A\* goal selection) for entities whose frames aren't square or aren't 48px.

**Fix:** compute the centre from the **solid area**, which is the collision contract with the rest of the engine, e.g. `worldX + solidArea.x + solidArea.width / 2`. That also removes the NPE risk for entities with no walk frames.

### 3.4 `P1` — Monster contact deals damage + 1 flat, bypassing the damage pipeline
**Where:** `Player.java:412-424` (`detectMonsterContact`)

```java
int damage = ...monster.getAttack() - defense;
if (damage < 1) damage = 1;
currentLife -= damage;
...
currentLife -= 1;   // ← second, unexplained subtraction
```

Two problems stack here. First, the literal `currentLife -= 1` double-applies damage (almost certainly a leftover from debugging). Second, this duplicates `GameEntity.damagePlayer(int)` with subtly different semantics (no guard/parry handling, no knockback, no `invincibleCounter` parity with the base class). Two damage paths will diverge — they already have.

**Fix:** delete the method and route contact damage through `GameEntity.damagePlayer(attack)` (which already handles parry/guard/knockback/invincibility). One damage pipeline, one place to tune.

### 3.5 `P1` — `GameEntityFactory` maps **BLUE_HEART → BlueShield**
**Where:** `GameEntityFactory.java:87`

```java
entityMap.put(BLUE_HEART, () -> new BlueShield(gamePanel));
```

Copy-paste slip: picking up a Blue Heart (the boss reward) produces a Blue Shield. There is a `BlueHeart` class; it is only used for the map placement in `AssetSetter`, so the factory path — used by save/load and by `canObtainItem()` — yields the wrong item type, and the inventory then shows a shield the player never had.

**Fix:** `entityMap.put(BLUE_HEART, () -> new BlueHeart(gamePanel));`

### 3.6 `P1` — Save/load indexing assumes `mapsObjects` has every map — it doesn't
**Where:** `GameEntity.java:315` (`getDetected`), `AssetSetter.java` (`setObject` creates keys `MAIN_MAP`, `DUNGEON_FIRST_FLOR`, `DUNGEON_SECOND_FLOR` — **no `INTERIOR_MAP`**)

`getDetected()` does `target.get(CURRENT_MAP).size()` with no null check. Using a Key (or any item whose `use()` calls `getDetected`) while inside `interior01.txt` dereferences `null` → `NullPointerException` on the Swing EDT (Key usage is triggered from `selectItem()` ← `KeyHandler` ← `keyPressed`). The exception is swallowed by Swing, `enterPressed` stays in a half-processed state, and the interaction just silently fails.

Same class of bug, different sites: `GamePanel.java:262` (`for (GameEntity projectile : projectiles.get(CURRENT_MAP))` — the one map-entity list *not* null-checked in `drawToTempScreen()`), and `Player.damageMonster`/`detectMonsterContact`/`damageInteractiveTile`, which index `mapsMonsters`/`mapsInteractiveTiles` by position after an `INIT_INDEX` sentinel check that is easy to defeat (any index ≥ 0 from an empty list, or a concurrent removal, corrupts assumptions).

**Fix:** (a) make the maps *complete* — build `mapsObjects`/`mapsMonsters`/`mapsNpc`/`projectiles`/`mapsInteractiveTiles` from a single declared list of map ids so every map is always present (even with an empty list), e.g. a `MapId` enum or a `World`/`GameSession` object; (b) replace `999` with `-1` or `OptionalInt` throughout; (c) null-check at the boundary anyway. The root fix is (a): "possibly missing key" is the defect, not the missing `if`.

### 3.7 `P1` — Saved data stores only the *current* map, and loses the boss flag
**Where:** `SaveLoad.java:89-113`; `data/Progress.java` (entire file); `entity/monster/SkeletonLord.java:140`

`save()` serializes `mapsObjects.get(CURRENT_MAP)` only. Objects collected or removed in every other map (chests opened, coins picked up, the iron door removed after the rock puzzle, doors unlocked) silently reappear when the player reloads in a different map than the one they saved in. Complementary losses:

- `Progress.skeletonLordDefeated` is a `public static boolean` that is **never serialized**. After loading a save in which the boss was defeated, the flag resets to `false` → the Skeleton Lord respawns at his spawn point and the `EventHandler.skeletonLord()` cutscene can trigger again.
- `saved`: player stats, inventory, equipment, interactive tiles (correctly iterated across *all* maps — the object block is the odd one out), and only the current map's objects.
- `not saved`: current map id, player world position (load resets to the default spawn — see `Player.setDefaultPositions()` called from `load()`), boss defeat, event one-time flags (`EventRect.oneTimeEventDone`), chest contents for unopened chests in other maps.

**Fix:** serialize a `SaveGame` DTO that covers *all* maps (object name/x/y/opened/loot, interactive tile state — you already do this correctly), plus `currentMap`, player `worldX/worldY`, `skeletonLordDefeated`, and event one-time flags. Bump the format with a `schemaVersion` field so future migrations are possible (see 7.2).

### 3.8 `P1` — Save format uses Java serialization without `serialVersionUID`, non-atomic, unclosed streams
**Where:** `data/DataStorage.java` (no `serialVersionUID`), `SaveLoad.java:35-150`

- **No `serialVersionUID`** → any refactor of `DataStorage` (rename/reorder/type-change a field) invalidates every existing save with `InvalidClassException`. Java serialization also pins you to exactly this class at exactly this package path forever — a poor fit when the game is "going to be developed into a big one".
- **Non-atomic write**: `new FileOutputStream("save.dat")` truncates in place; a crash mid-write corrupts the only save the player has.
- **Streams never closed**: `outputStream`/`inputStream` are never closed or flushed in a `finally`/try-with-resources; on failure the buffer may never hit disk, and the handle leaks. `Try.run(...)` swallows the exception into a log line ("Error saving data") with no user-visible signal.
- **Load is all-or-nothing halfway**: `load()` applies player stats, then clears and rebuilds the inventory, then reads equipment slot indices (`getInventory().get(currentWeaponSlot)`) with no bounds check. A stale/corrupt save (e.g., written by an older build, or hand-edited) throws `IndexOutOfBoundsException` *after* `getInventory().clear()` and the factory loop — leaving the player with an empty inventory and unchanged stats on screen. The `Try` wrapper logs it; the game carries on in a corrupt state.
- **Path is CWD-relative** (`"save.dat"`): the save lands wherever the process was started — project root in development, nowhere sensible (or unwritable) when installed.

**Fix:** replace with versioned JSON (`jackson-databind` is already a dependency — use it), e.g. `save.json` containing a `schemaVersion`, written to a temp file then atomically moved (`Files.move(..., ATOMIC_MOVE)`), loaded defensively (validate ranges, tolerate missing keys, wrap equipment re-resolution in a fallback to the default sword/shield). Store it in a per-user data dir (`java.util.prefs`/`System.getProperty("user.home")/.blue-boy/` or the OS equivalent), not the working directory. If you must keep binary serialization short-term, add `serialVersionUID` and try-with-resources and atomic write — but JSON is the right target.

### 3.9 `P1` — Persistent identity is the *localized display string*
**Where:** `enums/GameObject.java` (`name = TextManager.getItemName(textKey)`), `SaveLoad` (stores `inventory.get(i).getName()` / object names), `GameEntityFactory.getGameEntity(String)` (linear `equalsIgnoreCase` scan), `BigRock.java:54,58,83`, `Door.java` (uses `GameObject.DOOR.name()` — the only enum-consistent one, and it's "consistent" by accident of `equalsIgnoreCase`)

Entities set `name` to the **display name from the JSON texts** (`"Woodcutter's Axe"`). That string is then: stored in the save file, used as the factory lookup key, and compared for puzzle logic (BigRock ↔ MetalPlate ↔ IronDoor). Consequences:

1. Editing a display string in `items.json` silently breaks loading every existing save (`getObjectFromName` throws `IllegalArgumentException` → whole `load()` aborts → see 3.8) and silently breaks puzzle detection (string compare no longer matches).
2. The game is effectively un-localizable: any translated build invalidates saves made in another language.
3. Save files are not robust even to refactors of the text files.

**Fix:** introduce a stable `id` per item (the `GameObject` enum constant name / `textKey` is ideal) and use *it* for persistence, factory dispatch, and puzzle comparisons. Keep `name` purely as a rendering concern. `getObjectFromName` should return `Optional<GameObject>` (or throw with the offending id) and the load path should tolerate unknown ids by skipping the item with a warning, not by aborting the whole load.

### 3.10 `P1` — Boss defeat flag, and `EventRect.oneTimeEventDone`, are the only world-state outside the save — and they vanish
**Where:** `data/Progress.java`, `utils/event/EventRect.java`, `EventHandler.java` (`eventRect` map only ever populated for `MAIN_MAP`)

Covered in 3.7 for the boss flag. Additionally: `EventRect` state is per-session only, and the `eventRect` grid is only built for `MAIN_MAP` but `hit()` reads `eventRect.get(MAIN_MAP)` for events on *every* map (works today only because geometry is identical across maps — dungeon events like `hit(25, 27)` index the main-map array). If per-map rects ever diverge, dungeon events silently use main-map rects.

**Fix:** key `eventRect` by map id (`Map<String, EventRect[][]>`), persist one-time flags in the save, and delete `Progress.java` (its single field belongs in the save DTO).

### 3.11 `P1` — Level-up discards overflow experience
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

### 3.12 `P0` — The game cannot run from anything but the project root, and cannot be distributed
**Where:** `utils/text/TextManager.java:21-23`, `TextLoader.java` (`new File(folderPath)` + `Files.walk`), `config/Config.java:22` (`"src/main/gameConfig.txt"`), `SaveLoad.java:29` (`"save.dat"`), `Main.java`

Bundled texts are read via **filesystem paths relative to the CWD** (`src/main/resources/texts/...`) instead of the classpath. Consequences:

1. Run the jar from any other directory → `TextLoader` finds nothing → `dialogues`/`items`/`settingsAndUI` are **empty maps** → `TextManager.getUiText(...)` returns `null` → `null.get(...)` on the next line throws NPE → dialogues/UI/item names all break (or the game dies at startup inside enum init — see 3.13).
2. `gameConfig.txt` is *written into the source tree* (`src/main/gameConfig.txt` — a build artifact that was committed to git; see 9). Installed read-only, saving options fails silently.
3. Packaging as a single jar / jpackage / native image is impossible; this is the main blocker for actually shipping Blue Boy.

**Fix:**
- Load texts as **classpath resources** (`getResourceAsStream`) or, better, enumerate them at build time and bundle a single `texts.json`-style index. Fail-fast at startup with a clear message if a required key is missing (validate the three maps once at boot, not lazily at first draw).
- Put user state in a per-user directory (`${user.home}/.blue-boy/` or OS conventions): `save.json`, `config.properties`.
- Keep bundled defaults on the classpath; never write into `src/` at runtime.

### 3.13 `P1` — `GameObject` enum does I/O inside its constructor
**Where:** `enums/GameObject.java:52-56` (`GameObject(String textKey) { name = TextManager.getItemName(textKey); ... }`)

Enum class-init now depends on the filesystem (3.12) and on `TextManager`'s static init. Any failure → `ExceptionInInitializerError` — the JVM kills the thread with an error that bypasses the `Try` wrappers and is extremely hard to diagnose from a player's report ("the game doesn't start on Linux"). It also makes the enum untestable in isolation and couples *every* class that references a `GameObject` to the whole text-loading stack.

**Fix:** store only `textKey` in the enum; resolve display names via `TextManager.getItemName(key)` at the call site (or via a cached lookup in a `ItemCatalog`). The enum becomes a pure constant holder — as enums should be.

### 3.14 `P2` — `KeyHandler`: cheats and `System.exit` in release input paths
**Where:** `utils/KeyHandler.java` — `VK_G` (god mode), `VK_T` (debug text), `VK_R` (reload map/cheat), `VK_X`, `System.exit(0)` in `titleState`, `gameConfig.txt` machinery

None of this is gated behind a flag. For a game "developed into a big one", debug/cheat entry points should live behind a `devMode` property (`-Dblueboy.dev=true`) or a hidden settings toggle, not shipped unconditionally. Direct `System.exit` bypasses any shutdown hook (music line cleanup, atomic save on exit — see 8.4).

**Fix:** central `DevMode` gate; route quit through `windowClosing`/a `GameController.quit()` that stops audio and (optionally) auto-saves; use Swing **Key Bindings** (`InputMap`/`ActionMap`) instead of a raw `KeyListener` so input works regardless of focus and is remappable — which also sets up future control rebinding (an obvious next feature for a game with WASD hard-coded in `KeyHandler`).

### 3.15 `P2` — Duplicate knockback/damage implementation between `Player` and `GameEntity`
**Where:** `Player.update()` (knockback branch, lines ~445-470) vs `GameEntity.update()` (knockback branch) — same constants (`KNOCK_BACK_TIME = 5` inline vs `KNOCK_BACK_TIME` field), same switch, slightly different collision treatment.

This already diverged once (`Player` uses literal `5`; base uses the constant; knockback direction handling differs subtly). **Fix:** delete the `Player` override path and let the base-class knockback run (it already calls `checkTile` + entity checks — the extra `checkObject` in the player path can be lifted into the base).

### 3.16 `P2` — `door.name = GameObject.DOOR.name()` only works by accident
**Where:** `entity/items/Door.java:23`

Every other entity uses the localized `getName()`; `Door` uses the enum constant name (`"DOOR"`). The Key-vs-Door check passes only because `getObjectFromName` compares with `equalsIgnoreCase`. Any tightening of identity handling (3.9) breaks Key→Door silently. **Fix:** part of 3.9 — compare stable ids everywhere.

### 3.17 `P3` — Small but real: `getImage()` loads `BOY_UP1` twice
**Where:** `Player.java:getImage()` — `up1 = setup(BOY_UP1, ...)` appears twice; harmless, but signals the asset paths are maintained by hand (see 8.5). Also `MerchantNPC` assigns `up*` the down-frames (deliberate), and `GameEntity.setImage`-style duplication across `AssetSetter`/`Player`/`NPC` constructors is heavy copy-paste.

### 3.18 `P3` — `WoodShield`/`BlueShield` and friends: `item.setAmount(...)` on fresh instances during load is fine, but `GameEntity.maxInventorySize = 20` duplicates `GamePanel.ALLOWED_ITEMS = 20` (unused, `GamePanel.java:53`) — two sources of truth for the same limit.

---

## 4. Performance and memory

### 4.1 `P0` — Game loop busy-spins at 100% CPU when idle
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

### 4.2 `P1` — `TileManager.draw()` tests 2,500 tiles per frame instead of computing the visible window
**Where:** `tile/TileManager.java:135-185`

```java
for (int row = 0; row < worldRows; row++)
  for (int col = 0; col < worldCols; col++) { int tileNum = map[col][row]; ... visibility check ... }
```

The viewport covers ~20×12 tiles; 96% of iterations are discarded by the `visibleMin/Max` test. Compute `startCol/endCol/startRow/endRow` from `playerWorld - playerScreen - TILE_SIZE` and iterate only that window. Same for the world-map pre-render (`GameMap.createWorldMap` renders the whole world each time it is called — fine occasionally, but it's called on every map switch (`UI.drawTransition`) in addition to once per construction; cache per map id).

### 4.3 `P1` — Per-frame allocation churn across the update/draw paths
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

**Why it matters:** at 60 fps with a busy scene (10 monsters × pathfinding + collisions + particles + projectiles), the young generation is filled with garbage objects that do nothing; GC pressure shows up as micro-stutters exactly when the action is dense. None of these are hard to fix; doing them as part of the render-loop work (4.1) amortises the cost.

**Fix:** hoist constants, reuse scratch `Rectangle`s (or better, remove the rect mutation entirely — see 4.5), replace copy+upcast streams with a pre-built `List<GameEntity>` view of interactive tiles (maintain it alongside `mapsInteractiveTiles`), cache fonts and comparators, switch to `ThreadLocalRandom` (or an injected `Random`).

### 4.4 `P1` — `PathFinder` is re-run *every frame* for every chasing monster, and is O(n²)
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
- `PathFinder.getPathList()` returns `ArrayList<Node>` and callers use `getFirst()` — a Java 21 `SequencedCollection` call (see 5.2). Return an unmodifiable `List<Node>` or `Deque<Node>`.

### 4.5 `P2` — Collision detection mutates shared `Rectangle`s as scratch space
**Where:** `CollisionDetector` (`checkObject`, `checkEntity`, `checkPlayer`, `checkGameEntityCollision`)

Every check: (1) writes world coords into the *entity's* `solidArea.x/y`, (2) writes into the *target's* `solidArea.x/y`, (3) optionally `y -= speed` on the entity's rect, (4) intersects, (5) restores the defaults. If anything throws between (1) and (5) — an NPE from a missing tile (`tile[num]` null), an AIOOBE, a bug in a future `checkX` — the entity's `solidArea` is left in **world coordinates** and every subsequent collision is nonsense, with no log line explaining why. It also mutates the *attacker's* rect during knockback using the caller's direction — subtle, and it forces the ugly "use knockBackDirection instead" branch in four places.

**Fix:** compute collision rectangles **locally** (`Rectangle entityRect = new Rectangle(x, y, w, h)` — a 4-field allocation is far cheaper than corruption, or reuse a per-call scratch rect), never write into `gameEntity.getSolidArea()`. The default-x/y restore dance disappears and `solidAreaDefaultX/Y` become unnecessary. This also makes the detector stateless and unit-testable.

### 4.6 `P2` — Resource loading has no cache: every entity construction re-reads and re-scales PNGs
**Where:** `GameEntity.setup()` (ImageIO.read + `UtilityTool.scaleImage`), called from every constructor; `AssetSetter.setMonster()` on **every area change** and `resetGame()`; `UI` constructs `Heart`/`ManaCrystal`/`BronzeCoin` entities purely to steal `mainImage`; `Player.canObtainItem()` constructs a **new entity from disk** to read `stackable`; `SaveLoad.load()` constructs every inventory item and map object from disk

Each `setup()` does `ImageIO.read` (file open + PNG decode) then allocates a scaled `BufferedImage`. The same `sun.png` is decoded dozens of times per session. Map transitions hitch visibly; opening the inventory panel stutters; loading a save re-decodes hundreds of images serially on the UI thread.

**Fix:** a tiny **sprite cache** (Flyweight): `SpriteCache.get(path, width, height)` with a bounded map keyed by `path|width|height`. Combined with a static item-definition table (`ItemCatalog`: id → stackable/price/description key), `canObtainItem` and the UI HUD stop constructing entities entirely. This single change usually removes the largest per-frame and per-action disk traffic in Swing games.

### 4.7 `P2` — `drawToScreen()` uses `getGraphics()` instead of Swing's paint path
**Where:** `GamePanel.java:207-212`

Calling `getGraphics()` per frame and then `dispose()` bypasses Swing's repaint machinery: it can tear on some platforms, it allocates a new `Graphics` per frame, and it ignores the component lifecycle (no paint coalescing, no correct behaviour when the OS asks for a repaint — e.g., window un-minimize shows black until the next tick). The double-buffer via `tempScreen` is right; the final blit should happen in `paintComponent`.

**Fix:** `@Override protected void paintComponent(Graphics g)` → `g.drawImage(tempScreen, 0, 0, screenWidthFull, screenHeightFull, null)`; call `repaint()` at the end of frame draw (or keep the active-rendering shape but only via `paintComponent`), and drop `getGraphics()`.

### 4.8 `P3` — Sorting and drawing use-magia constants, magic numbers everywhere
`GamePanel.ORIGINAL_TITLE_SIZE` (typo, "TITLE" not "TILE"), `TILE_SIZE * 5` fudge in `inCamera()`, `KNOCK_BACK_TIME`/`SPRITE_CHANGE_TIME` scattered, `20` duplicated as inventory cap, `3000` day-length, `0.001F` filter ramp… Give them names and one home (`Constants` or the owning class) — a future tuning pass over "game feel" currently means grepping literals.

### 4.9 `P3` — `Lighting` re-renders the darkness filter only when told, but draws text lookups every frame
`setLightSource()` correctly only runs on `lightUpdated` — good. But `draw()` does two `TextManager` map lookups + a `deriveFont(50F)` per frame for a debug string almost never shown (end-credits only). Guard the whole block behind the ending-scene condition and hoist the font.

---

## 5. Build, dependencies, tooling

### 5.1 `P1` — Lombok plugin version vs Gradle 8.5, and no Java toolchain pinned
**Where:** `build.gradle.kts`, `gradle/wrapper/gradle-wrapper.properties`

- `io.freefair.lombok` **5.3.0** (Nov 2020, built for Gradle 6.x) paired with Gradle **8.5** is asking for trouble; you also redundantly declare `compileOnly`/`annotationProcessor` lombok yourself (good), so the plugin adds risk with little gain. **Drop `io.freefair.lombok`** or upgrade to ≥ 8.x.
- **No `java { toolchain { languageVersion = ... } }`** anywhere. The code uses `List.getFirst()` (`GameEntity.searchPath`, `GameMap`? and `EventHandler.handleInteriorMapEvents` `getFirst()`) — a **Java 21** `SequencedCollection` API. With no toolchain, the build compiles against whatever JDK the CI/dev box happens to have; on JDK 17 it fails, on JDK 21 it silently relies on unspecified defaults (source/target 21 with `-source`/`-target` mismatch warnings). Pin `languageVersion = JavaLanguageVersion.of(21)` and set `options.encoding = "UTF-8"`.
- `group = "org.example"`, `version = "1.0-SNAPSHOT"` — rename to the real coordinates (`org.savilusgame:blue-boy:<version>`) and start versioning properly; `save.dat`-style artifacts make releases untrackable.
- `tasks.test { useJUnitPlatform() }` is configured but there are **zero tests** (see Section 10) — the line is aspirational today.

### 5.2 `P1` — Outdated/vulnerable dependencies
| Dependency | Current | Why change |
|---|---|---|
| `org.apache.commons:commons-lang3:3.0` | 2011 | 13+ years old; call sites (`StringUtils.equals`, `EMPTY`) are covered today, but pin ≥ 3.17.0 — you also only use ~4 methods; consider dropping it for `java.util.Objects`/`String` APIs. |
| `com.fasterxml.jackson.core:jackson-databind:2.15.0` | 2023 | 2.15.0 is affected by known databind CVEs (fixed in 2.15.4+). Move to 2.17.x/2.18.x. |
| `org.yaml:snakeyaml:2.2` | OK | Fine; keep current when bumping others. |
| `org.slf4j:slf4j-api:2.0.9` + `logback-classic:1.4.12` | OK | Keep; add a `logback.xml` (5.3). |
| `io.vavr:vavr:0.10.5` | Unmaintained but stable | Acceptable; note the project is effectively dormant — isolate its use (it's used for `Try` only) behind thin helpers so it can be removed later if desired. |

Also: **no `logback.xml`** on the classpath — logback's default configuration prints **DEBUG** and above to stdout (logback default). Every `log.debug` you ever add will spam players' consoles. Add `src/main/resources/logback.xml` (console pattern + rolling file under `user.home/.blue-boy/logs`, level INFO).

### 5.3 `P2` — No packaging: `java -jar` is impossible
No `application`/`java` main-class manifest, no `Main-Class` in any jar task, no distribution (`installDist`, jpackage image, or even a fat jar). Given Section 3.12, fixing resources is step one; packaging is step two. With Gradle, the `application` plugin + `installDist` + a `jpackage` step (or a simple launcher script) gets you a distributable in an afternoon.

### 5.4 `P3` — No static analysis / formatting enforcement
`src/main/resources/code-style-formatting.xml` is an IDE config **inside the resources tree** (it gets copied into the jar). Checkstyle/SpotBugs/Error Prone are not wired into the build, so nothing stops regression of the very issues in this report. Add `checkstyle` (or Spotless) with that config moved to `config/checkstyle/`, run it in CI, and start with "warning only" to avoid a big-bang failure.

---

## 6. Robustness, error handling and threading

### 6.1 `P0` — The game thread dies silently on any exception; the window stays up frozen
**Where:** `GamePanel.run()` — no `try/catch` around `update()`/`drawToTempScreen()`

Every P0/P1 bug above (missing map key → NPE, corrupt save, null tile…) propagates out of `run()`; the thread dies; the `JFrame` remains visible with its last frame painted. The player sees a frozen game, nothing in the console, nothing in a log (there is no logger on this path), and no way to report anything. 

**Fix:** wrap the loop body in `try { ... } catch (Throwable t) { log.error("Game loop crashed", t); showCrashDialog(t); break; }` and always call `setGameState(...)`-safe cleanup. Additionally, `while (Objects.nonNull(gameThread))` never terminates on its own — introduce a `running` flag, an interrupt path, and a shutdown hook (`window.addWindowListener(EXIT_ON_CLOSE → dispose everything)`, stop music/lines, flush config/save). Also funnel the whole "world frame" through the try so a single bad frame doesn't kill the session.

### 6.2 `P1` — Cross-thread visibility of game state (EDT vs game thread)
**Where:** `utils/KeyHandler.java` (plain `boolean upPressed, …, enterPressed`), `GamePanel.gameState` (plain field, set from EDT, read from game thread), `TileManager.CURRENT_MAP` (`public static String`, mutated from game thread and read from EDT — e.g. `KeyHandler` map-reload, `UI.drawTransition`), `GameMap`/`UI` shared fields

Swing delivers key events on the **EDT**; the game loop reads the same fields from its own **game thread** with no `volatile`/synchronization. On x86 this usually appears to work; it is still a data race — updates can be cached, reordered, or lost, and any "worked on my machine" report in this area will be miserable to diagnose. `gameState` can also be half-observed (e.g., state switched to `OPTIONS_STATE` mid-iteration).

**Fix:** make the hand-off explicit and minimal — mark input flags `volatile` (they're single-writer/single-reader booleans), or better, push **input events** into a queue drained by the game thread; mark `gameState`, `CURRENT_MAP` as `volatile` or guard with the frame lock. Longer term this folds into the input-system work in 3.14.

### 6.3 `P1` — `Sound`: clips leak native lines; one shared instance cannot overlap SFX
**Where:** `Sound.java` (`setFile`, `play`, `loop`, `stop`), `GamePanel.soundEffect`

- `setFile()` does `AudioSystem.getClip(); clip.open()` **per call**. `playSoundEffect(...)` reuses one `Sound` instance; the previous `clip` reference is overwritten without `stop()`/`close()`. The old clip keeps its mixer line until GC finalizes it (and `Clip` holds native resources that are notoriously slow to finalize). Play enough sounds and you get `LineUnavailableException` / silent SFX — a classic "game loses sound after 20 minutes" bug.
- `stop()` (`clip.stop()`) NPEs if nothing was ever played (`clip == null`); `music.stop()` is called in `changeArea()` before any music may have been set in some flows.
- `checkVolume()` has no `default` in the `switch` — an out-of-range `volumeScale` leaves `floatControl.setValue(0f)` (0 dB = full scale) or, worse, an `IllegalArgumentException` if a corrupt config wrote a crazy value.
- The `FloatControl` cast assumes every mixer supports `MASTER_GAIN` (most do; some don't — the `Try` in `setFile` wraps it, but the error message is opaque).
- WAV assets are uncompressed PCM (41 MB for 4 tracks, 9–11 MB each) — fine for 15 s SFX, wrong for looped music (see Section 9).

**Fix:** cache one open `Clip` **per sound id** (`Map<SoundId, Clip>`), rewind with `setMicrosecondPosition(0)`, and `close()` music clips on switch. Use a small pool (or a dedicated `SoundEffect` player that opens-plays-closes per shot, since short clips decode fast). Null-guard `stop()`. Clamp `volumeScale` 0–5 at the setter. Convert WAV music to OGG/MP3 (`javax.sound` won't read OGG — either ship shorter/compressed WAV, use SPI (e.g., `vorbisspi`), or switch playback to `javax.sound` with MP3 via SPI — simplest: keep WAVs for SFX, transcode music to smaller sample rate/bitrate WAV or accept the SPI dependency).

### 6.4 `P1` — `draw()` mutates game state; render and update are not cleanly separated
**Where:** `GamePanel.drawToTempScreen` (fine), but also:
- `GameEntity.draw()` → `dyingAnimation()` increments `dyingCounter` and flips `alive = false` — death progresses inside rendering, so a paused or skipped frame never finishes a death, and a slow frame advances death twice.
- `UI.drawOptionsScreen()` / `drawTradeScreen()` call `gamePanel.getKeyHandler().setEnterPressed(false)` — input consumption inside render.
- `UI.drawTransition()` switches `CURRENT_MAP` (a static!), teleports the player and creates a new world map — inside `draw`.
- `CutsceneManager.draw()` and `UI.drawSleepScreen()` advance counters and force `setGameState(PLAY_STATE)` from the render path.

**Fix:** move all counter/state advancement into `update()`. Keep `draw*` strictly side-effect-free (read state, paint). This is the same separation you already practice with `update()` vs `drawToTempScreen()` — extend it consistently. It pays off immediately (correct pause behaviour) and is a prerequisite for any future rendering on a separate thread.

### 6.5 `P2` — Knockback/invincibility timers and `invincibleCounter` duplicated with different constants
`GameEntity.INVINCIBLE_TIME = 40` vs `Player.INVINCIBLE_TIME = 60`; `GameEntity.KNOCK_BACK_TIME = 5` vs inline `5` in `Player`; `SPRITE_CHANGE_TIME` vs `CHANGE_SPRITE_INTERVAL`. Two parallel implementations of the same mechanics (see 3.15) — same class of drift will hit any future status effect. **Fix:** single implementation, single constants table (or per-entity overridable fields with one code path).

### 6.6 `P2` — `getDetected()` uses the *player's* speed for any user entity
**Where:** `GameEntity.getDetected` — `int playerSpeed = getPlayer().speed;`

Used by `Key.use()` with `gameEntity == player` today, but any future non-player caller (e.g., an NPC-chave or a spell) silently checks the wrong reach. **Fix:** `user.getSpeed()`.

### 6.7 `P3` — `INIT_INDEX = 999` sentinel
Magic-number "not found" across 22 call sites. Use `-1` (unambiguous) or `OptionalInt`; fail loudly elsewhere (`if (idx == -1)`). Sentinels invite "forgot to check" bugs — 3.6 is exactly that.

### 6.8 `P2` — `Objects.requireNonNull(getClass().getResourceAsStream(...))` inside `Try`, then `.getOrElseThrow(...)`
`Sound`/`setup()` convert a *missing resource* (a build/packaging error) into a runtime exception with a decent message — that part is fine — but the same pattern in `YamlConfigLoader` (`RuntimeException("Config file not found...")`) bypasses the `Try` entirely and gives an inconsistent error model. Pick one strategy (unchecked `IllegalStateException` with the resource id, always with a message) and apply it uniformly; swallow-and-log is only acceptable for *optional* assets.

### 6.9 `P3` — `Window setUndecorated(true)` for fullscreen after `setResizable(false)` etc.
Ordering in `Main.java` is okay-ish, but the fullscreen path is untestable and undecorated-only; consider `GraphicsDevice.setFullScreenWindow` (exclusive mode) or simply maximized-bordered; at minimum document why undecorated is required. Also `Main.window` is a **public static mutable** — pass the frame explicitly or keep a package-private holder.

---

## 7. Save/load, persistence and data integrity

(Engineering-level details in 3.7–3.10; this section is the policy level.)

### 7.1 `P1` — No save versioning; no migration path
First field of the new save DTO must be `schemaVersion`. Even if you only ever ship one version, writing it now costs nothing and it future-proofs every fix in Section 3.

### 7.2 `P1` — Save writes only on two implicit triggers
The game auto-saves at the healing pool (`EventHandler.healingPool()` → `setMonster()` + `save()`) and nowhere else (no save on quit/game-over retry). The title-screen "Load Game" entry point exists (`KeyHandler.titleState`) — with no save file, `load()` logs an error, then starts `PLAY_STATE` anyway (misleading UX: "Load Game" silently begins a fresh run instead of telling the player "no save found").

**Fix:** explicit save slot(s) (even a single `save.json`), save-on-quit prompt, "Load" disabled/greyed when no save exists, and an in-game "saved" toast so the player knows the healing-pool save happened.

### 7.3 `P2` — Interactive-tile save is index-based
`SaveLoad` persists tile state as *parallel lists* aligned to the current `AssetSetter.setInteractiveTiles()` order. Reordering the list in code (trivial when adding a new destructible) corrupts every existing save silently — tiles resurrect or vanish with no error. **Fix:** persist by stable key: `mapId → list of {tileTypeId, col, row, currentLife, destroyed, invincible}` and rebuild by looking cells up, skipping unknown ids with a warning.

### 7.4 `P2` — Objects-on-map save is also index-based, and half-implemented
`load()` clears the map list and re-adds only non-null entries; `NA`/`null` holes are simply dropped (the `objectsOnMap.add(null)` line is commented out). That's actually consistent today (lists are horizontal-compacted), but it still: (a) only covers the current map (3.7), (b) assumes saved indexes match the freshly-`clear()`ed list, (c) loses chest *contents* for chests emptied this session (only `opened` is stored — fine, by design), and (d) doesn't store monster respawn state (acceptable design — respawning is a feature — but document it).

### 7.5 `P3` — `save.dat` and `gameConfig.txt` are committed to git
See Section 9.

---

## 8. Architecture and design

### 8.1 `P1` — `GamePanel` is a god object; `GameEntity` is a god base class
**Where:** `GamePanel.java` (entire), `entity/GameEntity.java` (759 lines, ~60 fields)

`GamePanel` simultaneously is: Swing component, game-loop owner, renderer, world state (`mapsObjects`, `mapsMonsters`, …), façade for every subsystem (`getPathFinder`, `getCollisionDetector`, `getEventHandler`, …), and the dependency everyone takes in their constructor. Every entity reaches the world **only** through it — that's 60+ call sites of `gamePanel.getX().getY()` and makes the engine untestable without a fully wired panel (impossible to unit test `Player` or `SkeletonLord` with a stub today).

`GameEntity` mixes: position/velocity, sprite sheets, attack animation state, parry windows, knockback, invincibility, inventory (`ArrayList<GameEntity> inventory` — every slime carries an `ArrayList`), mana/ammo/exp/level, dialogue state, loot, light radius, boss flags, `worldX/worldY`, a `type` enum, plus methods from `draw()` to `damagePlayer()` to `generateParticle()`. Items, projectiles, NPCs, tiles-as-entities and the player are all one class hierarchy with protected fields. This is the "Refs tutorial" shape — fine to *learn* with, expensive to *extend*.

**Fix (incremental, no big bang):**
1. Introduce a **`World`/`GameSession`** service object owned by `GamePanel` that owns the map-scoped entity lists, `CURRENT_MAP`, area, and boss flags — with methods (`entitiesOn(map)`, `spawnMonster(...)`, `dropLoot(...)`) instead of public mutable maps. `GamePanel` keeps the Swing/loop responsibilities only.
2. Split `GameEntity` along the axis it already secretly has: an **`Item`** family (stackable, price, description — no physics, no AI) and an **`Actor`** family (health, stats, movement, collision). Interactive tiles implement `Destructible`/`Interactable` interfaces rather than inheriting 60 fields. Keep the single `GameEntity` base *temporarily* and migrate one family at a time; the goal is that `new RedPotion()` doesn't drag in `attackArea`, `exp`, `knockBackPower`.
3. Constructor-inject **interfaces** (`AudioService`, `TextRepository`, `ItemCatalog`) rather than `GamePanel`. Even one interface (e.g., `EffectPlayer`) enables headless unit tests and breaks the worst coupling.
4. `TileManager` singleton (`getInstance`) → constructor injection like everything else; delete `public static String CURRENT_MAP` (see 8.2).

Do not attempt a full ECS rewrite — it's unjustified at this scale. The point is: **make the seams**; the game stays the same, the tests and the team start to flow.

### 8.2 `P1` — Global mutable state: `TileManager.CURRENT_MAP` (public static), `Main.window` (public static), `Progress` (public static boolean)
**Where:** `tile/TileManager.java:56`, `Main.java:14`, `data/Progress.java`

`CURRENT_MAP` is read and written from `GamePanel`, `TileManager.draw`, `UI.drawTransition`, `CollisionDetector`, `EventHandler`, `PathFinder`, `GameEntity`, `AssetSetter`, `SaveLoad`, `KeyHandler` — a dozen classes reaching into one static cell. It prevents: multiple worlds, map previews, cutscene maps (PlayerDummy already teleports!), replay/testing, and safe TDD. These are all things this project *will* want.

**Fix:** pass the current map id through the world object (8.1); `CURRENT_MAP` becomes `world.currentMapId()`. Delete `public static`. `Main.window` → inject into `GamePanel` (fullscreen needs screen size, not the window). `Progress` → save DTO (3.10).

### 8.3 `P2` — Collections of entities are open to every class, null-keyed by map id
`mapsObjects`/`mapsMonsters`/`mapsNpc`/`projectiles`/`mapsInteractiveTiles` are public fields (via `@Getter`) typed `Map<String, List<GameEntity>>`, populated piecemeal by `AssetSetter` with **inconsistent key coverage** (no INTERIOR_MAP for objects/monsters, no SECOND_FLOR for NPCs…), then null-checked at every call site with subtly different patterns. This is the structural cause of 3.6. **Fix:** one registry class (`WorldRegistry`) built from an explicit `List<String> mapIds`, always fully populated; absence is impossible by construction — not by discipline.

### 8.4 `P2` — The `UI` class is a 1,067-line renderer+controller for ~12 screens
Title, HUD, pause, character sheet, inventory (player & NPC), trade (buy/sell/select), options (settings/fullscreen/controls/end-game), dialogue, sleep transition, game-over, map, transition, debug — all in one class with a `Graphics2D graphics2D` field stashed on each `draw()`. Split into **screen classes** implementing e.g. `interface Screen { void draw(Graphics2D g, GamePanel p); }` with a `UiState`/`ScreenManager`; share `drawSubWindow`, fonts, and `getXForCenteredText` via a `UiKit`. This removes merge-conflict hell and makes each screen independently testable (at least construction-tested) — and it pairs naturally with replacing `GameState` switches scattered across `KeyHandler`/`UI`/`GamePanel` with a state-handler map (keep the enum; dispatch table instead of 10 switches).

### 8.5 `P2` — Assets are placed in Java code
`AssetSetter` hard-codes ~80 entity placements (coordinates, loot chains) as `new X(gamePanel).setWorldPosition(TILE_SIZE * 25, TILE_SIZE * 23)` lines; `Lighting`, `EnvironmentManager`, day length… World design (spawn points, trigger volumes, puzzle wiring) buried in code means every level tweak is a recompile and review. For a game that will grow, move **entity placement and events into data** (JSON/YAML alongside the map files — e.g., `maps/worldV3.entities.json`: `{type, x, y, loot, …}`), keep a thin loader. This also fixes 8.3 partially (placement defines the complete map set) and makes the a designer/mods workflow possible.

### 8.6 `P2` — Duplicated A\* demo package ships with the game
**Where:** `src/main/java/aStarPathfindingAlgorithmExample/` (`Main`, `DemoPanel`, `KeyHandler`, `Node`)

A standalone A\* teaching demo, duplicating `org.savilusGame.ai.Node`, living in the production source set (compiled into the game jar, warns in every build, confuses new devs about what's real). **Fix:** delete it, or move to `examples/` as a separate source set / separate module with its own run config. Same for `maps/map01.txt`, `maps/world01.txt`, `maps/worldV2.txt` (unreferenced — `MAIN_MAP` is `/maps/worldV3.txt`), `objects/chest (OLD).png`, and the unused `world.tile.images.sand` YAML key (points at a non-existent `/tiles/sand.png` — 204 keys check found exactly one missing path, this one; please fix the file or delete the key).

### 8.7 `P3` — Package naming and structure
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

### 8.8 `P3` — Concrete design-pattern notes (where patterns help, where they don't)
- **Flyweight / cache**: sprites & fonts (4.6, 4.3).
- **Factory / registry**: `GameEntityFactory` is the right idea — make it data-driven (`Map<GameObject, Supplier<…>>` built once — it already is), keyed by stable id (3.9), with duplicate-mapping validation at startup (it would have caught 3.5).
- **State pattern**: `GameState` + switches in 4 places → dispatch table or State objects when a third switch appears; today a table suffices.
- **Observer**: `EventHandler`/`checkEvent` is a poor-man's observer; a tiny `EventBus` (or direct calls) is fine at this scale — don't over-engineer.
- **Strategy**: monster AI (`setAction()` overrides) is already strategy-shaped; keep it.
- Avoid: full ECS, DI frameworks — the constructor wiring is fine; a hand-rolled `World`/`Services` record is enough.

---

## 9. Repository hygiene and assets

1. **`save.dat` and `src/main/gameConfig.txt` are committed to the repo** (`git ls-files` confirms). Both are runtime/user artifacts. Remove from tracking (`git rm --cached`), add to `.gitignore`, and — since history contains them — they are permanently in the pack; acceptable for a save file, but note it.
2. **41 MB of uncompressed WAV** music (9–11 MB × 4) and 250+ PNGs — the repo is ~84 MB. Use Git LFS for binary assets, or (better long-term) move assets out of git to an artifact host and fetch at build time (`gradle` task). At minimum convert music to OGG + a compact decoder SPI, or lower the sample rate/bit depth — 44.1 kHz stereo PCM is wasteful for this soundtrack.
3. **`game ss/` directory with a space** in its name, referenced from README — rename to `docs/screenshots/` and update README links; also rename `getScreenWidth()`-style typos? (no typos found in public API — good.)
4. **`code-style-formatting.xml` lives in `src/main/resources`** → move to `config/`.
5. **README is minimal** — no requirements (JDK 21), no build (`./gradlew run`), no controls list, no save location, no architecture overview, no contribution/license section. Add them; they are the front door for the "big game" phase.
6. **No LICENSE** file — as this goes public or commercial, pick one.
7. **No `.gitattributes`** — binary files (png/wav) have no merge strategy hints (`*.png binary`).
8. `.gitignore` doesn't ignore `save.dat` / `gameConfig.txt` / crash dumps / logs.
9. `lombok.config` at root with `config.stopBubbling=true` — fine; add `lombok.addLombokGeneratedAnnotation = true` so coverage/linters behave later.
10. Single squashed-looking history ("Create README.md") — fine for now; adopt PR-based flow with CI green requirement once CI exists (Section 10).

---

## 10. What's missing entirely (the checklist for "big game")

In rough priority order:

1. **Tests.** Zero at present (`src/test` doesn't exist; JUnit is declared). Start with pure-logic units that need no Swing: `PathFinder` (grid fixtures, goal unreachable, destructible walls), `CollisionDetector` (after 4.5-style isolation), `checkLevelUp` exp math, `SaveLoad` round-trip via a temp dir (after 3.8), `GameEntityFactory` mapping completeness (every `GameObject` resolvable, no duplicate suppliers), `TextLoader` on missing/corrupt files (must fail clearly), `Inventory.canObtainItem` (stackable vs full). To enable this you need the seams from 8.1 — that's the point: tests force the architecture you want anyway.
2. **CI** — GitHub Actions: `./gradlew build test` on push/PR, matrix JDK 21, plus checkstyle/spotless gate (warning-only initially) and a `save format` compat test.
3. **Packaging/distribution** — `application` plugin or `jpackage` (win/lin/mac), icons, desktop entry; version injection into the title bar (`Main.GAME_TITLE` + version).
4. **Crash reporting / logging** — `logback.xml`, rolling file log in `user.home/.blue-boy/logs`, and the crash dialog from 6.1; without this, every player report is unreproducible.
5. **Settings validation** — `Config` line-based format has no schema; corrupt values silently half-apply. Use `java.util.Properties` or JSON; validate range (volume 0–5, fullscreen bool); keep human-editable only if you document it.
6. **Input remapping & controller?** — optional, but the Key Bindings refactor (3.14) is the enabling step; don't build remapping on a raw `KeyListener`.
7. **Save UX** — save slots or at least: explicit save on quit, "Load" disabled without a save, "saved" toast (7.2).
8. **Localization framework** — beyond keyed JSON you have today (the shape is right); formalize key namespacing (`ui.*`, `items.*`), validate at startup in all shipped languages, and keep ids out of display strings (3.9).
9. **Accessibility/UX basics** — remappable keys, volume 0 = mute semantics (currently 0 → −80 dB — fine), text speed toggle for dialogue, colourblind-safe heart/mana palette (hearts vs crystals are red/cyan — mostly OK).
10. **Documentation** — architecture README (the map of Section 8.7), build/run docs, asset pipeline note, save-format doc (even one page), CONTRIBUTING.
11. **License, issue templates, PR checklist** — cheap, immediate.
12. **Artifact cleanup** — delete/move the A\* demo (8.6), prune unused maps/sprites (8.6), document the tile-id ↔ image contract (`tile[]` index ≥ 10 — magic 10 at `TileManager.setUp`).
13. **Crash-safe shutdown** — window listener: flush config/save, close audio lines, stop the loop thread (6.1).
14. **(Optional, later) threading model doc** — even if you stay single-threaded, write down EDT vs game-thread ownership rules now; it prevents an entire class of races as more devs join.

---

## 11. Suggested roadmap

**Phase 1 — Stabilize (this sprint):** 3.1–3.13 (the bug list), 4.1 (loop sleep + delta clamp), 6.1 (crash containment), 6.3 (sound lifecycle), plus `.gitignore` fixes and removing committed artifacts. Small, reviewable PRs, one bug per PR.

**Phase 2 — Make it shippable:** classpath resource loading (3.12), per-user data dir, atomic JSON save with `schemaVersion` (3.8, 7.1), `logback.xml`, crash dialog, packaging (`installDist`), CI green with a smoke build, README requirements/build docs.

**Phase 3 — Make it scalable:** seams & tests (8.1, Section 10.1), sprite/font caching (4.3, 4.6), `PathFinder` rework (4.4), `TileManager` window rendering (4.2), `CollisionDetector` purity (4.5), `TileManager.CURRENT_MAP` removal, `UI` screen split, entity placement to JSON (8.5).

**Phase 4 — Productize:** LFS for assets, localization validation, settings schema, save slots, release automation (jpackage), CHANGELOG.

Every phase is mergeable independently; nothing here requires stopping feature development for a quarter.

---

## 12. What's done well (credit where due)

- Fixed-timestep loop with an accumulator (`delta`, `drawInterval`) — the right core shape; it just needs the sleep and the clamp.
- Consistent use of `Optional`/`Objects.requireNonNull`/vavr `Try` at I/O boundaries in most (not all) places, and real `log.error` calls in `SaveLoad`/`TextLoader`/`Config` instead of empty catch blocks.
- Content-driven **text** via JSON (`texts/…`) and content-driven resource paths via `config.yml` + `@YamlValue` — a loader most small games never bother with; it's the right foundation for localization (once ids are stable, 3.9).
- `GameMap` pre-renders world map images once (good instinct) and scales the minimap ellipse clipping nicely; the lighting/day-night system with capped ramp (`filterAlpha`) is tidy; `Lighting.setLightSource()` caches the darkness filter and only rebuilds on `lightUpdated` — exactly right.
- Sprite `switch (direction)` in `GameEntity.draw()` with per-attack `temporaryScreenX/Y` offsets is clear and works for flipped attack sheets.
- Constructor wiring in `GamePanel` (all subsystems declared in one place) is *legible* — preferable to a DI container at this size.
- The project builds a real game end-to-end: shop economy, puzzles (rock/plate), boss with two phases, saves, dialogue with branching — scope discipline is visible. Keep it.
- `UtilityTool.scaleImage`, `EventRect`/`EventHandler` one-time events, `TextManager.getItemTexts` with `String.format` placeholders — small, reusable utilities done simply.
- JUnit is already declared; you're one `src/test` away from a safety net.

---

## 13. Appendix — verification notes and environment

- Sandbox used for review: no JDK available; network egress restricted to GitHub/npm/PyPI hosts — Gradle distribution could not be downloaded, so **compile/behaviour verification was not possible** and findings are static (see Section 2). Please confirm Section 5 findings with one local `./gradlew build`.
- All maps verified 50×50 (49 newlines + no trailing newline) — consistent with `maxWorldCol/Row = 50`; no off-by-one there.
- YAML resource audit: all 203 referenced paths in `config.yml` exist on disk except `world.tile.images.sand → /tiles/sand.png` (missing file; remove key or add asset).
- `git ls-files` confirms tracked runtime artifacts: `save.dat`, `src/main/gameConfig.txt`.
- `git ls-files | wc -l` = 315 tracked files; ~184 PNGs and 22+ WAVs.
- Line references are against `0511ef271dd577261e0ce74efbfaf8ad882727e1`.

---

*End of report. Happy to walk through any item, pair on the Phase 1 PRs, or sketch the `World`/`Services` split in more detail.*

*Reviewer's contact: external contractor — hand this file to the team lead.*
