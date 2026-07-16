# Current architecture

Updated 2026-07-16 after emulator verification of the hybrid gameplay slice.

## Repository map

```text
SimplePuzzle/
|- app/                 Compose application shell and Android wiring
|- core-model/          immutable domain models and serialized manifest schema
|- core-game/           pure Kotlin reducer, random source, rules, and tests
|- core-data/           Room, DataStore, repositories, and migrations
|- asset-pipeline/      deterministic JVM image/manifest generator
|- renderer-gdx/        libGDX/KTX Android renderer and command bridge
|- benchmark/           Macrobenchmark and Baseline Profile journeys
|- puzzles/             source puzzle definitions
|- docs/                architecture, migration, performance, and operations
`- tools/               Android CLI/JBR/Gradle/adb PowerShell wrappers
```

## Runtime flow

```text
Compose input
  -> GameViewModel
  -> pure GameEngine.reduce(action)
  -> immutable state + one-shot domain events
  -> stable Compose HUD state
  -> coarse RendererCommand queue
  -> libGDX fixed-step simulation and GPU draw
  -> reveal-finished callback
  -> GameEngine.reduce(RevealAnimationFinished)
  -> Room transaction / next question
```

Compose does not own particle locations, reveal interpolation, frame delta, or board geometry. The renderer does not access Compose state, Room, DataStore, an Activity, or a Composable.

## Application shell

`MainActivity` is a `FragmentActivity` because libGDX's Android fragment backend owns the rendering surface. It also hosts Compose navigation for title, gallery, gameplay, and settings. Root flows use `collectAsStateWithLifecycle`. Sound, music, and haptic feedback have composition-scoped owners and release/cancel their resources.

The application background is a cached Compose drawing, avoiding a full-screen bitmap decode on the first frame. The title preview is a static generated thumbnail. The gallery uses an adaptive lazy grid, stable keys/content types, and the same low-resolution placeholder until additional puzzle-specific source art is added. Gameplay remains a Compose HUD around one `FragmentContainerView` containing `PuzzleRendererFragment`.

Gameplay exit first pauses the reducer and synchronously removes the renderer Fragment while its surface is still attached, then pops Compose navigation. This ordering is required by `AndroidFragmentApplication`: detaching the `FragmentContainerView` first caused the backend's pause handshake to time out and SIGKILL the process on the emulator. Toolbar and system back share the same controlled teardown.

## Pure game engine

`core-game` has no Android, Compose, persistence, or libGDX dependency. `DefaultGameEngine` accepts a seeded `RandomSource`, creates valid unique answer choices, applies scoring/combo rules, blocks duplicate answers while revealing, and separates logical correctness from visual completion. `PieceSet` is not limited to a 64-bit mask.

`GameViewModel` is the strangler adapter between legacy screen models and the pure engine. Correct answers set a pending piece; progression changes only after the renderer reports that exact piece complete. Stale or duplicate callbacks are ignored.

## Persistence

`JigsawMathApplication` owns one `JigsawDataContainer` with:

- a Room database for puzzle progress and game sessions;
- a DataStore for sound, music, haptics, difficulty, graphics quality, reduced motion, and migration state;
- repositories mapping persistence entities to domain models;
- a one-time legacy migration boundary. The inspected prototype had no durable legacy progress, so the current legacy source is explicitly empty.

Completion/reset operations are transactional. I/O runs in structured application/ViewModel scopes. Current persistence restores aggregate revealed-piece counts, completion, scores, and attempts; exact mid-session piece identity/resume is a documented remaining feature.

## Asset pipeline

The representative `cosmic-journey` definition produces a 768 px active texture, a 320 px thumbnail, a SHA-256 source hash, display metadata, and 30 pieces containing vertices, UVs, indices, bounds, positions, and reveal ordering. Generation is deterministic and runs before app resource processing. Runtime reads the generated package from assets and never slices images or triangulates pieces.

Format version 2 uses `edgeSeed` to generate complementary tab/socket boundaries, pre-triangulated concave silhouettes, and deterministic reveal ranks. Tests verify adjacent edges share their points and that the sum of all piece areas remains one normalized board. The former Compose jigsaw renderer was removed after the generated mesh was verified on the emulator.

## Renderer

`renderer-gdx` uses libGDX 1.13.1 with KTX 1.13.1-rc1, the matching KTX/libGDX release line available from Maven. It:

- loads and validates the manifest/texture in `create()`;
- builds one reusable mesh from precomputed data in `create()`;
- keeps one texture and shader for the board;
- uses a 60 Hz fixed step and clamps resume delta to 100 ms;
- drains explicit commands from a thread-safe queue;
- uses preallocated reveal state and a 64-slot particle pool;
- provides LOW/MEDIUM/HIGH profiles plus reduced-motion reveal duration;
- disposes shader, mesh, texture, batch, glow texture, and backend surface resources.

Revealed pieces are copied into a preallocated index buffer only when coarse reveal/snapshot commands arrive and render in one indexed draw. The currently animating piece uses a second reusable mesh draw for independent alpha. No index, vertex, texture, or collection allocation occurs per rendered frame.

## Build and performance infrastructure

The `benchmark` app build type is non-debuggable, debug-signed, minified by R8, resource-shrunk, and profileable. The separate `benchmark` test module contains startup, navigation, and Baseline Profile journeys. All journeys execute on the API 37 emulator when the emulator warning is explicitly suppressed for diagnostic runs. The stable Baseline Profile Gradle plugin 1.4.1 rejected the AGP 9 application model; raw generated rules are R8-obfuscated and therefore are not copied into source without the plugin's mapping/rewrite support.
