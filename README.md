# Jigsaw Math

Jigsaw Math is an Android educational game that rewards correct mental-math answers by revealing a puzzle image. The implemented application uses a hybrid architecture:

- Compose owns navigation, title, gallery, settings, gameplay HUD, questions, and accessible controls.
- a pure Kotlin reducer owns deterministic game rules;
- a pure Kotlin learning boundary owns versioned skills, items, attempts, and transparent evidence summaries;
- Room stores progression and sessions;
- DataStore stores preferences;
- libGDX/KTX owns the real-time puzzle board and effects;
- a JVM generator produces versioned puzzle textures, thumbnails, and mesh manifests before runtime.

The repository contains an end-to-end hybrid slice for Cosmic Journey with deterministic tabbed meshes and a local-only Academy Phase 1 evidence foundation for approved Grade 2 MATATAG addition practice. Host and connected emulator tests have passed, runtime lifecycle/settings/rendering/reset behavior have been exercised, and a minified benchmark variant produces Macrobenchmark traces and Baseline Profile output. Emulator measurements are diagnostic; authoritative performance work still requires a physical device. See [repository knowledge](docs/REPOSITORY_KNOWLEDGE.md) for current entry points, flows, limitations, and debugging procedures.

## Modules

- `app`: Compose shell, navigation, ViewModel, Android lifecycle wiring
- `core-model`: immutable shared models and serialized puzzle schema
- `core-learning`: pure versioned taxonomy, learning items/attempts, and deterministic evidence policy
- `core-game`: pure deterministic reducer and question/scoring policies
- `core-data`: Room, DataStore, repositories, and one-time migration
- `asset-pipeline`: deterministic JVM asset generator
- `renderer-gdx`: Android libGDX/KTX surface, command bridge, meshes, timing, pooled effects
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
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :asset-pipeline:generatePuzzleAssets :asset-pipeline:syncPuzzleThumbnails
```

See [toolchain setup](docs/TOOLCHAIN.md), [testing](docs/TESTING.md), [asset generation](docs/ASSET_PIPELINE.md), and [current port status](docs/PORT_STATUS.md).
