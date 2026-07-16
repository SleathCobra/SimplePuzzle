# Jigsaw Math — Codex Repository Instructions

## Mission

Maintain and extend Jigsaw Math, an Android educational game whose current implementation uses:

- Kotlin
- Jetpack Compose for conventional Android UI
- libGDX with KTX for real-time puzzle rendering
- Kotlin Coroutines and StateFlow
- Room for structured progression data
- DataStore for preferences
- kotlinx.serialization for puzzle manifests
- a deterministic precomputed puzzle-asset pipeline
- Macrobenchmark, Baseline Profiles and Perfetto
- unit, integration, instrumentation and rendering tests

The Compose-to-libGDX migration is complete for the current Cosmic Journey slice. Preserve the implemented boundaries and keep the application buildable and usable after every substantial change. Do not reintroduce the removed per-piece Compose renderer or treat historical migration documents as the current architecture; use `docs/REPOSITORY_KNOWLEDGE.md` and the accepted ADRs.

Academy Phase 1 is also implemented as a local-only learning-evidence foundation for owner-approved Philippine DepEd MATATAG Grade 2 Quarter 1 addition practice. Preserve its cautious formative-use boundary: it is not diagnostic, grading, placement, predictive mastery, classroom/cloud, or social functionality. Use `docs/academy/FOUNDATION_SCOPE.md` and ADRs 0006–0007 as the source of truth.

---

# Mandatory Windows toolchain contract

## Environment facts

The repository workflow does not require or assume a standalone system JDK installation.

Java, Gradle tooling, adb, emulator binaries and Android SDK tools are supplied by:

1. Android Studio's bundled JetBrains Runtime (`jbr`)
2. the Android SDK selected by Android CLI
3. this repository's Gradle Wrapper

Never assume that any of these bare commands work:

- `java`
- `javac`
- `adb`
- `emulator`
- `sdkmanager`
- `avdmanager`
- `gradle`

Never install a separate system JDK without explicit user approval.

Never permanently modify machine-level environment variables without explicit user approval.

Never hardcode this user's absolute SDK, JDK or Android Studio paths into committed files.

## Android CLI is authoritative

Use the installed `android` CLI as the authoritative entry point for Android environment discovery and Android-specific operations.

At the beginning of every new Codex session, before running Gradle, adb, emulator or SDK commands:

1. Run:

   `android info`

2. Treat the returned path as the authoritative Android SDK root.

3. Derive tools from that SDK root:

   - adb:
     `<sdk-root>\platform-tools\adb.exe`
   - emulator:
     `<sdk-root>\emulator\emulator.exe`
   - Build Tools:
     `<sdk-root>\build-tools\<version>\`
   - command-line tools:
     `<sdk-root>\cmdline-tools\<version>\bin\`
   - platform tools:
     `<sdk-root>\platform-tools\`

4. Validate every derived executable before invoking it.

`android info` supplies the SDK path only. It does not supply the JDK path.

## JDK discovery order

Resolve the JDK independently from the SDK.

Use this order:

1. A valid project-specific Gradle JDK configured through:
   - `.gradle/config.properties` and its `java.home` property
   - Gradle daemon JVM criteria
   - an existing valid `org.gradle.java.home`
2. A valid `STUDIO_GRADLE_JDK` environment variable
3. A valid existing `JAVA_HOME`
4. Android Studio's bundled `jbr` directory found from a running Android Studio process
5. Android Studio's bundled `jbr` directory found through its Windows installation information
6. Android Studio's bundled `jbr` under known installation roots, only as a final discovery fallback

When Android Studio is running, its executable commonly allows the installation root to be derived:

- find the `studio64` process;
- read its executable path;
- move from the `bin` directory to the Android Studio installation root;
- test `<studio-root>\jbr\bin\java.exe`.

Possible fallback roots may be inspected dynamically, but must not be committed as fixed machine paths:

- `%ProgramFiles%\Android\Android Studio`
- `%LOCALAPPDATA%\Programs\Android Studio`
- Android Studio installations managed by JetBrains Toolbox
- Windows uninstall-registry entries whose display name identifies Android Studio

Every candidate JDK must be validated with:

`<candidate>\bin\java.exe -version`

The chosen JDK must also successfully run the repository Gradle Wrapper through:

`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --version`

Do not assume that the newest detected JDK is compatible. Inspect the repository's Android Gradle Plugin and Gradle versions before changing JDK or build versions.

## Required repository-local wrappers

These machine-independent PowerShell tools are part of the repository and are the only supported automated entry points for Gradle and adb:

- `tools/android-env.ps1`
- `tools/gradle.ps1`
- `tools/adb.ps1`
- `tools/android-doctor.ps1`

These scripts must contain no fixed user-specific absolute paths.

### `tools/android-env.ps1`

It must:

1. Verify that `android` is available through `Get-Command android`.
2. Run `android info`.
3. parse the actual SDK path from the command output without assuming one fixed output presentation;
4. validate that the SDK directory exists;
5. resolve Android Studio's bundled JBR using the discovery order above;
6. validate `<jdk>\bin\java.exe`;
7. set process-local:
   - `ANDROID_HOME`
   - `JAVA_HOME`
8. prepend process-local paths for:
   - `<jdk>\bin`
   - `<sdk>\platform-tools`
   - `<sdk>\emulator`
9. expose resolved paths to callers:
   - Android SDK root
   - JDK root
   - Java executable
   - adb executable
   - emulator executable
10. fail with a clear, actionable error when discovery fails.

It must not set permanent user or machine environment variables.

### `tools/gradle.ps1`

It must:

1. dot-source `tools/android-env.ps1`;
2. invoke the repository's `gradlew.bat`;
3. forward all supplied arguments unchanged;
4. return Gradle's real exit code.

Use it for all automated Gradle commands:

`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 <gradle-arguments>`

Examples:

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --version`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 lint`

Do not invoke bare `gradle`.

Do not invoke `gradlew.bat` from Codex before initializing the embedded JBR environment.

### `tools/adb.ps1`

It must:

1. dot-source `tools/android-env.ps1`;
2. invoke the exact adb executable derived from `android info`;
3. forward all supplied arguments;
4. return adb's real exit code.

Examples:

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\adb.ps1 devices -l`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\adb.ps1 logcat`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\adb.ps1 shell dumpsys gfxinfo <package>`

