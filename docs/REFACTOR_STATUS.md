# Refactoring Status — Blue Boy

**Branch:** `arena/bb05864e-blue-boy-game`  ·  **Base:** `0511ef2`  ·  **Updated:** 2026-11-06
**Companion docs:** `docs/CODE_REVIEW.md` (input), `docs/PROJECT_RULES.md` (standing engineering rules)

---

## 0. Environment incident this report must state up front

On 2026-11-06 the sandbox hosting the working copy was **restored from a base
snapshot (≈11:03 UTC)**. Consequences:

* Git history was truncated to the base commit. The previously reported
  Phase 0 / Phase 1 commits (`5e9ca83`, `dd3c878`) **no longer exist in the
  object store** and are not referenced by any branch.
* The working-tree contents survived: **100 % of the refactoring work from
  all phases is present as uncommitted changes** and is now committed in one
  commit (see git log). Phase boundaries could not be reconstructed reliably
  from the hunk-level diff, so the previously separate phase commits were
  intentionally **not** re-created by hand-splitting the diff.
* The Java toolchain that was bootstrapped for verification
  (`/home/user/depsrc`, JDK 21 javac, dependency jars, `compile_game.sh` /
  `run_tests.sh`) was deleted by the same restore and no compiler can be
  reacquired from inside this sandbox (see §5).

**Bottom line:** the tree is *review-clean and statically verified*; it is
**not** compiler-verified, and the JUnit suite has **not** been re-run since
the restore. Treat every "compiles / tests pass" statement from before the
restore as historical, not current.

---

## 1. What the refactoring delivers (verified by reading the current tree)

### 1.1 Service seams (`org.savilusGame.service`, new package)
`CollisionService` (checkTile/checkObject/checkEntity/checkItems/…),
`AudioService` (playSE/loopSE/stopSE/setSoundEnabled), `DialogueSpeaker`
(dialogue map + pointer + `startDialogue`), `Movable`, `ParticleSpec`,
`DialogueStarter` (shared conversation-start behaviour), `GameServiceException`.
`GamePanel` now *delegates* to the services instead of owning the logic, which
is the seam the rest of the decomposition hangs on. **Not done:** EventHandler /
cutscene paths keep direct `gamePanel` access (acceptable for now — review §8.1
is only partially addressed).

### 1.2 World extraction (`org.savilusGame.world`)
New `World` class owns map-scoped state: `mapsMonsters`, `mapsObjects`,
`mapsItems`, `mapsNpc`, `mapsCurrentInteractiveTiles`, `currentMap`, typed
collections, `setMap(MapName, isSaveLoad)`, `clearTemporaryItems()` and
`removeTemporaryObjects(...)`. `CURRENT_MAP` static field **removed** (confirmed
by grep: zero references). `AssetSetter` places entities and uses a dedicated
`drop()` helper for item drops so inventory items and world objects are no
longer stored in one raw list.

### 1.3 Entity model split — the 760-line god class is gone
`entity/GameEntity.java` is **deleted**. The concerns were split into:

| New type | Role |
|---|---|
| `entity/CollisionBody` (interface) | Structural collision contract: solid area, position, speed, direction, knockback, collision flag, `temporaryObject`. Implemented by `Actor`, `InteractiveObject`, `ItemDrop`. |
| `entity/Actor` (abstract, ~668 lines) | Everything that can move, fight and speak: stats, inventory, combat, AI cadence, draw, invincibility, `DialogueSpeaker`, `Movable`, `ParticleSpec`. |
| `entity/InteractiveObject` (abstract) | World-placed, non-character objects (doors, chests, iron doors, interactive tiles): open/loot state, `interact()` default. |
| `entity/item/Item` | Pure inventory data + `use(Actor)` hook; stable `id` (locale-independent), localized `name`/`description`, stacking, price. |
| `entity/item/ItemDrop` | The world representation of a dropped item; never solid; `temporaryObject` for save/restore hygiene; y-sorted with everything else. |
| `utils/SpriteLoader`, `utils/CameraUtil`, `utils/ParticleUtil`, `utils/ObjectDetector` | Extracted helpers (sprites/HUD icons, camera math, particle factory, object lookup without `GameObject` class-init coupling). |

