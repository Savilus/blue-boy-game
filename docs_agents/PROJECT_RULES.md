# Project Rules — Blue Boy

Standing engineering rules for this repository. They apply to every change,
large or small, regardless of who or what makes it. Where a rule has known
legacy exceptions in the tree, the exception is documented in
`docs_agents/CODE_REVIEW.md`; it is not grandfathered into new code.

---

## 1. Verification & Definition of Done

* **No claim without evidence.** "Compiles", "tests pass", "coverage of new code ≥ 85%" are
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

* **Contracts over casts.** Code that needs "anything collidable" depends on
  `CollisionBody`, never on `Actor`/`InteractiveObject` concretes. The same
  rule applies to dialogue (`DialogueSpeaker`), particles (`ParticleSpec`),
  movement (`Movable`), trading (`Merchant`-like guards) and any future seam.
* **No god classes.** A class that grows a second unrelated responsibility
  gets split before it gets bigger; ~450–500 lines is the tripwire for review.
* **No static mutable state.** Static may hold only immutable constants and
  self-contained utilities. Runtime state lives in `World` (map-scoped),
  `GamePanel` (loop), or a service. Static initializers must not call into
  other classes' side effects.
* **No circular class dependencies.** Never reference another class from a
  static initializer of this class if that class can transitively read this
  class's constants. Break the cycle with literals or lazy accessors.
* Prefer constructor injection of `GamePanel`'s seams; new code never adds a
  new parameter of bare `GamePanel` where a service/interface suffices.

## 3. Resources & Build Hygiene

* Gradle is the build system of record; `gradlew` stays the entry point.
  Custom compile/test scripts may wrap it for agents, but must not diverge
  from it in flags or classpath semantics.
* JDK baseline and dependency versions live in `build.gradle.kts`
  (currently Java 25, Lombok via `compileOnly`/`annotationProcessor`).
  Build prerequisites and setup steps are documented in `README.md` and
  updated whenever they change.
* Write and maintain code for the configured Java 25 baseline. Prefer stable
  Java 25 language and standard-library features when they improve clarity,
  safety or maintainability; do not use preview features without an explicit
  team decision or introduce modern syntax merely for its own sake.

## 4. Testing

* JUnit 5 only. Tests are deterministic, headless
  (`java.awt.headless=true`), and never depend on execution order or wall
  clock; the suite must pass from a clean checkout on a quiet machine.
* Every service seam has a test at its interface level; domain behaviour
  (damage pipeline, inventory stacking, spawn/clear, save/restore round trip)
  is tested at the smallest seam that reproduces it.
* A failing test is either fixed or escalated in the same change set — never
  left red, never muted.
* Coverage is measured, not guessed: target ≥ 85 % line coverage; new seams do
  not land without meaningful assertions.

## 5. Language & Style Conventions

* **Use Lombok to remove boilerplate by default.** Do not hand-write routine
  getters, setters, constructors, `equals`/`hashCode`, `toString`, builders or
  logger fields when an appropriate Lombok annotation expresses the intended
  API. Choose from annotations such as `@Getter`, `@Setter`,
  `@RequiredArgsConstructor`, `@AllArgsConstructor`, `@NoArgsConstructor`,
  `@EqualsAndHashCode`, `@ToString`, `@Builder` and `@Slf4j` as appropriate;
  prefer the narrowest annotation that fits. Use `@Data`/`@Value` for suitable
  data carriers, not as a requirement for services or domain objects. Do not
  generate setters that expose mutable internals or bypass invariants, or
  equality/string methods that leak state or have unsuitable semantics.
  Keep handwritten code only where behavior, validation, framework
  requirements or API constraints make generated code inappropriate. Class
  cohesion, not annotation choice, determines whether a class needs splitting.
* Enums carry stable vocabularies (`GameObject`, `Direction`, `MapName`);
  display text lives in `texts/*.json|yaml` and is read through
  `TextManager`. **No display strings in code.** The `GameObject` `textKey`
  constants are an API contract with `TextManager` and the YAML — they are
  literals and must match exactly.
* Identifiers are complete words; booleans read as predicates
  (`isCollision`, `isKnockBack`, `isTemporaryObject`).
* Package-private by default; `public` is a deliberate API decision at the
  seam level, never on implementation details.
* Keep fields in a consistent order within each class: group by visibility
  (`public`, `protected`, package-private, `private`); within each group put
  `String` fields first, then other `static` fields (constants before mutable
  fields), then instance reference fields, and finally primitive fields.
  Keep fields with related responsibilities together within a group.
* Order methods in each class by visibility: `public`, `protected`,
  package-private, then `private`. In every iteration, check the code being
  changed for compliance with this method order and update it when needed.
* Remove all unused imports from every file changed. After a change, check
  whether it leaves methods unused; remove methods that are no longer needed,
  unless they are part of a public/framework API or are invoked indirectly
  (for example by reflection, serialization, or callbacks).

## 6. Documentation & Status Tracking

* `docs_agents/CODE_REVIEW.md` is the living defect ledger: findings are referenced
  by status and current number (for example `OPEN 3` or `FIXED 3`), and each
  is either fixed or open.