Do not invoke bare `adb`.

### `tools/android-doctor.ps1`

It must print and validate:

- Android CLI version
- SDK root returned by `android info`
- JDK root
- Java version
- Gradle Wrapper version
- adb version
- installed platform-tools
- installed build-tools
- connected-device status
- Android Studio CLI connection status when available

Do not print secrets.

Do not save absolute machine paths into tracked documentation.

## Toolchain verification

At the beginning of a new session, run `android info` before any Gradle, adb, emulator, or SDK command. Before changing source code or build configuration, run:

1. `android info`
2. `powershell -NoProfile -ExecutionPolicy Bypass -Command ". .\tools\android-env.ps1; android describe --project_dir=."`
3. `android sdk list "platform-tools|build-tools|platforms" --all-versions`
4. `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\android-doctor.ps1`
5. `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --version`

The process-local environment is required for `android describe` because Android CLI invokes the Gradle Wrapper and must see the resolved JBR. Record failures that existed before source changes. Documentation-only work requires the doctor plus the smallest focused host verification needed to validate any documented command; connected tests and benchmarks remain device-dependent.

## Android CLI first policy

Use Android CLI whenever it provides an appropriate command.

Use:

- `android info` for SDK discovery
- `android describe --project_dir=.` for project and output-artifact metadata
- `android docs search "<query>"` for official Android guidance
- `android docs fetch <kb-url>` for selected documentation
- `android sdk list` to inspect installed and available SDK packages
- `android sdk install` only when a required package is missing
- `android run --apks=<path>` to deploy a built APK
- `android screen capture` for screenshots
- `android layout` for the runtime layout or semantics tree
- `android studio check` before attempting Android Studio integration
- `android studio analyze-file` when supported
- `android studio find-declaration` and `find-usages` when supported
- `android studio render-compose-preview` when supported
- `android studio version-lookup` when supported

Android Studio integration commands are optional and must not block the task. They may require a compatible active Android Studio version, an open project and a working IDE connection.

When Android CLI has no equivalent operation, use the exact tool path derived from the SDK root.

Examples:

- exact `adb.exe` for device enumeration, logcat and shell commands;
- exact `emulator.exe` for an existing AVD on Windows;
- the Gradle Wrapper for builds and tests.

Android CLI's `run` command deploys an APK; it does not build the project. Always build with the repository Gradle Wrapper first.

Do not run broad SDK updates automatically.

Do not upgrade all SDK packages merely because updates exist.

Install or update only packages required by the inspected project or a documented migration requirement.

## Android skills

Use the installed Android skills when their scope matches the task.

Relevant skills include:

- `android-cli`
- `testing-setup`
- `adaptive`
- `edge-to-edge`
- `r8-analyzer`
- `perfetto-trace-analysis`
- `perfetto-sql`
- `navigation-3` only if a Navigation 3 migration is explicitly justified

Do not trigger an unrelated migration merely because a skill is installed.

The repository is already on AGP 9.2.1. Do not invoke an AGP migration solely because the `agp-9-upgrade` skill exists.

---

# Repository safety

- Inspect `git status` before editing.
- Preserve all existing uncommitted user work.
- Confirm the current branch and understand its existing changes before editing; do not switch branches without authorization.
- Never rewrite Git history.
- Never silently discard changes.
- Never delete source artwork, puzzle images or legacy data without preserving them.
- Never commit:
  - `local.properties`
  - keystores
  - signing credentials
  - API secrets
  - machine-specific SDK paths
  - machine-specific JDK paths
  - generated local toolchain reports
- Do not modify release signing unless explicitly requested.
- Prefer focused, reviewable changes over giant rewrites.
- Do not make commits unless the repository is clean enough and the user has allowed agent-created commits.

---

# Implemented module map and dependency rules

