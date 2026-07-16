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
  -> exact mutation-finished callback
  -> GameEngine.reduce(BoardMutationFinished)
  -> Room transaction / next question
```

Compose does not own particle locations, reveal interpolation, frame delta, or board geometry. The renderer does not access Compose state, Room, DataStore, an Activity, or a Composable.

## Application shell

`MainActivity` is a `FragmentActivity` because libGDX's Android fragment backend owns the rendering surface. It also hosts Compose navigation for title, gallery, gameplay, and settings. Root flows use `collectAsStateWithLifecycle`. Sound, music, and haptic feedback have composition-scoped owners and release/cancel their resources.

The application background is a cached Compose drawing. The title contains one 30 FPS `PuzzlePreviewRendererFragment` using a generated package and deterministic partial-board reveal/removal loop; reduced motion keeps it static. The gallery uses an adaptive lazy grid, stable keys/content types, and dedicated generated thumbnails. Gallery selection opens an adaptive mode-selection grid before gameplay. Gameplay remains a Compose mode-aware HUD around one `FragmentContainerView` containing `PuzzleRendererFragment`.

Gameplay exit abandons/cancels the active session and synchronously removes the renderer Fragment while its surface is still attached, then pops Compose navigation. Title navigation similarly removes its preview Fragment before navigating. This ordering is required by `AndroidFragmentApplication`: detaching the `FragmentContainerView` first caused the backend's pause handshake to time out and SIGKILL the process on the emulator. Toolbar and system back share the same controlled teardown, and title/game surfaces are mutually exclusive.

## Pure game engine

`core-game` has no Android, Compose, persistence, or libGDX dependency. `DefaultGameEngine` accepts a seeded `RandomSource`, applies the centrally cataloged Classic/Time Attack/Survival/Puzzle Decay/Combo Rush policies, rejects stale timer generations, and separates logical board mutations from visual completion. `PieceSet` is not limited to a 64-bit mask and supports acknowledged removal.

`GameViewModel` is the adapter between screen models and the pure engine. It owns `ModeTimerCoordinator`, which uses monotonic anchors and semantic expirations instead of ticks. Correct answers/removals set one `PendingBoardMutation`; progression changes only after the renderer reports that exact mutation complete. Stale or duplicate callbacks are ignored, and a recreated renderer replays uncommitted work.

## Persistence

`JigsawMathApplication` owns one `JigsawDataContainer` with:

- Room schema version 2 for puzzle progress, mode-aware game sessions, and per-mode best metrics;
- a DataStore for sound, music, haptics, difficulty, graphics quality, reduced motion, and migration state;
- repositories mapping persistence entities to domain models;
- a one-time legacy migration boundary. The inspected prototype had no durable legacy progress, so the current legacy source is explicitly empty.

Migration 1→2 is explicit and non-destructive. Completion/reset operations are transactional; reset includes mode sessions/bests but retains DataStore preferences. Completing any mode completes permanent progress, while unfinished challenge runs only append statistics. Exact mid-session piece identity/deadline restoration remains outside the current scope.

## Asset pipeline

The representative `cosmic-journey` definition produces a 768 px active texture, a 320 px thumbnail, a SHA-256 source hash, display metadata, and 30 pieces containing vertices, UVs, indices, bounds, positions, and reveal ordering. `puzzles/catalog.json` is validated into a generated runtime catalog before app resource processing. Gameplay and preview route the catalog asset root; runtime never slices images or triangulates pieces.

Format version 2 uses `edgeSeed` to generate complementary tab/socket boundaries, pre-triangulated concave silhouettes, and deterministic reveal ranks. Tests verify adjacent edges share their points and that the sum of all piece areas remains one normalized board. The former Compose jigsaw renderer was removed after the generated mesh was verified on the emulator.

## Renderer

`renderer-gdx` uses libGDX 1.13.1 with KTX 1.13.1-rc1, the matching KTX/libGDX release line available from Maven. It:

- loads and validates the manifest/texture in `create()`;
- builds one reusable mesh from precomputed data in `create()`;
- keeps one texture and shader for the board;
- uses a 60 Hz fixed step and clamps resume delta to 100 ms;
- drains explicit commands from a thread-safe queue;
- uses preallocated reveal state and a 64-slot particle pool;
- provides LOW/MEDIUM/HIGH profiles plus reduced-motion reveal/removal behavior and preview profiles;
- disposes shader, mesh, texture, batch, glow texture, and backend surface resources.

Revealed pieces are copied into a preallocated index buffer only when coarse reveal/snapshot commands arrive and render in one indexed draw. The currently animating piece uses a second reusable mesh draw for independent alpha. No index, vertex, texture, or collection allocation occurs per rendered frame.

## Build and performance infrastructure

The `benchmark` app build type is non-debuggable, debug-signed, minified by R8, resource-shrunk, and profileable. The separate `benchmark` test module contains startup, navigation, and Baseline Profile journeys. All journeys execute on the API 37 emulator when the emulator warning is explicitly suppressed for diagnostic runs. The stable Baseline Profile Gradle plugin 1.4.1 rejected the AGP 9 application model; raw generated rules are R8-obfuscated and therefore are not copied into source without the plugin's mapping/rewrite support.