`GameEntityFactory` is now **double-keyed** (`GameObject` + raw name) and builds
items and world objects from the right map. **Verified fix:** `BLUE_HEART`
mapped to `BlueHeart` (was `BlueShield`) in the factory.

### 1.4 Rendering
`GamePanel` draws a single **y-sorted `List<CollisionBody>`** with `instanceof`
dispatch (identical z-order to the legacy per-list draw loop). HUD icons
(hearts, mana crystals, coins) are `SpriteLoader` constants used by `UI`.
`UI` inventory panes take `Actor`; the merchant trade screens guard with
`instanceof Actor` before touching merchant inventory.

### 1.5 Save / load hardening (`data/SaveLoad`)
Rewritten around a **full-map object save** with an additive item-drop
persistence path (`dataStorage` gains `mapItemDropNames`/`itemAmounts` accessors;
`Progress.java` deleted). Restore is hardened: unknown/absent entries no longer
throw; temporary (runtime-spawned) objects are excluded from persistence and
cleared on map reset. **Not done** (out of scope): JSON migration, per-user data
dir, atomic write, `schemaVersion`, encryption — all review §7 items remain.

### 1.6 Test suite
`src/test/java/org/savilusGame/world/WorldTest.java` and
`service/CollisionServiceTest.java` rewritten for the new hierarchy (Actor /
InteractiveObject / ItemDrop coverage, `checkItems` trio);
`service/AudioServiceTest.java` added. 23 tests at the last run — all inside the
`test` source set with JUnit 5.

### 1.7 Review defects fixed during the final audit (this commit)
| Review ref | Fix |
|---|---|
| **3.1 P0** `CollisionDetector` RIGHT corner checked the *trailing* column | Now mirrors LEFT (`entityRightCol` for both `tileNum1/2`). |
| **3.2 P0** attack reach used `attackArea.height` horizontally | `Actor.attacking()`: `worldX += attackArea.width`. |
| **3.3 P1** entity centre computed from `up1` height/width, swapped | `getCenterX/Y()` now derive from `solidArea` (`worldX/Y + solidArea.x/y + w/h / 2`). |
| **3.4 P1** monster contact applied a stray extra `currentLife -= 1` | Removed. (Full unification onto `damagePlayer` left for a follow-up with a compiler.) |
| **3.5 P1** `BLUE_HEART → BlueShield` factory mapping | Now `BlueHeart` (verified in tree). |
| Class-init cycle `GameObject ↔ TextManager` (`ExceptionInInitializerError`, poisoned 19/23 tests) | `TextManager` static block no longer touches `GameObject`; format keys are literals keyed to `GameObject` `textKey` values — **literals verified against `enums/GameObject.java`** (`chest`, `redPotion`, `key`). |

Not fixed (unchanged, still present): 3.6+ review items, classpath resource
loading (3.12), atomic save / `schemaVersion`, crash containment (6.1),
settings schema, LFS, packaging — i.e. the entire review Phase 2/3/4 product
roadmap is untouched by this refactoring.

---

## 2. Verification log (honest timeline)