The Gradle modules declared by `settings.gradle.kts` are:

- `app`: Android application, `JigsawMathApplication`, `MainActivity`, Compose navigation/screens, `GameViewModel`, Android audio and haptics, and dependency wiring;
- `core-model`: pure Kotlin immutable domain models and the kotlinx.serialization puzzle schema;
- `core-learning`: pure Kotlin versioned identifiers, Grade 2 Quarter 1 taxonomy, learning-item/attempt contracts, deterministic evidence policy, and personal summaries;
- `core-game`: pure Kotlin deterministic reducer, question generation, scoring, seeded randomness, and the Jigsaw-to-learning adapter;
- `core-data`: Android Room/DataStore implementations, progress and learning repositories, schema export/migrations, and the one-time legacy migration boundary;
- `asset-pipeline`: Kotlin/JVM ImageIO generator and deterministic manifest/mesh/thumbnail production;
- `renderer-gdx`: Android libGDX/KTX Fragment backend, command controller, mesh upload, fixed-step simulation, pooling, rendering, and GPU-resource ownership;
- `benchmark`: self-instrumenting Macrobenchmark and BaselineProfileRule journeys targeting the `app` benchmark variant.

Allowed production dependency direction:

```text
app -> core-game -> (core-model, core-learning)
app -> core-data -> (core-model, core-learning)
app -> core-learning
app -> renderer-gdx -> core-model
asset-pipeline -> core-model
benchmark -> app benchmark artifact
```

`core-model`, `core-learning`, and `core-game` must remain free of Android, Compose, libGDX, Room, DataStore, WorkManager, network clients, and Android resources. `core-learning` must also avoid direct system-clock or random-ID access; inject `LearningClock` and `LearningIdSource` at application boundaries. `core-data` must not depend on UI or renderer modules. `renderer-gdx` must not depend on `app`, `core-game`, `core-learning`, or `core-data`. Cross-boundary behavior uses immutable models, repository APIs, reducer actions/transitions, learning adapters, and `RendererCommand`; do not bypass these boundaries with Android contexts, global mutable state, or direct database access.

