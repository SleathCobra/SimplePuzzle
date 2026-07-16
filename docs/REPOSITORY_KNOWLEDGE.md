# Repository knowledge

This document describes the current implementation. It complements the operational rules in `AGENTS.md`, the accepted decisions in `docs/adr/`, and the measurement record in `docs/PERFORMANCE_RESULTS.md`.

## 1. Repository overview

Jigsaw Math is a native Android mental-math game. A correct answer starts a jigsaw-piece reveal; the pure reducer commits that piece only after the renderer acknowledges animation completion. Conventional UI is Compose, the board is one embedded libGDX surface, rules are pure Kotlin, structured progress and local learning evidence are Room-backed, preferences are DataStore-backed, and puzzle geometry/images are generated before runtime.

Academy Phase 1 adds only a local formative-evidence foundation for the owner-approved Philippine DepEd MATATAG Grade 2 Quarter 1 Number and Algebra scope. Current Jigsaw items emit evidence for symbolic addition with/without regrouping. It adds no accounts, classroom, cloud, social, predictive-mastery, or Number Line Expedition functionality.

The application ID and package root are `com.qtpie.simplepuzzle`. The app compiles with SDK 37, targets SDK 36, supports API 24+, and emits Java 11 bytecode. Version sources of truth are `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and the module build files.

The original product report and visual references are under `docs/reference/`. They explain the educational intent and visual direction; they are not architecture specifications.

## 2. Current module map

| Module | Responsibility | Direct project dependencies |
|---|---|---|
| `app` | Application startup, dependency wiring, Compose navigation/screens and learner summary, ViewModel adapter, Android audio/haptics, renderer host | `core-learning`, `core-game`, `core-data`, `renderer-gdx` |
| `core-model` | Immutable game/progress models, preferences model, serialized puzzle definition/manifest | none |
| `core-learning` | Versioned skill/curriculum/activity/item contracts, immutable attempts, taxonomy validation, deterministic evidence and personal summaries | none |
| `core-game` | Deterministic reducer, addition questions, scoring, difficulty policies, random abstraction, Jigsaw learning adapter | `core-model`, `core-learning` |
| `core-data` | Room progress/learning repositories and migrations, DataStore repository, legacy migration boundary | `core-model`, `core-learning` |
| `asset-pipeline` | JVM ImageIO scaling, deterministic tab topology, triangulation, hashing, JSON output | `core-model` |
| `renderer-gdx` | Android Fragment backend, command queue, mesh preparation, fixed-step simulation, pooled effects, GPU ownership | `core-model` |
| `benchmark` | Self-instrumenting Macrobenchmark and BaselineProfileRule journeys | targets the `app` benchmark artifact |

The dependency graph is deliberately one-way. In particular, `core-learning` and `core-game` have no Android dependency, `core-data` has no UI dependency, and `renderer-gdx` has no repository, learning, or ViewModel dependency.

## 3. Application startup and navigation

`app/src/main/AndroidManifest.xml` names `JigsawMathApplication` and exports only the launcher `MainActivity`.

`JigsawMathApplication.onCreate` creates one `JigsawDataContainer` with an application-owned `SupervisorJob + Dispatchers.IO` scope and launches `JigsawDataContainer.initialize`. The container exposes progress, preferences, and learning repositories; initialization runs the guarded legacy-migration boundary.

`MainActivity`:

- extends `FragmentActivity` to support the libGDX Fragment backend;
- creates `GameViewModel` with repositories from the application container;
- enables edge-to-edge and installs `SimplePuzzleApp`;
- pauses/resumes reducer timing with Activity lifecycle;
- implements `AndroidFragmentApplication.Callbacks.exit` as a no-op.

`SimplePuzzleApp` owns a Navigation Compose `NavHost`. Routes are the `Screen` objects in `MainActivity.kt`:

```text
start -> gallery -> game
start -> settings
start -> learning
```

Title, gallery, settings, and gameplay state are collected with `collectAsStateWithLifecycle`. `JigsawMathBackground` uses `drawWithCache`; it replaced first-frame decoding of a full-screen background image.

## 4. Game-state data flow

The authoritative rules live in `core-game`:

1. `GameViewModel.selectPuzzle` creates `GameConfiguration`, calls `GameEngine.newGame`, reduces `GameAction.Start`, maps the immutable result into `GameUiState`, starts the timer, and queues a Room attempt.
2. `GameViewModel.onAnswerSelected` reduces `SelectAnswer` only when the reducer is awaiting an answer.
3. After the reducer accepts the answer, `JigsawLearningAttemptFactory` maps the current deterministic question and selected integer into one immutable attempt. `GameViewModel` queues it on a learning-persistence chain independent of score/render progression; a lifecycle/reveal callback cannot create another attempt.
4. A wrong answer resets combo through the reducer and increments the coarse Compose shake trigger; it does not advance the puzzle.
5. A correct answer moves the reducer to `REVEALING_PIECE`, assigns `pendingPiece`, updates score/combo, and exposes that pending index as `GameUiState.revealingPiece`. The piece is not yet in `revealedPieces`.
6. The renderer completes its animation and invokes `GameViewModel.onRevealAnimationFinished(pieceIndex)`.
7. The ViewModel validates phase and exact piece, reduces `RevealAnimationFinished`, maps the committed piece into UI state, saves aggregate progress, and completes the Room transaction if the final piece was revealed. This callback records no learning attempt.

`DefaultGameEngine` is deterministic for a supplied `RandomSource`. `GameViewModel` currently constructs it with `SeededRandomSource(DEFAULT_GAME_SEED)`. For the approved Phase 1 content boundary, `DefaultMathQuestionGenerator` emits addition only, generates one item seed from the engine random source, and reproduces the same operands/options from seed, generator version, content version, difficulty, and option count. The engine retains subtraction support for existing authored/test questions, but the Academy adapter rejects it. `StandardScoringPolicy` awards `10 * combo` before the combo increments.

`PieceSet` is a value backed by a Kotlin set and is tested beyond 64 pieces. The engine accepts arbitrary positive piece counts; 30 is a property of the current generated package, not a reducer limit.

The ViewModel is still a strangler adapter: `app/model/Models.kt` contains UI-specific `Difficulty`, `MathQuestion`, `PuzzleInfo`, and `UserSettings` alongside mapping functions in `GameViewModel.kt`. The obsolete UI random question fallback is removed; the core generator is authoritative. Room progress writes and learning writes use separate serialized queues so delayed evidence persistence cannot block gameplay progress. DataStore preference writes use independent `viewModelScope` jobs.

`core-learning/InitialEvidencePolicy` derives summaries deterministically for an explicit as-of time. It keeps only the latest non-invalidated attempt per template as contributing evidence, applies documented retry/assistance/recency weights, requires five distinct templates before a status, and never uses elapsed time as a status signal. `LearningSummaryScreen` exposes supportive status language, counts, variety, and recency without a mastery percentage.

## 5. Compose-to-renderer command flow

`GameScreen` owns the accessible HUD, answer buttons, progress, timer, and completion overlay. It passes only coarse values to `GdxPuzzleBoard`.

`GdxPuzzleBoard` obtains the Activity-scoped `PuzzleRendererHostViewModel.controller` and submits commands from keyed `LaunchedEffect` blocks:

- `SetVisiblePieces` for a coarse revealed-piece snapshot;
- `SetQuality` for a preference change;
- `CorrectAnswerEffect` followed by `RevealPiece` for a pending piece;
- `IncorrectAnswerEffect` for a new shake trigger;
- `CompletionEffect` when completion becomes true;
- `Pause` and `Resume` from lifecycle events.

`PuzzleRendererController` is a `ConcurrentLinkedQueue` plus an atomic reveal listener. `PuzzleRenderer.render` drains commands on the render thread before its fixed-step update. Reveal completion returns through the controller, is posted to the Android main looper by `GdxPuzzleBoard`, and reaches the latest Compose callback via `rememberUpdatedState`.

Per-frame particle positions, reveal progress, camera shake, interpolation, and delta time never enter Compose state or `StateFlow`.

Learning attempts and summaries do not cross this command flow. A correct answer can enqueue Room work and a `RevealPiece` independently; renderer completion only acknowledges reducer progression.

## 6. libGDX lifecycle and resource ownership

`GdxPuzzleBoard` creates one `FragmentContainerView` with `R.id.puzzle_renderer_container` and installs `PuzzleRendererFragment` synchronously after attachment. `PuzzleRendererFragment` calls `initializeForView` with OpenGL ES 2 (`useGL30 = false`) and two samples.

The controller is Activity-scoped so configuration/surface recreation can retain queued coarse commands without retaining an Activity in renderer code. The actual `PuzzleRenderer` and its GL resources are surface-owned.

Gameplay exit has a non-obvious ordering contract. `SimplePuzzleApp.leaveGameplay` pauses the reducer, finds the renderer Fragment, removes it with `commitNow` while the `FragmentContainerView` is still attached, and then pops navigation. `GameScreen` routes system back and toolbar back through that function. Removing the Compose container first previously caused `AndroidGraphics` pause synchronization to time out and terminate the process.

`PuzzleRenderer.create` owns manifest read/validation, texture creation, mesh upload, shader compilation, SpriteBatch creation, particle pool allocation, and glow-texture generation. `dispose` releases shader, both meshes, puzzle texture, SpriteBatch, and glow texture. `pause`/`resume` reset the clock.

Rendering uses a `FitViewport(1, 1)`, one puzzle texture, one preallocated static revealed-piece index buffer, and a second reusable draw for the active alpha reveal. `FixedStepClock` advances at 60 Hz and clamps a resumed frame delta to 100 ms. `ParticlePool` has 64 preallocated slots. LOW/MEDIUM/HIGH change reveal duration and particle limits; AUTO currently maps to MEDIUM.

## 7. Puzzle asset pipeline

The source definition is `puzzles/cosmic-journey/puzzle.json`. It references `app/src/main/res/drawable/puzzle.png`, requests a 5 x 6 board, 768-pixel active texture, 320-pixel thumbnail, and stable edge seed.

`asset-pipeline/PuzzleAssetGenerator`:

- validates IDs, dimensions, sizes, and maximum 512-piece count;
- reads source bytes and computes SHA-256;
- scales the source with Java ImageIO;
- generates deterministic complementary tab/socket boundaries from `edgeSeed`;
- triangulates each concave polygon with ear clipping;
- emits global normalized vertices/UVs, triangle indices, bounds, final position, and deterministic reveal rank;
- writes `texture.png`, `thumbnail.png`, and `manifest.json` using format version 2.

Gradle task `:asset-pipeline:generatePuzzleAssets` writes the committed package under `app/src/main/assets/puzzles/cosmic-journey/`. `:asset-pipeline:syncPuzzleThumbnails` copies the committed thumbnail to `app/src/main/res/drawable-nodpi/cosmic_journey_thumbnail.png`. `app:preBuild` depends on both tasks, so hand edits to generated outputs will be overwritten.

`PuzzleMeshDataBuilder` validates contiguous indices and precomputed triangles, flips manifest Y positions into renderer coordinates, and combines pieces into reusable vertex/index arrays. Runtime performs no crop, slicing, curve generation, or triangulation.

The current Gradle tasks and renderer default are hardcoded for Cosmic Journey. Follow `docs/ADDING_A_PUZZLE.md`; multi-puzzle generation and selected asset-root routing must be extended together.

## 8. Room and DataStore persistence

`JigsawDataContainer` creates:

- Room file `jigsaw-math.db` using `JigsawMathDatabase`;
- Preferences DataStore file `jigsaw-math.preferences_pb`;
- `RoomProgressRepository`, `RoomLearningRepository`, and `DataStorePreferencesRepository`;
- `LegacyProgressMigrator`.

Room version 2 stores existing `PuzzleProgressEntity`/`GameSessionEntity` plus `LearningAttemptEntity` and `LearningSessionEntity`. `MIGRATION_1_2` creates the learning tables and indices while leaving every schema-1 row unchanged. Both schema exports are committed under `core-data/schemas/com.qtpie.simplepuzzle.core.data.progress.JigsawMathDatabase/`.

`LearningDao.recordAttemptAndUpdateSession` inserts an immutable serialized attempt with `IGNORE` conflict handling and updates its session aggregate in one transaction only when the insert succeeds. The production repository exposes no attempt-update/delete-by-ID operation. Corrections are later append-only attempts with explicit supersession/invalidation fields. Derived skill summaries are recomputed from raw rows rather than stored as authoritative values.

DataStore persists sound/music enablement and volume, haptics, difficulty, graphics quality, reduced motion, and the legacy-migration-complete flag. Enum reads fall back safely when stored text is unknown.

`LegacyProgressMigrator` runs at application initialization and marks its flag after importing. The currently wired `EmptyLegacyProgressSource` imports no rows; the abstraction and its test establish idempotence but do not constitute a real legacy reader.

Room currently stores only the revealed-piece count, not exact identities or complete reducer state. Selecting a puzzle starts a fresh engine. Unlock state, coins, UI background-music mode, and optional confetti are not all persisted. Reset All Progress clears progress/session rows and learning attempts/sessions in one transaction while leaving preferences intact. Existing aggregate rows are preserved through migration but are never expanded into fabricated historical attempts. Android backup/transfer rules exclude the Room database for this local-only child-data pilot.

## 9. Audio and haptics

`SoundManager` owns one `SoundPool` using the application context. Effects are grouped by `SoundEffect`, loaded lazily on first request, and released from `SimplePuzzleApp`'s `DisposableEffect`. The first pending playback for an effect is played after successful load; failed loads are ignored without crashing.

`MusicManager` owns one application-context `MediaPlayer`, a `SupervisorJob + Dispatchers.Main.immediate` scope, and a single cancellable fade job. Media creation runs on `Dispatchers.IO`; transitions fade out, release, create, fade in, and loop/switch according to `BackgroundMusicMode`. Root Compose creation defers the first settings application by two frame clocks to keep media preparation off the first frame.

`SimplePuzzleApp` collects non-replayed `soundEvent`, applies the latest persisted volume, and sends Compose haptics for correct/reveal and wrong/failure feedback when enabled. Audio managers are composition-scoped and explicitly released. Music does not yet have explicit Activity background/foreground pause coordination.

## 10. Build and toolchain

Windows setup is defined by `tools/android-env.ps1`, `tools/gradle.ps1`, `tools/adb.ps1`, and `tools/android-doctor.ps1`.

- Android CLI `android info` is the SDK source of truth.
- Java is resolved separately, preferring project configuration and a valid Android Studio bundled JBR.
- Environment changes are process-local.
- `tools/gradle.ps1` is the only supported automated Gradle entry point.
- `tools/adb.ps1` is the only supported adb entry point.
- Both wrappers preserve raw `$args`; this is required for single-dash native options.
- Use PowerShell `--%` when an outer `powershell -File` invocation must forward a Gradle `-P...` property literally.

The current project versions are AGP 9.2.1, Gradle 9.6.0, Kotlin 2.2.10, libGDX 1.13.1, KTX 1.13.1-rc1, Room 2.8.4, DataStore 1.2.1, and Benchmark 1.4.1. Do not infer upgrades from this list; use the catalog and official compatibility guidance.

There are currently no workflows under `.github/workflows`; verification is local/manual until CI is added.

## 11. Tests and benchmarks

The verified host command is documented in `docs/TESTING.md` and exercises:

- `core-model`: scalable `PieceSet` invariants;
- `core-learning`: stable IDs, taxonomy structure/cycles, immutable attempt correction metadata, deterministic evidence, insufficient evidence, diversity/near-duplicates, recency, assistance/retries, and response-time exclusion;
- `core-game`: deterministic item reproduction/config identity, choice validity, Jigsaw learning mapping, scoring, combos, wrong answers, reveal separation, completion, duplicate protection, pause/resume, restart, and >64 pieces;
- `core-data`: DataStore round trip, legacy migration idempotence, and learning entity/domain mapping;
- `asset-pipeline`: byte determinism, serialization, validation, topology, complementary boundaries, area-preserving triangulation, and reveal ranks;
- `renderer-gdx`: fixed-step clamp, particle reuse/capacity, mesh combination/validation, command ordering, and callback replacement;
- `app`: reducer-to-UI mapping, correct/wrong attempt recording, delayed persistence, renderer-delayed progression without duplicate evidence, duplicate completion, and lifecycle pause.

Device-dependent suites are `:core-data:connectedDebugAndroidTest` (five Room DAO/migration/reset tests) and `:app:connectedDebugAndroidTest` (six learner-summary empty/status/semantics/large-text tests plus the package smoke assertion).

The `app` benchmark build type is release-derived, non-debuggable, debug-signed, R8-minified, resource-shrunk, and profileable through `app/src/benchmark/AndroidManifest.xml`. The `benchmark` module measures cold title startup, title-to-settings, title-to-gameplay, and incorrect-answer-to-local-summary for five iterations and contains a BaselineProfileRule journey through gameplay.

The benchmark source retains the emulator warning. Emulator results in `docs/PERFORMANCE_RESULTS.md` are diagnostic only. Raw generated Baseline Profile rules are R8-obfuscated and are not committed because the stable Baseline Profile plugin used during migration could not provide compatible mapping/rewrite integration for the existing AGP model.

## 12. Important entry points

| Path | Symbol / purpose |
|---|---|
| `app/src/main/java/com/qtpie/simplepuzzle/JigsawMathApplication.kt` | `JigsawMathApplication.onCreate`, data-container lifetime |
| `app/src/main/java/com/qtpie/simplepuzzle/MainActivity.kt` | `MainActivity`, `SimplePuzzleApp`, navigation and controlled renderer exit |
| `app/src/main/java/com/qtpie/simplepuzzle/viewmodel/GameViewModel.kt` | engine/UI adapter, settings/progress collection, persistence ordering |
| `core-model/src/main/kotlin/com/qtpie/simplepuzzle/core/model/GameModels.kt` | actions, events, phases, immutable state, preferences |
| `core-learning/src/main/kotlin/com/qtpie/simplepuzzle/core/learning/Grade2Quarter1Taxonomy.kt` | version-1 approved skills/mapping and structural validator |
| `core-learning/src/main/kotlin/com/qtpie/simplepuzzle/core/learning/LearningModels.kt` | versioned items, immutable attempts, evidence and summary contracts |
| `core-learning/src/main/kotlin/com/qtpie/simplepuzzle/core/learning/InitialEvidencePolicy.kt` | transparent deterministic Phase 1 derivation |
| `core-game/src/main/kotlin/com/qtpie/simplepuzzle/core/game/GameEngine.kt` | `DefaultGameEngine.reduce` |
| `core-game/src/main/kotlin/com/qtpie/simplepuzzle/core/game/QuestionGenerator.kt` | difficulty policies and valid choices |
| `core-game/src/main/kotlin/com/qtpie/simplepuzzle/core/game/JigsawLearningAdapter.kt` | deterministic Jigsaw item/attempt mapping |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/JigsawDataContainer.kt` | Room/DataStore wiring |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/progress/ProgressDao.kt` | atomic progress/session operations |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/progress/DatabaseMigrations.kt` | explicit `MIGRATION_1_2` |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/learning/LearningDao.kt` | append/session transaction and observation queries |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/learning/LearningRepository.kt` | entity/domain mapping and summary recomputation |
| `app/src/main/java/com/qtpie/simplepuzzle/ui/screens/LearningSummaryScreen.kt` | cautious learner-facing local summary |
| `app/src/main/java/com/qtpie/simplepuzzle/ui/components/GdxPuzzleBoard.kt` | Compose/Fragment/command bridge |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/PuzzleRendererFragment.kt` | libGDX Android backend creation |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/PuzzleRenderer.kt` | resource creation, render loop, fixed-step effects, disposal |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/RendererCommand.kt` | cross-thread renderer API |
| `asset-pipeline/src/main/kotlin/com/qtpie/simplepuzzle/assets/PuzzleAssetGenerator.kt` | deterministic generation and triangulation |
| `benchmark/src/main/java/com/qtpie/simplepuzzle/benchmark/` | journeys and profile generator |
| `tools/android-env.ps1` | SDK/JBR resolver and process-local environment |

## 13. Common failure modes

- **`JAVA_HOME is set to an invalid directory ...\jbr\bin`:** the environment variable points to the JBR's `bin` child instead of the JBR root. Use repository wrappers. `android-env.ps1` can recover the parent JBR without changing the machine environment.
- **Single-dash adb or Gradle flags disappear:** advanced PowerShell parameter binding consumed them. Keep `tools/adb.ps1` and `tools/gradle.ps1` on raw `$args`; use `--%` for outer-shell Gradle `-P` arguments.
- **`AndroidGraphics: waiting for pause synchronization took too long` followed by process termination:** the FragmentContainerView detached before libGDX paused. Use the controlled `leaveGameplay` ordering; never pop navigation first.
- **Benchmark journey starts on the wrong screen:** Android task state was reused. Navigation benchmarks must `pressHome`, `killProcess`, then `startActivityAndWait` in setup.
- **Room instrumentation runner class not found:** `core-data` must keep `androidx.test:runner` in `androidTestImplementation`.
- **Room reports an identity/schema mismatch after a learning change:** increment `JigsawMathDatabase` beyond version 2, add explicit migrations from supported versions, regenerate the committed schema, and run the migration plus DAO connected tests. Do not use destructive fallback.
- **An accepted answer appears twice in learning evidence:** keep attempt creation after reducer acceptance, use one stable attempt ID per action delivery, and verify configuration/reveal callbacks never call `recordAttempt`. Room's `IGNORE` conflict handling is the final idempotency boundary, not a substitute for correct event ownership.
- **Learning evidence reappears after Reset All Progress:** the ViewModel must stop accepting answers and join the captured learning-persistence tail before invoking the transactional Room reset. Do not merge renderer timing with persistence or block gameplay while ordinary attempts are recorded.
- **Unsupported puzzle format:** renderer rejects a manifest whose `formatVersion` differs from `PUZZLE_FORMAT_VERSION`. Regenerate with compatible code; do not bypass validation.
- **Non-triangulatable generated piece:** inspect seed/topology and the generator's strict-interior/collinear ear-clipping logic; never move triangulation to Android runtime.
- **SoundPool decode errors on the API 37 emulator:** the app remains crash-safe, but codec compatibility must be checked on physical devices before changing audio ownership or suppressing diagnostics.
- **Baseline Profile output cannot be copied directly:** raw descriptors are R8-obfuscated without compatible plugin mapping/rewrite support.

## 14. Proven debugging procedures

1. Run `android info`, then `tools/android-doctor.ps1`.
2. Run the smallest failing Gradle task through `tools/gradle.ps1`; rerun it with `--stacktrace` and diagnose the first meaningful exception.
3. Enumerate devices through `tools/adb.ps1 devices -l`. Build with Gradle, locate artifacts with process-local `android describe`, and deploy with `android run`.
4. For UI failures, use `android layout` before relying only on screenshots. Use `android screen capture` for visual evidence.
5. For renderer lifecycle failures, inspect wrapper-based logcat for `AndroidGraphics`, verify the Fragment tag `jigsaw-math-puzzle-renderer`, repeat enter/exit and background/resume, and confirm the SurfaceView is absent after leaving gameplay.
6. For asset failures, run `:asset-pipeline:test`, regenerate twice, inspect manifest version/hash/dimensions/piece count, and ensure the second run has no Git diff.
7. For persistence changes, run host DataStore/mapping tests and connected Room migration/DAO tests; inspect the committed Room schema diff before accepting it. For learning rows, verify old aggregate rows survive and no item history is synthesized.
8. For jank, use the minified profileable benchmark variant and Perfetto skills. Keep raw traces ignored and record only contextualized conclusions in `docs/PERFORMANCE_RESULTS.md`.

## 15. Known limitations

- Only Cosmic Journey has a generated runtime package. Other gallery cards reuse its thumbnail, and gameplay always creates `PuzzleRendererFragment` with the Cosmic Journey default asset root.
- The gallery catalog, coin balance, and unlock state are hardcoded/in-memory rather than manifest/repository driven.
- Aggregate revealed count is persisted, but exact piece identity and reducer state are not resumed.
- Background music mode and optional confetti are UI settings but are not in `PlayerPreferences`; confetti preference is not wired to renderer commands.
- Graphics AUTO maps to MEDIUM rather than using hardware/runtime selection.
- Reduced motion shortens the reveal and suppresses its correct-answer particle burst, but does not currently suppress every renderer effect.
- `IncorrectAnswerEffect` changes `shakeRemainingSeconds`, but the camera offset is only calculated in `PuzzleRenderer.create`; the current render/update path does not apply a changing shake offset.
- Manifest `revealOrder` is generated and validated but the current reducer selects randomly from hidden indices instead of consuming that order.
- Completion is represented as coarse state, so recreating `GdxPuzzleBoard` while already completed can submit `CompletionEffect` again.
- Music is composition-owned but is not explicitly paused/resumed with Activity backgrounding.
- Physical-device frame pacing, memory ceilings, thermals, high-refresh behavior, and audio codec compatibility remain unmeasured.
- Generated Baseline Profile output is not integrated into app source.
- Academy evidence currently covers only generated symbolic addition with/without regrouping; the other 11 approved taxonomy skills have no reviewed Jigsaw content.
- Taxonomy mappings and evidence-policy thresholds are unvalidated product assumptions, not diagnostic or mastery models.
- Summary recency is recomputed on Room emissions/screen collection; wall-clock passage alone does not push a new summary value while a screen remains continuously collected.
- Append-only attempt retention is currently until Reset All Progress or app-data removal; a broader pilot needs an educator/privacy-reviewed retention rule.
- There is no CI workflow, screenshot test suite, or automated renderer-surface recreation test yet.
- `docs/reference/report.typ` references image files that are absent from the checkout, so the historical PDF is not reproducible from source as-is. Poppler/PDF extraction utilities were unavailable during this consolidation; the Typst source and present references were inspected instead.

## 16. Remaining technical debt

- Replace the remaining duplicated app UI models with domain-first immutable screen models; the obsolete UI-only random question fallback is already removed.
- Build a manifest/repository-backed puzzle catalog and pass the selected asset root through the renderer bridge.
- Persist exact revealed pieces, resumable game state, coins, and unlock state with explicit Room migrations.
- Decide whether background music mode/confetti belong in DataStore and test compatibility if added.
- Make AUTO quality selection and reduced-motion behavior complete and testable.
- Apply incorrect-answer camera shake during fixed-step updates and make completion/reveal effects recreation-safe one-shot commands.
- Either consume manifest reveal ordering or remove/version that currently unused metadata.
- Add explicit music lifecycle/audio-focus handling and physical-device codec verification.
- Add CI for the verified host suite, lint, asset determinism, and debug/benchmark assembly.
- Add automated navigation/surface recreation, adaptive screenshot/accessibility, and gameplay-specific Macrobenchmark journeys.
- Integrate a stable mapped Baseline Profile when compatible tooling exists.
- Keep the Room backup/data-transfer exclusion reviewed as persistence scope changes, and localize user-facing strings.
- Obtain educator/curriculum review for taxonomy version 1, generated-item distributions, regrouping mapping, and evidence-policy thresholds before presenting pilot summaries as credible educational interpretation.
- Add a measured retention policy and explicit local evidence inspection/export decision before a broader pilot; Phase 1 intentionally has no automatic export.
- Make summary recency refresh explicitly at screen entry/day boundaries without polling or main-thread work.

## 17. Relevant ADR index

- `docs/adr/0001-hybrid-compose-libgdx.md`: Compose shell plus one Fragment-hosted libGDX surface and teardown ordering.
- `docs/adr/0002-pure-kotlin-game-engine.md`: deterministic pure reducer and renderer-acknowledged progression.
- `docs/adr/0003-precomputed-puzzle-assets.md`: deterministic format-2 assets and runtime-free triangulation.
- `docs/adr/0004-android-studio-embedded-jbr.md`: Android CLI SDK discovery and dynamic bundled-JBR wrappers.
- `docs/adr/0005-room-datastore-persistence-boundary.md`: structured progress in Room, preferences/migration flag in DataStore.
- `docs/adr/0006-pure-kotlin-learning-foundation.md`: pure versioned learning contracts and Jigsaw adapter boundary.
- `docs/adr/0007-append-only-local-learning-evidence.md`: Room v2 append-only attempts, derived summaries, reset and backup policy.
