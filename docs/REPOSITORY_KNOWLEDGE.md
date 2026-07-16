# Repository knowledge

This document describes the current implementation. It complements the operational rules in `AGENTS.md`, the accepted decisions in `docs/adr/`, and the measurement record in `docs/PERFORMANCE_RESULTS.md`.

## 1. Repository overview

Jigsaw Math is a native Android mental-math game. A correct answer starts a jigsaw-piece reveal; the pure reducer commits that piece only after the renderer acknowledges animation completion. Conventional UI is Compose, the board is one embedded libGDX surface, rules are pure Kotlin, structured progress is Room, preferences are DataStore, and puzzle geometry/images are generated before runtime.

The application ID and package root are `com.qtpie.simplepuzzle`. The app compiles with SDK 37, targets SDK 36, supports API 24+, and emits Java 11 bytecode. Version sources of truth are `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and the module build files.

The original product report and visual references are under `docs/reference/`. They explain the educational intent and visual direction; they are not architecture specifications.

## 2. Current module map

| Module | Responsibility | Direct project dependencies |
|---|---|---|
| `app` | Application startup, dependency wiring, Compose navigation/screens, ViewModel adapter, Android audio/haptics, renderer host | `core-game`, `core-data`, `renderer-gdx` |
| `core-model` | Immutable game/progress models, preferences model, serialized puzzle definition/manifest | none |
| `core-game` | Deterministic reducer, questions, scoring, difficulty policies, random abstraction | `core-model` |
| `core-data` | Room database/repository, DataStore repository, legacy migration boundary | `core-model` |
| `asset-pipeline` | JVM ImageIO scaling, deterministic tab topology, triangulation, hashing, JSON output | `core-model` |
| `renderer-gdx` | Android Fragment backend, command queue, mesh preparation, fixed-step simulation, pooled effects, GPU ownership | `core-model` |
| `benchmark` | Self-instrumenting Macrobenchmark and BaselineProfileRule journeys | targets the `app` benchmark artifact |

The dependency graph is deliberately one-way. In particular, `core-game` has no Android dependency, `core-data` has no UI dependency, and `renderer-gdx` has no repository or ViewModel dependency.

## 3. Application startup and navigation

`app/src/main/AndroidManifest.xml` names `JigsawMathApplication` and exports only the launcher `MainActivity`.

`JigsawMathApplication.onCreate` loads the validated generated puzzle catalog, creates one `JigsawDataContainer` with an application-owned `SupervisorJob + Dispatchers.IO` scope, and launches `JigsawDataContainer.initialize`. Initialization currently runs the guarded legacy-migration boundary.

`MainActivity`:

- extends `FragmentActivity` to support the libGDX Fragment backend;
- creates `GameViewModel` with repositories from the application container;
- enables edge-to-edge and installs `SimplePuzzleApp`;
- pauses/resumes reducer timing with Activity lifecycle;
- implements `AndroidFragmentApplication.Callbacks.exit` as a no-op.

`SimplePuzzleApp` owns a Navigation Compose `NavHost`. Routes are the `Screen` objects in `MainActivity.kt`:

```text
start -> gallery -> mode-selection -> game
start -> settings
```

Title, gallery, settings, and gameplay state are collected with `collectAsStateWithLifecycle`. `JigsawMathBackground` uses `drawWithCache`; it replaced first-frame decoding of a full-screen background image.

## 4. Game-state data flow

The authoritative rules live in `core-game`:

1. Gallery selection records the catalog puzzle for `ModeSelectionScreen`; choosing a mode makes `GameViewModel.selectPuzzle` create a resolved `GameConfiguration`, reduce `GameAction.Start`, map the immutable result into `GameUiState`, apply semantic timer directives, and queue a Room attempt.
2. `GameViewModel.onAnswerSelected` reduces `SelectAnswer` only when the reducer is awaiting an answer.
3. A wrong answer resets combo through the reducer and increments the coarse Compose shake trigger; it does not advance the puzzle.
4. A correct answer creates `PendingBoardMutation(REVEAL)` with a session-scoped ID. Puzzle Decay can create the same model with `REMOVE`. The committed `PieceSet` is unchanged until animation acknowledgement.
5. The renderer completes the reveal/removal using the package mesh and invokes `GameViewModel.onBoardMutationFinished(mutationId)`.
6. The ViewModel/reducer validate the exact mutation ID, commit the `PieceSet`, reject stale/duplicate acknowledgements, and complete the Room transaction only when an acknowledged final reveal completes the puzzle.

`DefaultGameEngine` is deterministic for a supplied `RandomSource`. `GameViewModel` currently constructs it with `SeededRandomSource(DEFAULT_GAME_SEED)`. `DefaultMathQuestionGenerator` prevents negative subtraction answers and produces four unique non-negative choices. `StandardScoringPolicy` awards `10 * combo` before the combo increments.

`PieceSet` is a value backed by a Kotlin set and is tested beyond 64 pieces. The engine accepts arbitrary positive piece counts; 30 is a property of the current generated package, not a reducer limit.

The ViewModel is still a strangler adapter: `app/model/Models.kt` contains UI-specific `Difficulty`, `MathQuestion`, `PuzzleInfo`, and `UserSettings` alongside mapping functions in `GameViewModel.kt`. Room progress writes are serialized by `enqueuePersistence`, which joins the previous job before running the next repository operation. DataStore preference writes use independent `viewModelScope` jobs.

## 5. Compose-to-renderer command flow

`GameScreen` owns the accessible HUD, answer buttons, progress, timer, and completion overlay. It passes only coarse values to `GdxPuzzleBoard`.

`GdxPuzzleBoard` obtains the Activity-scoped `PuzzleRendererHostViewModel.controller` and submits commands from keyed `LaunchedEffect` blocks:

- `SetVisiblePieces` for a coarse revealed-piece snapshot;
- `SetQuality` for a preference change;
- `CorrectAnswerEffect` followed by `RevealPiece` for a pending piece;
- `IncorrectAnswerEffect` for a new shake trigger;
- `CompletionEffect` when completion becomes true;
- `Pause` and `Resume` from lifecycle events.

`PuzzleRendererController` is a `ConcurrentLinkedQueue` plus an atomic mutation listener and retained coarse snapshot/quality/pending-operation state. `PuzzleRenderer.render` drains commands on the render thread before its fixed-step update. Mutation completion returns through the controller, is posted to the Android main looper by `GdxPuzzleBoard`, and reaches the latest Compose callback via `rememberUpdatedState`. A recreated surface replays the committed snapshot and any operation not yet reflected by a committed snapshot.

Per-frame particle positions, reveal progress, camera shake, interpolation, and delta time never enter Compose state or `StateFlow`.

`GameModeCatalog` centrally defines Classic, Time Attack, Survival, Puzzle Decay, and Combo Rush from immutable policies. `ModeTimerCoordinator` converts reducer timer directives into monotonic scheduled callbacks and coarse UI anchors. Pause/resume preserves remaining time exactly; restarts and new questions use new generations. See `docs/GAME_MODES.md`.

## 6. libGDX lifecycle and resource ownership

`GdxPuzzleBoard` creates one `FragmentContainerView` with `R.id.puzzle_renderer_container` and installs `PuzzleRendererFragment` synchronously after attachment. `PuzzleRendererFragment` calls `initializeForView` with OpenGL ES 2 (`useGL30 = false`) and two samples.

The controller is Activity-scoped so configuration/surface recreation can retain queued coarse commands without retaining an Activity in renderer code. The actual `PuzzleRenderer` and its GL resources are surface-owned.

Gameplay exit has a non-obvious ordering contract. `SimplePuzzleApp` abandons the active run/cancels its timers, finds the renderer Fragment, removes it with `commitNow` while the `FragmentContainerView` is still attached, and then pops navigation. `BackHandler` and toolbar back use that same path. The title preview uses the identical ordering before Play/Settings navigation. Removing a Compose container first previously caused `AndroidGraphics` pause synchronization to time out and terminate the process.

`PuzzleRenderer.create` owns manifest read/validation, texture creation, mesh upload, shader compilation, SpriteBatch creation, particle pool allocation, and glow-texture generation. `dispose` releases shader, both meshes, puzzle texture, SpriteBatch, and glow texture. `pause`/`resume` reset the clock.

Rendering uses a `FitViewport(1, 1)`, one puzzle texture, one preallocated static visible-piece index buffer, and a second reusable draw for an active reveal/removal. `FixedStepClock` advances at 60 Hz and clamps resumed delta. `ParticlePool` has 64 preallocated slots. Removal fades, scales, rotates, and moves the existing mesh; LOW/reduced-motion suppress optional complexity. The title configuration caps foreground rendering at 30 FPS and drives a pure deterministic preview sequence without Compose frame state.

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

Gradle tasks `:asset-pipeline:generatePuzzleAssets` and `:asset-pipeline:syncPuzzleThumbnails` write the committed package/thumbnail. `:asset-pipeline:generatePuzzleCatalog` validates `puzzles/catalog.json` against generated packages and writes `app/src/main/assets/puzzles/catalog.json`. `app:preBuild` depends on all three tasks, so hand edits to generated outputs will be overwritten.

`PuzzleMeshDataBuilder` validates contiguous indices and precomputed triangles, flips manifest Y positions into renderer coordinates, and combines pieces into reusable vertex/index arrays. Runtime performs no crop, slicing, curve generation, or triangulation.

The title uses a separate `PuzzlePreviewRendererFragment` and controller. `PreviewSequenceGenerator` creates a deterministic 20–80 percent occupancy loop, and `PreviewPackageSequence` changes real catalog packages only after the renderer reports an idle operation boundary. Reduced motion leaves a static partial board. No preview path creates a game session or persistence write; see `docs/TITLE_PREVIEW.md`.

Cosmic Journey is the only real package currently registered. Gameplay passes the selected catalog `assetRoot` through `GameScreen`/`GdxPuzzleBoard`, and title preview candidates come from the same validated runtime catalog. Follow `docs/ADDING_A_PUZZLE.md`; per-puzzle generation/sync inputs still need explicit Gradle extension.

## 8. Room and DataStore persistence

`JigsawDataContainer` creates:

- Room file `jigsaw-math.db` using `JigsawMathDatabase`;
- Preferences DataStore file `jigsaw-math.preferences_pb`;
- `RoomProgressRepository` and `DataStorePreferencesRepository`;
- `LegacyProgressMigrator`.

Room version 2 stores `PuzzleProgressEntity`, mode-aware `GameSessionEntity`, and `ModeBestEntity`. Migration 1→2 adds session mode/outcome/stat columns, preserves legacy rows, normalizes the former `puzzle-1` ID to `cosmic-journey`, and creates per-puzzle/per-mode best records. `ProgressDao` keeps score, fastest completion, combo, and mistakes as independent best metrics. Reset transactionally clears progress, sessions, and mode bests. Both schema exports are committed under `core-data/schemas/com.qtpie.simplepuzzle.core.data.progress.JigsawMathDatabase/`.

DataStore persists sound/music enablement and volume, haptics, difficulty, graphics quality, reduced motion, last selected mode, and the legacy-migration-complete flag. Enum/mode reads fall back safely when stored text is unknown.

`LegacyProgressMigrator` runs at application initialization and marks its flag after importing. The currently wired `EmptyLegacyProgressSource` imports no rows; the abstraction and its test establish idempotence but do not constitute a real legacy reader.

Room currently stores only the revealed-piece count, not exact identities or complete reducer state. Selecting a puzzle starts a fresh engine. Unlock state, coins, UI background-music mode, and optional confetti are not all persisted. Reset All Progress clears Room progress/session rows transactionally and leaves preferences intact.

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

- `core-model`: scalable `PieceSet` and mode-catalog invariants;
- `core-game`: deterministic questions plus all five mode policies, semantic timer races, acknowledged reveal/removal, completion/failure, duplicate protection, pause/resume, restart, and seed reproduction;
- `core-data`: DataStore round trip and legacy migration idempotence;
- `asset-pipeline`: package/catalog byte determinism, serialization, validation, topology, complementary boundaries, area-preserving triangulation, and reveal ranks;
- `renderer-gdx`: fixed-step clamp, particle reuse/capacity, mesh validation, mutation replay/order, callback replacement, and deterministic preview bounds/package order;
- `app`: mode selection/HUD mapping, one-shot feedback, timer cancellation/pause, renderer-delayed progression, duplicate completion, and lifecycle pause.

Device-dependent suites are `:core-data:connectedDebugAndroidTest` (Room 1→2 migration, mode sessions/bests/reset) and `:app:connectedDebugAndroidTest` (title preview and controlled title/gallery/mode/game renderer navigation).

The `app` benchmark build type is release-derived, non-debuggable, debug-signed, R8-minified, resource-shrunk, and profileable through `app/src/benchmark/AndroidManifest.xml`. Journeys cover cold animated-title startup, title-to-settings/mode/game, timed answers, Puzzle Decay reveal/removal, and timed background/resume. Existing measurements predate these additions until new benchmark results are recorded.

The benchmark source retains the emulator warning. Emulator results in `docs/PERFORMANCE_RESULTS.md` are diagnostic only. Raw generated Baseline Profile rules are R8-obfuscated and are not committed because the stable Baseline Profile plugin used during migration could not provide compatible mapping/rewrite integration for the existing AGP model.

## 12. Important entry points

| Path | Symbol / purpose |
|---|---|
| `app/src/main/java/com/qtpie/simplepuzzle/JigsawMathApplication.kt` | `JigsawMathApplication.onCreate`, data-container lifetime |
| `app/src/main/java/com/qtpie/simplepuzzle/MainActivity.kt` | `MainActivity`, `SimplePuzzleApp`, navigation and controlled renderer exit |
| `app/src/main/java/com/qtpie/simplepuzzle/viewmodel/GameViewModel.kt` | engine/UI adapter, settings/progress collection, persistence ordering |
| `app/src/main/java/com/qtpie/simplepuzzle/viewmodel/ModeTimerCoordinator.kt` | monotonic semantic timer scheduling and coarse UI anchors |
| `core-model/src/main/kotlin/com/qtpie/simplepuzzle/core/model/GameModeModels.kt` | stable IDs, policy models, catalog and tuning |
| `core-model/src/main/kotlin/com/qtpie/simplepuzzle/core/model/GameModels.kt` | actions, events, phases, immutable state, preferences |
| `core-game/src/main/kotlin/com/qtpie/simplepuzzle/core/game/GameEngine.kt` | `DefaultGameEngine.reduce` |
| `core-game/src/main/kotlin/com/qtpie/simplepuzzle/core/game/QuestionGenerator.kt` | difficulty policies and valid choices |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/JigsawDataContainer.kt` | Room/DataStore wiring |
| `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/progress/ProgressDao.kt` | atomic progress/session operations |
| `app/src/main/java/com/qtpie/simplepuzzle/ui/components/GdxPuzzleBoard.kt` | Compose/Fragment/command bridge |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/PuzzleRendererFragment.kt` | libGDX Android backend creation |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/PuzzleRenderer.kt` | resource creation, render loop, fixed-step effects, disposal |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/RendererCommand.kt` | cross-thread renderer API |
| `renderer-gdx/src/main/java/com/qtpie/simplepuzzle/renderer/gdx/PreviewSequenceGenerator.kt` | deterministic preview piece/package sequences |
| `app/src/main/java/com/qtpie/simplepuzzle/ui/components/TitlePuzzlePreview.kt` | title Fragment host and idle package switching |
| `puzzles/catalog.json` | authoritative source catalog |
| `asset-pipeline/src/main/kotlin/com/qtpie/simplepuzzle/assets/PuzzleAssetGenerator.kt` | deterministic generation and triangulation |
| `benchmark/src/main/java/com/qtpie/simplepuzzle/benchmark/` | journeys and profile generator |
| `tools/android-env.ps1` | SDK/JBR resolver and process-local environment |

## 13. Common failure modes

- **`JAVA_HOME is set to an invalid directory ...\jbr\bin`:** the environment variable points to the JBR's `bin` child instead of the JBR root. Use repository wrappers. `android-env.ps1` can recover the parent JBR without changing the machine environment.
- **Single-dash adb or Gradle flags disappear:** advanced PowerShell parameter binding consumed them. Keep `tools/adb.ps1` and `tools/gradle.ps1` on raw `$args`; use `--%` for outer-shell Gradle `-P` arguments.
- **`AndroidGraphics: waiting for pause synchronization took too long` followed by process termination:** the FragmentContainerView detached before libGDX paused. Use the controlled `leaveGameplay` ordering; never pop navigation first.
- **Benchmark journey starts on the wrong screen:** Android task state was reused. Navigation benchmarks must `pressHome`, `killProcess`, then `startActivityAndWait` in setup.
- **Room instrumentation runner class not found:** `core-data` must keep `androidx.test:runner` in `androidTestImplementation`.
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
7. For persistence changes, run host DataStore/migration tests and connected Room tests; inspect the committed Room schema diff before accepting it.
8. For jank, use the minified profileable benchmark variant and Perfetto skills. Keep raw traces ignored and record only contextualized conclusions in `docs/PERFORMANCE_RESULTS.md`.

## 15. Known limitations

- Only Cosmic Journey has a generated runtime package; the UI does not fabricate additional playable gallery entries. Coin balance and unlock state remain in-memory.
- Aggregate revealed count is persisted, but exact piece identity and reducer state are not resumed.
- Background music mode and optional confetti are UI settings but are not in `PlayerPreferences`; confetti preference is not wired to renderer commands.
- Graphics AUTO maps to MEDIUM rather than using hardware/runtime selection.
- Reduced motion uses bounded gameplay fades and a static partial title board; incorrect-answer shake is still a coarse gameplay effect.
- `IncorrectAnswerEffect` changes `shakeRemainingSeconds`, but the camera offset is only calculated in `PuzzleRenderer.create`; the current render/update path does not apply a changing shake offset.
- Manifest `revealOrder` is generated and validated but the current reducer selects randomly from hidden indices instead of consuming that order.
- Completion is represented as coarse state, so recreating `GdxPuzzleBoard` while already completed can submit `CompletionEffect` again.
- Music is composition-owned but is not explicitly paused/resumed with Activity backgrounding.
- Physical-device frame pacing, memory ceilings, thermals, high-refresh behavior, and audio codec compatibility remain unmeasured.
- Generated Baseline Profile output is not integrated into app source.
- There is no CI workflow or screenshot golden suite. Room migration and repeated navigation are automated, but physical-device renderer recreation/performance evidence remains outstanding.
- `app/src/main/res/xml/data_extraction_rules.xml` retains the template backup-policy TODO.
- `docs/reference/report.typ` references image files that are absent from the checkout, so the historical PDF is not reproducible from source as-is. Poppler/PDF extraction utilities were unavailable during this consolidation; the Typst source and present references were inspected instead.

## 16. Remaining technical debt

- Replace duplicated app UI models and `generateMathQuestion` fallback with domain-first immutable screen models.
- Persist exact revealed pieces, resumable game state, coins, and unlock state with explicit Room migrations.
- Decide whether background music mode/confetti belong in DataStore and test compatibility if added.
- Make AUTO quality selection and reduced-motion behavior complete and testable.
- Apply incorrect-answer camera shake during fixed-step updates and make completion/reveal effects recreation-safe one-shot commands.
- Either consume manifest reveal ordering or remove/version that currently unused metadata.
- Add explicit music lifecycle/audio-focus handling and physical-device codec verification.
- Add CI for the verified host suite, lint, asset determinism, and debug/benchmark assembly.
- Add automated navigation/surface recreation, adaptive screenshot/accessibility, and gameplay-specific Macrobenchmark journeys.
- Integrate a stable mapped Baseline Profile when compatible tooling exists.
- Complete Android backup/data-extraction policy and localization of user-facing strings.

## 17. Relevant ADR index

- `docs/adr/0001-hybrid-compose-libgdx.md`: Compose shell plus one Fragment-hosted libGDX surface and teardown ordering.
- `docs/adr/0002-pure-kotlin-game-engine.md`: deterministic pure reducer and renderer-acknowledged progression.
- `docs/adr/0003-precomputed-puzzle-assets.md`: deterministic format-2 assets and runtime-free triangulation.
- `docs/adr/0004-android-studio-embedded-jbr.md`: Android CLI SDK discovery and dynamic bundled-JBR wrappers.
- `docs/adr/0005-room-datastore-persistence-boundary.md`: structured progress in Room, preferences/migration flag in DataStore.
- `docs/adr/0006-game-mode-policy-architecture.md`: immutable local mode policies and semantic timers.
- `docs/adr/0007-renderer-acknowledged-board-mutations.md`: unified reveal/removal identity and acknowledgement.
- `docs/adr/0008-title-preview-renderer-lifecycle.md`: decorative renderer ownership and controlled teardown.