Version sources of truth are `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and module build files. The current build is already on AGP 9.2.1, Gradle 9.6.0, Kotlin 2.2.10, compile SDK 37, target SDK 36, minimum SDK 24, and Java 11 bytecode. Do not run an AGP 9 or Navigation 3 migration unless a separate task explicitly justifies it.

See `docs/REPOSITORY_KNOWLEDGE.md` for symbols and detailed flows and `docs/TARGET_ARCHITECTURE.md` for the boundary diagram.

---

# Project inspection

Before substantial edits:

1. Read:
   - `settings.gradle` or `settings.gradle.kts`
   - root Gradle files
   - module Gradle files
   - `gradle/libs.versions.toml`
   - `gradle/wrapper/gradle-wrapper.properties`
   - `gradle.properties`
   - `.idea/gradle.xml` when relevant to JDK discovery
   - `.gradle/config.properties` when present
   - manifests
   - application and activity classes
   - navigation setup
   - ViewModels
   - repositories
   - persistence implementations
   - current puzzle-generation code
   - bitmap and image-loading code
   - assets
   - tests

2. Run the baseline build and tests through `tools/gradle.ps1`.

3. Record pre-existing failures separately from failures introduced by the current change.

4. Identify:
   - broad recomposition triggers
   - frame-by-frame Compose state updates
   - bitmap decoding on the main thread
   - repeated `Path` generation
   - runtime image slicing
   - runtime mesh generation
   - repeated clipping
   - large allocations
   - unstable lazy-layout keys
   - transparent overdraw
   - unnecessary blur and shadows
   - blocking persistence
   - lifecycle leaks
   - renderer-resource leaks
   - expensive composition-time calculations

---

# Architecture boundaries

## Application shell

Jetpack Compose owns:

- title screen
- gallery
- settings
- profile
- navigation
- dialogs
- gameplay HUD
- question text
- answer controls
- accessibility semantics
- lifecycle-aware coarse UI state

## Pure game engine

A pure Kotlin game engine owns:

- question generation
- answer validation
- distractor generation
- scoring
- combos
- difficulty policies
- deterministic randomization
- puzzle progression
- immutable transitions
- completion rules

The game engine must not depend on:

- Android
- `Context`
- Activity or Fragment
- Compose
- libGDX
- Room
- DataStore
- Android resources

## libGDX/KTX renderer

libGDX/KTX owns:

- puzzle-board rendering
- piece meshes
- texture coordinates
- reveal animations
- placement animations
- particles
- sparkles
- trails
- glow sprites
- frame timing
- renderer-timed effects
- renderer asset lifetimes
- graphics-quality profiles

The renderer must not:

- access Room;
- access DataStore;
- access Compose mutable state directly;
- retain an Activity;
- retain a Composable;
- decode puzzle images in `render()`;
- generate meshes in `render()`;
- perform file or network I/O in `render()`.

## Compose-to-renderer contract

`GameViewModel` owns coarse gameplay UI state. `GdxPuzzleBoard` converts state changes into commands submitted to the Activity-scoped `PuzzleRendererController`. `PuzzleRenderer` drains those commands on the render thread. A correct answer only creates a pending piece; logical progression occurs after the renderer callback reaches `GameViewModel.onRevealAnimationFinished` and the reducer accepts the matching piece/phase.

- Keep particle state, reveal interpolation, camera shake, frame delta, and other per-frame values inside `renderer-gdx`.
- Never update Compose state or `StateFlow` once per rendered frame.
- Use the `RendererCommand` queue for renderer one-shots, non-replayed `MutableSharedFlow` for app events, and callbacks for renderer acknowledgements. Validate phase/piece identity so stale or duplicate callbacks are harmless.
- Do not make the renderer reach into a ViewModel or Compose mutable state. Add a `RendererCommand` or immutable snapshot instead.
- When adding a command, test submission order and replay/duplication behavior in `renderer-gdx` and the ViewModel adapter where applicable.

## Renderer lifecycle and disposal invariants

`MainActivity` is a `FragmentActivity` and implements `AndroidFragmentApplication.Callbacks`; `exit()` must remain a no-op so the embedded libGDX backend cannot finish the Compose shell. `GdxPuzzleBoard` hosts exactly one `PuzzleRendererFragment` in a `FragmentContainerView`.

On gameplay exit, pause the game, synchronously remove `PuzzleRendererFragment` with `commitNow` while its surface is still attached, and only then pop Compose navigation. Detaching the Compose container first can make libGDX's pause synchronization time out and terminate the process. System back, toolbar back, and completion exit must use the same teardown path.

The Activity-scoped `PuzzleRendererHostViewModel` may retain only the controller across surface recreation; the renderer must not retain an Activity, Fragment, View, or Composable. Every `Mesh`, `Texture`, `ShaderProgram`, `SpriteBatch`, Pixmap-created texture, and backend resource must have one owner and deterministic `dispose()`. Lifecycle changes must pause/resume both reducer timing and renderer timing, reset/clamp accumulated delta, and tolerate surface recreation.

---

# Compose rules

- Use lifecycle-aware Flow collection.
- Use immutable UI models.
- Use stable keys and useful content types in lazy layouts.
- Keep per-frame animation data out of `StateFlow`.
- Do not model continuously animated puzzle pieces as individual composables.
- Do not decode full puzzle images during composition.
- Do not perform Room, DataStore or filesystem work during composition.
- Cache genuinely reusable drawing objects.
- Avoid large live blur effects and repeated off-screen layers.
- Use dedicated gallery thumbnails.
- Preserve touch-target sizes and accessibility descriptions.
- Support different window sizes and aspect ratios.
- Do not hardcode coordinates copied from mockup screenshots.

---

# Renderer performance invariants

Never perform the following inside the hot render path:

- bitmap decoding
- texture creation
- font creation
- mesh generation
- triangulation
- Room access
- DataStore access
- file I/O
- network I/O
- Gradle-generated asset work
- large collection creation
- unbounded object allocation
- Compose state mutation

Requirements:

- Reuse meshes and geometry.
- Batch compatible draw operations.
- Use one active puzzle texture when practical.
- Use atlases for shared effects and UI sprites.
- Pool temporary animation and particle objects.
- Clamp extreme delta times after resume.
- use deterministic coordinate mapping;
- dispose textures, meshes, audio and effects;
- handle surface recreation;
- handle pause and resume;
- avoid recurring allocation in the render loop.

---

# Coroutines

- No `GlobalScope`.
- No `runBlocking` on the UI thread.
- No unmanaged fire-and-forget jobs.
- Use structured concurrency.
- Inject clocks, dispatchers and random sources where tests require control.
- Keep Room, file and asset work off the main thread.
- Make cancellation behavior explicit.
- Do not use arbitrary delays as synchronization.

---

# Persistence

Use Room for:

- puzzle progress
- completion state
- best scores
- attempt counts
- completion timestamps
- relevant session summaries
- append-only local learning attempts and their local session summaries

Use DataStore for:

- sound enabled
- sound volume
- music enabled
- music volume
- haptics enabled
- difficulty
- graphics quality
- reduced motion

Do not store large images, textures or mesh blobs in Room.

Preserve existing Room/DataStore rows through explicit compatibility and tested migrations; use the one-time legacy import boundary only when a real legacy source exists.

Use transactions for atomic puzzle-completion updates.

The Room sources of truth are `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/progress/` and `core-data/src/main/kotlin/com/qtpie/simplepuzzle/core/data/learning/`; the committed schema export is `core-data/schemas/com.qtpie.simplepuzzle.core.data.progress.JigsawMathDatabase/`. The current database is version 2. `MIGRATION_1_2` preserves schema-1 puzzle progress/session rows and creates empty learning tables; it never fabricates item history. Any later entity/schema change must increment the database version, add an explicit Room migration from every supported version, update the exported schema, and add migration/DAO instrumentation coverage. Never use destructive migration fallback to hide a missing migration.

The DataStore source of truth is `DataStorePreferencesRepository`. Preference-key changes must preserve existing keys or implement a tested compatibility mapping. Room reset is transactional, deletes puzzle/session rows and all local learning evidence together, and intentionally does not reset user preferences. The Room database is excluded from Android cloud backup/device transfer for the local child-data pilot.

`LegacyProgressMigrator` is a one-time boundary guarded by the DataStore migration flag. The current application wires `EmptyLegacyProgressSource` because the inspected prototype had no durable legacy store. Do not claim real legacy import support or replace the source without a tested reader. Exact revealed piece identities, resumable reducer state, unlock state, coins, background music mode, and the optional confetti setting are not all durably persisted today; treat them as known limitations, not as implemented behavior.

## Local learning-evidence invariants

- Owner-approved scope is `docs/academy/FOUNDATION_SCOPE.md`; do not broaden curriculum, activity, learner, classroom, cloud, social, or educational claims without a new owner decision.
- Stable skill/activity/item identity and taxonomy/content/activity/evidence-policy versions must remain interpretable for historical attempts. Never silently reinterpret or rewrite an ID already stored.
- `LearningAttempt` is an append-only raw fact. Inserts are idempotent by attempt ID; corrections use explicit supersession/invalidation records. Do not add a production update-in-place or per-attempt destructive correction path.
- Existing aggregate puzzle progress is lower-detail legacy data. Preserve it, but never synthesize item attempts from it.
- Current random Jigsaw distractors have no reviewed misconception meaning. Do not infer a misconception tag from an arbitrary wrong choice.
- Response time may be stored when reliable but must not independently change `EvidenceStatus`.
- One accepted answer records one attempt. Configuration/lifecycle replay and `RevealAnimationFinished` must not record another attempt. Learning persistence must not block score, Compose input, or renderer timing.
- Reset must be ordered after every already-accepted attempt persistence job before the single Room reset transaction runs; otherwise a delayed attempt can repopulate evidence after deletion. Add a queue-ordering regression test when this orchestration changes.
- `renderer-gdx` remains unaware of learning contracts and persistence. Learning evidence is created at the Jigsaw adapter/ViewModel boundary after reducer acceptance.
- Personal summaries must retain an explicit insufficient-evidence state and supportive, non-permanent language. Never display diagnostic claims, child comparisons, or unexplained mastery percentages.
- Phase 1 stores no learner name/account, email, date of birth, precise location, advertising ID, contacts, photos, audio/video, free-form child content, analytics payload, or network transmission.

For taxonomy changes, follow `docs/academy/SKILL_TAXONOMY.md`. For policy changes, add a new version, deterministic tests, migration/interpretation documentation, and educator-validation status; never mutate version 1 semantics in place.

---

# Precomputed asset pipeline

Puzzle runtime assets must be generated before application runtime.

The generator must be:

- deterministic
- reproducible
- cacheable or incremental
- testable
- independent of Android runtime
- validated
- capable of clear failures for malformed source data

Generate:

- optimized puzzle texture
- gallery thumbnail
- piece vertices
- texture coordinates
- triangle indices
- final positions
- bounds
- reveal ordering
- manifest version
- source hash
- display metadata

Runtime gameplay must not:

- slice full images;
- triangulate pieces;
- create thumbnails;
- regenerate manifests.

## Generated puzzle file policy

The current representative definition is `puzzles/cosmic-journey/puzzle.json`; its source image is `app/src/main/res/drawable/puzzle.png`. `:asset-pipeline:generatePuzzleAssets` writes the committed runtime package under `app/src/main/assets/puzzles/cosmic-journey/`, and `:asset-pipeline:syncPuzzleThumbnails` writes the committed Compose thumbnail `app/src/main/res/drawable-nodpi/cosmic_journey_thumbnail.png`. `app:preBuild` depends on both tasks.

These generated files are source-controlled runtime inputs. Do not hand-edit them and do not ignore them. Regenerate them only through the pipeline, review them together with their definition/source change, and verify that a second identical run is byte-identical. Build directories, benchmark traces, APKs, local profiles, IDE state, and `app/release/` are not source artifacts and must remain ignored.

Format 2 is the current manifest format. Any change to serialized meaning, curve sampling, topology, triangulation, or coordinate interpretation requires an explicit format-version decision, generator/renderer compatibility changes, regenerated committed packages, and determinism/geometry tests.

To add a puzzle safely, follow `docs/ADDING_A_PUZZLE.md`. The current Gradle integration and renderer default are representative-puzzle specific, so adding only a JSON file is insufficient: extend generation/sync inputs, add a dedicated thumbnail/catalog entry, route the selected asset root to `PuzzleRendererFragment`, and test lifecycle/disposal for the new package. Never use the active gameplay texture as a gallery thumbnail.

---

# Dependencies and build versions

- Inspect current versions before editing.
- Use mutually compatible stable versions.
- Prefer the existing version catalog.
- Do not invent dependency versions.
- Use `android studio version-lookup` when available.
- Otherwise use official documentation through `android docs`.
- Do not upgrade Kotlin, AGP, Gradle, Compose, Room, KSP, libGDX or KTX merely because a newer release exists.
- Add a production dependency only when it provides clear value.
- Keep release builds compatible with R8 and resource shrinking.
- Do not require signing credentials for ordinary verification.

---

# Testing

Use fakes for:

- clocks
- random sources
- repositories
- dispatchers
- renderer command sinks

Tests must not rely on arbitrary sleeps.

At minimum cover:

- deterministic question generation
- stable learning identifiers and deterministic item reproduction
- taxonomy uniqueness, mapping versioning and prerequisite-cycle rejection
- immutable append-only attempts, assistance/retries and explicit correction metadata
- deterministic evidence, insufficient evidence, item diversity/near-duplicate handling and response-time exclusion
- valid and unique answers
- scoring
- combos
- incorrect answers
- piece progression
- final-piece completion
- restart
- pause and resume
- difficulty policies
- Room repository behavior
- Room schema migration and preservation of existing puzzle progress
- attempt duplicate prevention, transaction consistency and reset/delete behavior
- legacy-data migration
- DataStore preferences
- manifest serialization
- asset-generator determinism
- invalid asset definitions
- ViewModel event handling
- exactly one attempt per accepted answer and none from renderer acknowledgement/recreation
- renderer command ordering
- duplicate-event prevention

Use the installed `testing-setup` skill when building the test infrastructure.

---

# Performance validation

Do not judge production performance using:

- Compose Preview alone
- a debug APK alone
- subjective visual impressions
- fabricated measurements

Use:

- profileable or release-like builds
- Macrobenchmark
- Baseline Profiles
- Perfetto traces
- allocation and memory diagnostics
- physical-device tests where available

Use the installed Perfetto skills for actual trace files.

Distinguish clearly between:

- measured results
- inferred bottlenecks
- expected architectural improvements
- tests requiring a device that is not available

Never fabricate FPS, memory or frame-time values.

Keep traces, benchmark outputs, screenshots, APKs, and local analysis scratchpads out of Git. Record only reproducible conclusions and their device/OS/refresh/build/scenario/iteration context in `docs/PERFORMANCE_RESULTS.md`. Emulator measurements are diagnostic and cannot establish physical-device frame pacing, thermals, memory ceilings, audio compatibility, or 90/120 Hz sustainability.

---

# Standard verification

Use the smallest applicable verified command set:

- documentation-only operational changes: `tools/android-doctor.ps1`, validate documented paths/links/commands, and run a focused host task when a command claim changes;
- pure models/rules: `:core-model:test :core-learning:test :core-game:test`;
- ViewModel/Compose mapping: `:app:testDebugUnitTest :app:assembleDebug`;
- Room/DataStore: `:core-data:testDebugUnitTest`; schema/DAO changes additionally require `:core-data:connectedDebugAndroidTest` on a connected device;
- renderer command/timing/mesh work: `:renderer-gdx:testDebugUnitTest :app:assembleDebug` plus gameplay enter/exit, background/resume, and surface recreation on a device;
- asset schema/generator work: `:asset-pipeline:test :asset-pipeline:generatePuzzleAssets :asset-pipeline:syncPuzzleThumbnails :renderer-gdx:testDebugUnitTest :app:assembleDebug`, then verify generated diffs and device rendering;
- build/dependency/release work: focused tests plus `:app:assembleDebug :app:assembleBenchmark :benchmark:assembleBenchmark :app:assembleRelease lint`;
- benchmark changes: assemble the app and harness first, then run connected benchmarks only on an available device.

The full verified host suite is:

`powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-learning:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest`

Other common commands:

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 lint`