* Design decisions that affect more than one file get recorded in the closest
  relevant documentation file (or the class's Javadoc when local). Chat history is not
  documentation.
* Standing rules (this file) change rarely and only by team agreement;
  per-task instructions belong in issues/PRs, never here.

## 7. Review Finding Format

* Apart from the short "How to read this report" introduction,
  `docs_agents/CODE_REVIEW.md` contains only finding descriptions under `## OPEN`
  and `## FIXED`. Do not add executive summaries, roadmaps, progress reports,
  general commentary or appendices. Resolution and verification evidence
  belong inside the relevant finding.
* Keep all `OPEN` findings at the top and all `FIXED` findings at the bottom.
  Number each list independently with consecutive positive integers starting
  at `1`, without gaps, decimals (`3.1`) or letter suffixes. These numbers
  describe the current position in a status list, not permanent IDs.
* Use the heading `### <integer>. <title> — 🔴 OPEN` or
  `### <integer>. <title> — 🟢 FIXED`. The title immediately follows the
  number; severity, when applicable, is separate metadata below the heading.
  Use the coloured indicators together with the text, never colour alone.
* After every completed agent iteration, reassess touched findings, move
  verified resolutions to the bottom `FIXED` list, and renumber both lists
  from `1`. Remaining `OPEN` entries move up to close gaps. Preserve the
  relative order of remaining entries and append newly fixed findings to
  `FIXED`; new or reopened findings join the end of `OPEN`.
* Preserve each finding's unique HTML anchor when moving or renumbering it;
  new anchors must be unique across both lists and must not be derived from
  a changeable position. Update link labels to the current status and number
  (for example `[OPEN 3](#finding-17)`) so cross-references remain unambiguous.
* `OPEN` means unresolved or awaiting verification. `FIXED` requires a
  change reference, a description of the resolution, and verification evidence
  (exact command and result, plus relevant manual checks or explicit gaps).
  A partial or unverified fix remains `OPEN`; reopening preserves the anchor,
  description and prior evidence, with the reason for reopening recorded.
* Record the reviewed revision and distinguish historical observations from
  current verification. A bulk status reset does not prove a defect still
  exists; reassess it before changing code.

## 8. General Design & Clean Code

* **One clear responsibility.** Each class represents a cohesive concept or
  service with a clear reason to change; each method works at one level of
  abstraction. Separate domain rules, persistence, rendering, input and
  orchestration. Split by responsibility, not arbitrary line counts.
* **Simple, intentional patterns.** Apply SOLID pragmatically, keep solutions
  simple (KISS), and avoid speculative features (YAGNI). Use Factory, Strategy,
  State or other patterns only when they solve a concrete variation, ownership
  or lifecycle problem; document non-obvious tradeoffs. Prefer composition to
  inheritance when there is no genuine substitutable "is-a" relationship.
  Do not introduce frameworks or abstractions just to name a pattern.
* **Consistent project structure.** Follow the existing Gradle source sets and
  package conventions; group related functionality cohesively. Keep domain
  rules independent of Swing, filesystem and audio implementations through
  narrow contracts where needed. Bootstrap wires dependencies; domain code
  must not reach back into bootstrap or UI. Do not mix competing layouts or
  undertake unrelated package migrations.
* **Encapsulate invariants.** Validate external input at boundaries, use
  domain types instead of ambiguous primitives where useful, prefer immutable
  values, and expose only the operations callers need. Do not leak mutable
  collections or use nullable/invalid state where a valid state is required.
* **Clear code, one source of truth.** Use intention-revealing names, small
  cohesive methods and guard clauses rather than deeply nested flows. Reuse
  existing logic and centralize duplicated business rules without merging
  unrelated code merely because it looks similar. Replace meaningful magic
  values with named constants in the owning component. Comments explain
  reasons and constraints, not what readable code already says.
* **Explicit failures and resource ownership.** Catch only errors the boundary
  can handle; never silently swallow failures or report success after failure.
  Include actionable context in diagnostics without sensitive data. Close
  owned resources deterministically and make thread/state ownership explicit.
  Rendering must not advance domain state.
* **Focused, behaviour-preserving changes.** Separate refactors from intentional
  behaviour changes, keep unrelated edits out, and protect touched contracts
  with regression tests. Optimize measured bottlenecks, not guesses; avoid
  blocking I/O and unnecessary allocation in update/render hot paths.

## 9. Agent Branches, Commits & Pull Requests

* **Agents must never merge into `main`, `master`, or the repository's default
  branch.** This includes merge, squash, rebase-and-merge, enabling auto-merge,
  joining a merge queue, or pushing changes directly to that branch. Agent
  delivery stops at commits on a working branch and an open pull request;
  final review and merging belong to a human.
* Agent-created branches use `feature/<feature-name>` for a feature,
  `bugfix/<bug-description>` for one fix, or `bugfix/<fixes-summary>` for
  multiple fixes. Use short, descriptive lowercase kebab-case names, such as
  `feature/inventory-sorting`, `bugfix/right-wall-collision`, or
  `bugfix/save-and-collision-fixes`.
* Every agent-created PR has a concise, concrete description, not just a
  generic summary or a list of changed files. For a feature, include
  `## Feature` (what capability was added) and `## Added` (the main additions).
  For a bugfix, include `## Bug` (the previous incorrect behaviour) and
  `## Fix` (how the change corrects it). For multiple fixes, pair each bug
  with its resolution; if both features and fixes are included, use both
  sets of headings. A short sentence or bullet per item is sufficient.
* Include `## Verification` in the PR with the exact checks and results,
  explicitly identifying anything not verified. Do not claim successful
  compilation, tests or manual checks without evidence.
