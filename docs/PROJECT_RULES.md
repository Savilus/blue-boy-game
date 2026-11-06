# Project Rules — Blue Boy

Standing engineering rules for this repository. They apply to every change,
large or small, regardless of who or what makes it. Where a rule has known
legacy exceptions in the tree, the exception is documented in
`docs/REFACTOR_STATUS.md`; it is not grandfathered into new code.

---

## 1. Verification & Definition of Done

* **No claim without evidence.** "Compiles", "tests pass", "coverage ≥ N%" are
  statements that must each name the exact command run and its result. If a
  gate could not run, say so explicitly — never imply it passed.
* A change is done when: the full unit suite is green, the game still starts
  and the touched flow was manually exercised, and the status doc reflects
  reality. Partial verification is reported as partial.
* Bug fixes ship with a regression test when the behaviour is testable
  headlessly. Pure-visual or timing fixes are reproduced by the nearest
  deterministic seam instead.
* Never weaken a check to land a change: no skipped tests, no widened
  timeouts, no `@Disabled` without a tracked reason, no "temp" catch blocks.
* Do not merge unreviewed structural changes on top of an unverifiable tree.

## 2. Architecture & Dependency Direction

* **Allowed layering** (imports point down, never up):
  `service` (interfaces) ← `utils`/`world`/`data` (implementations, state) ←
  `entity` (domain) ← `GamePanel`/`UI`/`bootstrap` (orchestration, drawing).
  `config`, `enums`, `constants` are leaves used by all.
* **Contracts over casts.** Code that needs "anything collidable" depends on
  `CollisionBody`, never on `Actor`/`InteractiveObject` concretes. The same
  rule applies to dialogue (`DialogueSpeaker`), particles (`ParticleSpec`),
  movement (`Movable`), trading (`Merchant`-like guards) and any future seam.
* **No god classes.** A class that grows a second unrelated responsibility
  gets split before it gets bigger; ~450–500 lines is the tripwire for review.
* **No static mutable state.** Static may hold only immutable constants and
  self-contained utilities. Runtime state lives in `World` (map-scoped),
  `GamePanel` (loop), or a service. Static initializers must not call into
  other classes' side effects (see §6 for the class-init rule).
* **No circular class dependencies.** Never reference another class from a
  static initializer of this class if that class can transitively read this
  class's constants. Break the cycle with literals or lazy accessors.
* Prefer constructor injection of `GamePanel`'s seams; new code never adds a
  new parameter of bare `GamePanel` where a service/interface suffices.

## 3. State, Lifecycle & Save Data

* Map-scoped state is owned by `World` only; tiles, entities and item drops
  are placed through `AssetSetter`, never ad-hoc in random classes.
* **Persisted vs temporary spawns are disjoint.** Runtime-spawned objects
  (loot drops, cutscene props) are flagged `temporaryObject`; the save layer
  excludes them and map reset clears them. Nothing temporary ever reaches
  disk.
* Save format changes are **additive only** until a versioned migration tool
  exists: new fields/keys may be written, existing ones are read
  backwards-compatibly, restore is hardened (unknown/absent entries degrade
  gracefully; a corrupt save must never throw on the game thread).
* Map switching preserves invariants: state is fully built before any update
  tick observes it; clearing is explicit, never implicit by GC timing.

## 4. Resources & Build Hygiene

* Resources are loaded from the **classpath** (or a single, injected
  resource root). New filesystem-relative paths (`src/main/resources/...`,
  cwd-dependent) are forbidden; existing ones are migrated opportunistically,
  never copied.
* Build artifacts, `save.dat`, screenshots-of-record and dependency jars do
  not enter Git. `.gitignore` is the source of truth for that boundary.
* Gradle is the build system of record; `gradlew` stays the entry point.
  Custom compile/test scripts may wrap it for agents, but must not diverge
  from it in flags or classpath semantics.
* JDK baseline and dependency versions live in `build.gradle.kts`
  (currently Java 21 target, Lombok via the freefair plugin). Toolchain
  bootstrap steps are documented in `docs/REFACTOR_STATUS.md` and updated
  whenever they change.

## 5. Testing

* JUnit 5 only. Tests are deterministic, headless
  (`java.awt.headless=true`), and never depend on execution order or wall
  clock; the suite must pass from a clean checkout on a quiet machine.
* Every service seam has a test at its interface level; domain behaviour
  (damage pipeline, inventory stacking, spawn/clear, save/restore round trip)
  is tested at the smallest seam that reproduces it.
* A failing test is either fixed or escalated in the same change set — never
  left red, never muted.
* Coverage is measured, not guessed: `Player`, the boss (`SkeletonLord`),
  `CollisionService` and `World` target ≥ 70 % line coverage; new seams do
  not land without meaningful assertions.

## 6. Language & Style Conventions

* **Lombok is a convenience, not a hiding place.** Allowed: class-level
  `@Getter`/`@Setter`, `@FieldDefaults(level=..., makeFinal=...)`, explicit
  `@AccessLevel.NONE` for API boundaries, `@UtilityClass` for true
  stateless helpers, `@Slf4j`, `@RequiredArgsConstructor`. Field-level
  annotations require a reason. Generated boolean getters (`is*`) are the
  contract — never add a hand-written duplicate.
* Enums carry stable vocabularies (`GameObject`, `Direction`, `MapName`);
  display text lives in `texts/*.json|yaml` and is read through
  `TextManager`. **No display strings in code.** The `GameObject` `textKey`
  constants are an API contract with `TextManager` and the YAML — they are
  literals and must match exactly.
* Identifiers are complete words; booleans read as predicates
  (`isCollision`, `isKnockBack`, `isTemporaryObject`).
* Package-private by default; `public` is a deliberate API decision at the
  seam level, never on implementation details.
* New files follow the tree's existing ordering (package-info light, explicit
  imports, one top-level class per file) and pass the same style checks as
  the surrounding module.

## 7. Documentation & Status Tracking

* `docs/CODE_REVIEW.md` is the living defect ledger: findings are referenced
  by number, and each is either closed (with the change that closed it) or
  explicitly open with an owner and a phase.
* `docs/REFACTOR_STATUS.md` is updated whenever a phase lands: what landed,
  what is verified, what is not, and how to rebuild the gate. It never
  overstates; "unverified" is a first-class, documented state.
* Design decisions that affect more than one file get recorded in the closest
  `docs/` file (or the class's Javadoc when local). Chat history is not
  documentation.
* Standing rules (this file) change rarely and only by team agreement;
  per-task instructions belong in issues/PRs, never here.

## 8. Change Control & Review

* One concern per commit. Bug fixes are separated from refactors; mechanical
  moves are separated from behaviour changes. Commit messages state **what**
  and **why**, plus verification performed (or the explicit gap).
* Work lands on the session's assigned branch only; never on `master` /
  `test` directly. Pull requests are the unit of review; each PR keeps the
  suite green and updates both status docs when behaviour moved.
* Large deletions (dead files, dead code) are separate commits with a clear
  rationale (unreachable, superseded, or duplicate), never mixed with
  behaviour edits.
* Reviewers check three things first: the gate evidence in the PR, the
  dependency direction (§2), and the save/load + temporary-spawn invariants
  (§3). Everything else is negotiable.