Prefer focused tasks after discovering real module names:

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-game:test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-learning:test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :renderer-gdx:testDebugUnitTest`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug`

When a device is connected:

1. build the APK through `tools/gradle.ps1`;
2. use `android describe --project_dir=.` to locate outputs;
3. deploy with `android run --apks=<apk-path>`;
4. inspect with:
   - `android screen capture`
   - `android layout`
   - exact adb through `tools/adb.ps1` where needed.

Connected commands are device-dependent; never report them as run when no device is attached. Macrobenchmark keeps its emulator warning enabled. A diagnostic emulator run may suppress only `EMULATOR` explicitly with PowerShell's `--%`, but production conclusions require declared physical hardware. Raw BaselineProfileRule output is currently R8-obfuscated and must not be copied into source until a compatible Baseline Profile Gradle plugin can perform mapping/rewrite for the existing AGP model.

When a task fails:

- rerun the smallest failing task with `--stacktrace`;
- diagnose the first meaningful error;
- fix the root cause;
- do not suppress failures with broad exception handling;
- do not weaken tests solely to make them pass.

---

# Documentation

Maintain:

- `README.md`
- `AGENTS.md`
- `docs/TOOLCHAIN.md`
- `docs/REPOSITORY_KNOWLEDGE.md`
- `docs/CURRENT_ARCHITECTURE.md`
- `docs/TARGET_ARCHITECTURE.md`
- `docs/PERFORMANCE_AUDIT.md`
- `docs/PERFORMANCE_RESULTS.md`
- `docs/MIGRATION_PLAN.md`
- `docs/ASSET_PIPELINE.md`
- `docs/ADDING_A_PUZZLE.md`
- `docs/TESTING.md`
- `docs/PORT_STATUS.md`
- `docs/adr/0001-hybrid-compose-libgdx.md`
- `docs/adr/0002-pure-kotlin-game-engine.md`
- `docs/adr/0003-precomputed-puzzle-assets.md`
- `docs/adr/0004-android-studio-embedded-jbr.md`
- `docs/adr/0005-room-datastore-persistence-boundary.md`
- `docs/adr/0006-pure-kotlin-learning-foundation.md`
- `docs/adr/0007-append-only-local-learning-evidence.md`
- `docs/academy/FOUNDATION_SCOPE.md`
- `docs/academy/SKILL_TAXONOMY.md`
- `docs/academy/LEARNING_EVIDENCE.md`
- `docs/academy/LOCAL_DATA_INVENTORY.md`
- `docs/academy/PHASE_1_STATUS.md`