| When (UTC) | Gate | Command | Result |
|---|---|---|---|
| Pre-restore | Compile | `/home/user/depsrc/compile_game.sh` (javac 21 + lombok/vavr/commons-lang3/slf4j) | **GAME_COMPILE_OK** (all Phase 2 code incl. tests) |
| Pre-restore | Test | `/home/user/depsrc/run_tests.sh` | **TESTS_RUN=23 FAILED=19** — all `ExceptionInInitializerError` from the `GameObject↔TextManager` clinit cycle |
| Pre-restore | Fix | `TextManager` static block decoupled (§1.7) | Applied; **suite never re-ran**; toolchain deleted by the restore minutes later |
| Post-restore | Syntax | `tree-sitter-java` parse of **all 101** main+test sources (pip wheel, offline) | **0 error nodes** |
| Post-restore | Contracts | Mechanical interface-satisfaction analysis (`CollisionBody`, `DialogueSpeaker`, `ParticleSpec`, `Movable`) incl. Lombok class-level `@Getter/@Setter` expansion and superclass chains | **ALL OK** (`Actor`, `InteractiveObject`, `ItemDrop`, NPC/monster/tile hierarchies) |
| Post-restore | References | Dead-reference scan for deleted symbols (`GameEntity`, `Progress`, `gameObjects`, `getGameObjects`) | **Clean** — no references anywhere |
| Post-restore | Literal contract | `TextManager` literal keys vs `enums/GameObject.java` `textKey` values | **Match** (`chest`, `redPotion`, `key`) |
| Post-restore | Manual review | `UI.tradeBuy()` void-method `return` guard; Lombok `@UtilityClass` static ordering; boolean getter naming (`isCollision`, `isTemporaryObject`) | Sound |
| Post-restore | — | **javac / JUnit / coverage / gameplay — NOT AVAILABLE in this sandbox** | **Unverified** |

**Expected-but-unproven:** with the clinit fix, the 19 failing tests should
pass on the first re-run. That is a *prediction*, not a result. Re-run both
gates before building on this branch.

---

## 3. Architecture snapshot after the refactoring

```
GamePanel ── delegates ──► service.CollisionService ─ impl utils.CollisionDetector
        │        └──────► service.AudioService ───── impl utils.Sound (SoundPlayback)
        ├─ world.World ── owns per-map state; AssetSetter populates; TileManager pure tiles
        ├─ entity.Actor ────── characters (NPC/monster/projectile/player)
        │      implements CollisionBody, DialogueSpeaker, Movable, ParticleSpec
        ├─ entity.InteractiveObject ── doors/chests/tiles ── Interactable
        │      implements CollisionBody, DialogueSpeaker, Interactable, ParticleSpec
        └─ entity.item.Item / ItemDrop  (ItemDrop implements CollisionBody)
```

Package rules in effect: `entity/*` never imports UI; `world` owns state,
`utils` owns logic, `service` owns contracts, `data` owns persistence, `config`
owns typed settings (YAML via SnakeYAML), `enums` holds the stable vocabularies
(`GameObject`, `Direction`, `MapName`, …). See `docs/PROJECT_RULES.md`.

**Metric honesty:** `GamePanel.java` is **508 lines** (was ~760 with everything
mixed in). The brief's `< 300` target is **not met**; the remaining bulk is the
`draw()` rendering path (~150 lines) and HUD delegation wiring. Extracting a
`RenderContext` + UI screen objects is the obvious next phase and is
**deliberately not started** (see §4).

---

## 4. Remaining work / phases (not started, intentionally)

