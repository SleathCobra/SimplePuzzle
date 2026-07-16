# Jigsaw Math — Codex Repository Instructions

## Mission

Incrementally migrate and optimize this Android educational game using:

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

Keep the application buildable and usable after every substantial migration phase.

---

# Mandatory Windows toolchain contract

## Environment facts

This Windows machine does not have a standalone system JDK installation.

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

The chosen JDK must also successfully run:

`.\gradlew.bat --version`

Do not assume that the newest detected JDK is compatible. Inspect the repository's Android Gradle Plugin and Gradle versions before changing JDK or build versions.

## Required repository-local wrappers

At the beginning of the migration, create these machine-independent PowerShell tools if they do not already exist:

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

Before changing source code, run:

1. `android info`
2. `android describe --project_dir=.`
3. `android sdk list "platform-tools|build-tools|platforms" --all-versions`
4. `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\android-doctor.ps1`
5. `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --version`

Record failures that existed before source changes.

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

Android Studio integration commands are optional and must not block the migration. They may require a compatible active Android Studio version, an open project and a working IDE connection.

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

Do not perform an AGP 9 migration solely because the `agp-9-upgrade` skill exists.

---

# Repository safety

- Inspect `git status` before editing.
- Preserve all existing uncommitted user work.
- Confirm that work occurs on a migration branch.
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

3. Record pre-existing failures separately from failures introduced by the migration.

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

Use DataStore for:

- sound enabled
- music enabled
- haptics enabled
- difficulty
- graphics quality
- reduced motion

Do not store large images, textures or mesh blobs in Room.

Preserve existing user progress through a tested one-time migration.

Use transactions for atomic puzzle-completion updates.

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
- legacy-data migration
- DataStore preferences
- manifest serialization
- asset-generator determinism
- invalid asset definitions
- ViewModel event handling
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

---

# Standard verification

After creating the toolchain wrappers, use commands such as:

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 lint`

Prefer focused tasks after discovering real module names:

- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-game:test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :renderer-gdx:test`
- `powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug`

When a device is connected:

1. build the APK through `tools/gradle.ps1`;
2. use `android describe --project_dir=.` to locate outputs;
3. deploy with `android run --apks=<apk-path>`;
4. inspect with:
   - `android screen capture`
   - `android layout`
   - exact adb through `tools/adb.ps1` where needed.

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

Documentation must never contain this user's fixed absolute SDK or JDK paths.

Use placeholders such as:

- `<android-sdk>`
- `<android-studio-jbr>`
- `<project-root>`

---

# Definition of done

A task is complete only when:

- the toolchain was initialized through Android CLI and the repository wrappers;
- relevant code is implemented;
- obsolete code is removed when safe;
- relevant tests exist;
- relevant Gradle tasks pass;
- Android resources and renderer objects have correct ownership;
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