Documentation must never contain this user's fixed absolute SDK or JDK paths.

Use placeholders such as:

- `<android-sdk>`
- `<android-studio-jbr>`
- `<project-root>`

---

# Engineering Idea Study Protocol

Use this protocol whenever the user asks to investigate, evaluate, compare, plan, assess, or study an idea.

Examples include:

- a new gameplay mechanic;
- a renderer change;
- a dependency or framework change;
- a different persistence model;
- multiplayer or online functionality;
- a new platform;
- procedural puzzle generation;
- downloadable content;
- monetization;
- analytics;
- accessibility changes;
- artificial intelligence features;
- architectural refactoring;
- build or deployment changes;
- performance optimizations.

## Default mode

An idea study is analysis-only by default.

Do not modify production code, dependencies, Gradle files, schemas, generated assets, or application behavior unless the user explicitly asks for implementation or a proof of concept.

The study may create or update a document under:

`docs/studies/`

Use a descriptive lowercase hyphenated filename, for example:

`docs/studies/procedural-puzzle-generation.md`

Do not create a proof of concept unless explicitly authorized.

Do not interpret “study,” “investigate,” “explore,” or “evaluate” as permission to implement.

## Source-of-truth order

Ground the study in this order:

1. Current repository code and configuration
2. Existing tests and benchmark results
3. Existing ADRs and repository documentation
4. Reproducible local experiments
5. Official Android, Kotlin, Gradle, Jetpack, libGDX, and KTX documentation
6. Primary upstream source repositories or release notes
7. Clearly labeled engineering inference