1. **Re-establish the verification gate** (§5) and re-run compile + full test
   suite; fix fallout; then measure coverage (`> 70 %` for `Player`,
   `SkeletonLord`, `CollisionService`, `World` is the brief's target).
2. **GamePanel < 300**: extract `RenderContext` (graphics + HUD primitives) and
   screen objects (`MenuScreen`, `InventoryScreen`, `ShopScreen`, …) so `UI`
   shrinks below its current 1074 lines too.
3. **Review Phase 3 seams**: pathfinder rework (A* — the tree contains a minimal
   5-line touched file, not a rework), `CollisionDetector` purity, sprite/font
   caching, `UI` screen split, `entity` placement to JSON.
4. **Review Phase 2 packaging/distribution**: classpath resource loading,
   per-user data dir, atomic JSON save with `schemaVersion`, crash dialog,
   `logback.xml`, CI smoke build.
5. **Review Phase 4 productization**: LFS assets, localization validation, save
   slots, release automation.
6. **Other review defects** (3.6–3.13) — unaudited in this pass; fix in small PRs
   with the gate green.

Nothing in §4 was started: the sandbox cannot compile Java (§5), and landing
unverifiable structural changes on top of a known-good tree would be strictly
worse than leaving it.

---

## 5. Rebuilding the verification gate (instructions)

On any machine with normal network access:

```bash
# 1) JDK 21 (OpenJDK 21.0.13) — adoptium release assets
gh release download jdk-21.0.13+9 -R adoptium/temurin21-binaries \
    -p 'OpenJDK21U-jdk_x64_linux_hotspot_21.0.13_9.tar.gz' --dir ~/tools/jdk21
# (or download from https://adoptium.net/temurin21-binaries)

# 2) Dependency jars — from Maven Central (build.gradle.kts is authoritative):
#    lombok 1.18.36, vavr 0.10.5, commons-lang3 3.0, jackson-databind 2.15.0,
#    jackson-core 2.15.0, jackson-annotations 2.15.0, snakeyaml 2.2,
#    slf4j-api 2.0.9, logback-classic/core 1.4.12, junit-jupiter (+api/params/engine), opentest4j, apiguardian
#    mkdir -p ~/tools/depsrc/lib && cd ~/tools/depsrc/lib  # curl each from https://repo.maven.apache.org/maven2/...

# 3) Compile (equivalent of the retired compile_game.sh gate)
JDK=~/tools/jdk21/jdk-21.0.13+9
$JDK/bin/javac --release 21 -parameters -proc:full -cp 'lib/*' \
    -d build/classes $(find src/main/java -name '*.java')
mkdir -p build/test-classes
$JDK/bin/javac --release 21 -cp 'build/classes:lib/*' \
    -d build/test-classes $(find src/test/java -name '*.java')

# 4) Test (headless, equivalent of run_tests.sh)
mkdir -p build/test-reports
JAVA_TOOL_OPTIONS=-Djava.awt.headless=true $JDK/bin/java \
    -cp 'build/classes:build/test-classes:lib/*' \
    org.junit.platform.console.ConsoleLauncher --scan-classpath src/test/java 2>&1 | tee build/test.log
```

Or, on a network where `services.gradle.org` and Maven Central are reachable,
the project's own `./gradlew test` is authoritative (Gradle 8.5, `io.freefair.lombok` 5.3.0, `lombok.config` → `config.stopBubbling = true`).

**This sandbox specifically cannot do either:** egress is limited to `github.com`,
`codeload.github.com`, `api.github.com`, `registry.npmjs.org`, `pypi.org`,
`files.pythonhosted.org`; release-asset downloads (Adoptium →
`release-assets.githubusercontent.com`) and Maven Central are blocked. `pypi`
`jdk4py` was tested and is **JRE-only** (no `javac`, no `jdk.compiler` module).
GitHub code search finds no vendored jars; npm has none. Do not burn time
re-attempting those paths.

**Two standing risks to re-check on first re-run:** (a) Lombok features used
here (`@UtilityClass`, `@FieldDefaults`, `@RequiredArgsConstructor`, `@Slf4j`)
need Lombok ≥ 1.18.20 and `--release 21` with an annotation processor path;
(b) the tests construct `GamePanel`, which loads YAML text/config files from
`src/main/resources` **relative to the working directory** — always run from
the repository root.

---

## 6. How to review this change set

1. Read §0 (environment) and §2 (verification) first — everything else is
   qualified by them.
2. Follow the dependency direction: `service` → interfaces only; `entity` →
   contracts (`CollisionBody`/`DialogueSpeaker`/…); `world` → state;
   `GamePanel`/`UI` → orchestration + drawing.
3. Spot-check the four review fixes (§1.7) — they are independent one-liners,
   the only behavioural changes in an otherwise move-only refactoring.
4. If the gate (§5) comes up green, the remaining risk profile is: Lombok
   codegen subtleties (visibility of generated methods is public; boolean
   getters are renamed `is*` automatically) and the y-sorted draw dispatch
   ordering in `GamePanel`.
