# Jigsaw Math

Jigsaw Math is an Android educational game that rewards correct mental-math answers by revealing a puzzle image. The implemented application uses a hybrid architecture:

- Compose owns navigation, title, gallery, settings, gameplay HUD, questions, and accessible controls.
- a pure Kotlin reducer owns deterministic game rules;
- Room stores progression and sessions;
- DataStore stores preferences;
- libGDX/KTX owns the real-time puzzle board and effects;
- a JVM generator produces versioned puzzle textures, thumbnails, and mesh manifests before runtime.

The same generated puzzle can be played in Classic, Time Attack, Survival, Puzzle Decay, or Combo Rush. Classic is the default untimed learning mode. Timed and penalty modes use deterministic pure rules, lifecycle-aware monotonic deadlines, and renderer-acknowledged reveal/removal operations. The title uses a separate lightweight animated libGDX preview; it never starts a game or writes progress.

The repository contains an end-to-end hybrid slice for Cosmic Journey with deterministic tabbed meshes. Host and connected emulator tests have passed, runtime lifecycle/settings/rendering have been exercised, and a minified benchmark variant produces Macrobenchmark traces and Baseline Profile output. Emulator measurements are diagnostic; authoritative performance work still requires a physical device. See [repository knowledge](docs/REPOSITORY_KNOWLEDGE.md) for current entry points, flows, limitations, and debugging procedures.

## Modules

- `app`: Compose shell, navigation, ViewModel, Android lifecycle wiring
- `core-model`: immutable shared models, game-mode policies, and serialized puzzle/catalog schema
- `core-game`: pure deterministic reducer, questions, scoring, timers, and board-mutation rules
- `core-data`: Room v2, DataStore, repositories, and explicit migrations
- `asset-pipeline`: deterministic JVM puzzle and catalog generator
- `renderer-gdx`: gameplay/title libGDX surfaces, command bridge, meshes, timing, and pooled effects
- `benchmark`: Macrobenchmark journeys and Baseline Profile generator

## Build

Use Android CLI for SDK discovery and the repository wrappers for Gradle and adb. No standalone JDK is required.

```powershell
android info
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\android-doctor.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 test
```

Generate the representative puzzle package with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :asset-pipeline:generatePuzzleAssets :asset-pipeline:syncPuzzleThumbnails :asset-pipeline:generatePuzzleCatalog
```

See [game modes](docs/GAME_MODES.md), [title preview](docs/TITLE_PREVIEW.md), [toolchain setup](docs/TOOLCHAIN.md), [testing](docs/TESTING.md), [asset generation](docs/ASSET_PIPELINE.md), and [current port status](docs/PORT_STATUS.md).