Do not rely on generic assumptions when repository evidence is available.

For Android-specific current guidance, use Android CLI documentation tools when applicable.

Do not invent library versions, API availability, benchmark results, device behavior, or migration costs.

## Required initial interpretation

Begin every study by stating:

- the idea as understood;
- the user outcome it appears intended to achieve;
- whether the idea changes product behavior, implementation, infrastructure, or more than one;
- assumptions being made;
- ambiguities that materially affect the result;
- the current repository baseline relevant to the idea.

Do not block on minor ambiguities. Analyze reasonable interpretations separately when needed.

## Current-state grounding

Before comparing alternatives, inspect the current implementation.

Identify:

- affected modules;
- relevant source files and symbols;
- existing data flow;
- ownership boundaries;
- dependency directions;
- persistence implications;
- renderer implications;
- current tests;
- current performance evidence;
- build and platform constraints.

Reference repository-relative files and symbols.

Do not propose replacing a subsystem without understanding how it currently behaves.

## Required alternatives

Evaluate at least:

1. Keep the current implementation unchanged
2. Make the smallest viable modification
3. Implement the proposed idea directly
4. Use a materially different alternative when one exists

Do not manufacture meaningless alternatives solely to increase the option count.

For each option, state:

- what changes;
- what remains unchanged;
- modules and files likely affected;
- new dependencies or services;
- migration requirements;
- reversibility;
- expected benefits;
- limitations;
- failure modes;
- unresolved questions.

## Mandatory implication dimensions

Evaluate every applicable dimension below.

Use “not applicable” with a reason instead of silently omitting a dimension.

### Product and gameplay

- player-visible behavior;
- learning or gameplay value;
- difficulty and balancing;
- progression effects;
- offline behavior;
- failure experience;
- abuse or cheating possibilities;
- child suitability.

### Architecture

- responsibility ownership;
- module boundaries;
- coupling;
- dependency direction;
- state ownership;
- lifecycle ownership;
- API surface;
- long-term maintainability.

### Rendering and performance

- CPU cost;
- GPU cost;
- memory;
- allocations;
- texture memory;
- batching;
- overdraw;
- frame pacing;
- startup time;
- loading time;
- battery and thermal impact;
- low-end-device behavior;
- high-refresh behavior.

Do not claim performance improvement without measurement or a clearly labeled hypothesis.

### Android lifecycle and platform behavior

- background and foreground transitions;
- surface recreation;
- process death;
- saved state;
- configuration changes;
- window-size adaptation;
- Android version constraints;
- device compatibility;
- permissions;
- Play policy implications when relevant.

### Data and persistence

- schema changes;
- Room migrations;
- DataStore changes;
- compatibility with existing saves;
- rollback safety;
- data loss risk;
- backup and restore;
- synchronization needs;
- retention and deletion.

### Build and dependency management

- Gradle changes;
- plugin compatibility;
- Kotlin and AGP compatibility;
- dependency size;
- transitive dependencies;
- R8 or keep rules;
- native libraries;
- ABI implications;
- reproducibility;
- CI impact.

### Testing and observability

- unit tests;
- integration tests;
- instrumentation tests;
- renderer tests;
- screenshot tests;
- benchmark coverage;
- traces and diagnostics;
- logging;
- failure detection;
- regression risk.

### Security, privacy, and safety

- collected data;
- external communication;
- secrets;
- authentication;
- authorization;
- untrusted input;
- child privacy;
- analytics;
- supply-chain exposure;
- attack surface.

### Accessibility and localization

- screen-reader behavior;
- touch targets;
- reduced motion;
- color contrast;
- text scaling;
- localization;
- right-to-left layout;
- cognitive load.

### Delivery and maintenance

- implementation effort;
- uncertainty;
- review complexity;
- migration stages;
- operational burden;
- maintenance cost;
- vendor dependence;
- contributor learning curve;
- rollback strategy.

## Evidence labels

Label material conclusions using one of:

- **Observed:** directly confirmed in the repository or a reproducible command
- **Measured:** supported by an actual benchmark, trace, or recorded test
- **Documented:** supported by primary official documentation
- **Inferred:** engineering conclusion derived from evidence
- **Assumed:** necessary assumption not yet verified
- **Unknown:** insufficient evidence

Never present an inference or assumption as a measured fact.

For measurements, include:

- device or emulator;
- build type;
- scenario;
- measurement method;
- relevant configuration.

## Comparison format

Provide a decision matrix containing, where applicable:

- option;
- user value;
- implementation complexity;
- runtime performance;
- memory impact;
- compatibility risk;
- migration risk;
- test burden;
- maintenance burden;
- reversibility;
- confidence.

Use relative ratings only when accompanied by explanations.

Avoid false numerical precision.

Do not turn uncertain estimates into exact schedules.

## Risk analysis

For each serious risk, state:

- trigger;
- consequence;
- likelihood;
- impact;
- detection method;
- mitigation;
- rollback or containment strategy.

Separate:

- implementation risk;
- runtime risk;
- migration risk;
- product risk;
- operational risk.

## Recommendation rules

End with one of these conclusions:

- adopt now;
- prototype first;
- adopt incrementally;
- defer until a prerequisite is met;
- reject for this repository;
- insufficient evidence.

The recommendation must include:

- why it is preferred;
- what evidence supports it;
- what assumptions it depends on;
- confidence level;
- conditions that would change the recommendation;
- the smallest safe next action;
- a rollback point.

Do not recommend a full rewrite when an incremental path reasonably achieves the objective.

## Experiment design

When uncertainty is material, propose the smallest experiment capable of resolving it.

Define:

- hypothesis;
- isolated scope;
- exact files or modules;
- implementation limit;
- measurement method;
- success threshold;
- failure threshold;
- maximum acceptable time or complexity;
- cleanup and rollback procedure.

An experiment proposal is not permission to execute it.

## Study document format

A study written under `docs/studies/` must use this structure:

1. Title and status
2. Decision sought
3. Idea interpretation
4. Current repository baseline
5. Assumptions and unknowns
6. Constraints
7. Evaluated options
8. Detailed implications
9. Compatibility analysis
10. Performance analysis
11. Data and migration analysis
12. Security, privacy, accessibility, and policy analysis
13. Testing and observability
14. Risk register
15. Comparison matrix
16. Recommendation
17. Smallest validation experiment
18. Migration outline
19. Rollback strategy
20. Conditions that would change the recommendation
21. Open questions
22. Evidence and repository references

## Efficiency rules

- Begin with the highest-impact unknowns.
- Reuse existing repository documentation instead of repeating it.
- Inspect only the modules relevant to the idea before expanding scope.
- Prefer focused build or test tasks over a full build during initial study.
- Do not run expensive benchmarks unless they answer a material question.
- Do not explore unrelated refactors.
- Stop research when additional information is unlikely to change the decision.
- Explicitly state which questions remain unanswered.

# Definition of done

A task is complete only when:

- the toolchain was initialized through Android CLI and the repository wrappers;
- the requested implementation or documentation is complete within the authorized scope;
- obsolete code or documentation is removed or corrected when safe and in scope;
- relevant tests exist for behavior changes;
- relevant Gradle tasks pass;
- Android resources and renderer objects have correct ownership when affected;
- no user-specific path is committed;
- no secret is committed;
- documentation reflects changed behavior;
- measured results are distinguished from expectations;
- `docs/PORT_STATUS.md` is updated;
- the final response reports:
  - files changed
  - commands run
  - build and test results
  - current phase
  - next exact action
  - blockers
