# Port status

Last updated: 2026-07-16.

| Phase | State | Evidence / remaining work |
|---|---|---|
| 0. Toolchain | Complete | Android CLI SDK discovery, embedded JBR recovery, wrappers, doctor, Gradle validation |
| 1. Audit | Complete | baseline build/test/lint and performance audit recorded |
| 2. Pure game engine | Complete | deterministic reducer, scalable piece set, rule/ViewModel tests |
| 3. Room/DataStore | Complete for initial schema | progress/session/preferences repositories and transaction/migration tests |
| 4. Asset pipeline | Complete for format 2 | deterministic 768 px texture, 320 px thumbnail, complementary tabs, pre-triangulated meshes, manifest and geometry tests |
| 5. libGDX bridge | Complete for Cosmic Journey | one Fragment-hosted surface, explicit queue, renderer-finished progression callback |
| 6. Effects/audio | Complete for initial profiles | fixed step, pooled sparkles, reveal/shake/completion effects, quality/reduced motion, owned audio jobs/resources |
| 7. Compose shell | Complete for current scope | lifecycle collection, cached background, adaptive gallery, stable keys/content types, thumbnails, settings/haptics, emulator visual QA |
| 8. Performance | Emulator diagnostics complete | full Macrobenchmark diagnostic passes, profile/traces generated, representative startup traces analyzed; physical data remains |
| 9. Cleanup | Complete for current repository scope | legacy Compose renderer removed; final host, connected, benchmark, release and lint verification passed |

## Current working application

- gameplay entry: `MainActivity` -> gallery -> `GameScreen`;
- active gameplay renderer: `GdxPuzzleBoard` -> `PuzzleRendererFragment` -> `PuzzleRenderer`;
- board package: `assets/puzzles/cosmic-journey`;
- persistence: Room plus DataStore;
- renderer progression: correct answer -> reveal command -> renderer callback -> reducer completion;
- release-like target: non-debuggable, R8 minified, resource-shrunk, profileable `benchmark` variant;
- native packaging: `libgdx.so` present for arm64-v8a, armeabi-v7a, x86, and x86_64;
- connected device: API 37 `Medium_Phone` AVD; functional evidence only.
- renderer batching: all static revealed pieces use one preallocated index-buffer draw; the active reveal uses one additional draw.

## Verified commands

Passing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest :app:assembleDebug
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug :app:assembleBenchmark :benchmark:assembleBenchmark :app:assembleRelease lint
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

Room instrumentation passes 2/2 tests and app instrumentation passes 1/1 on the AVD. The final one-off emulator-suppressed Macrobenchmark run passes all 4/4 tests and emits five traces per timing journey plus 8,394 profile rules; the default guard still rejects timing tests on emulators as intended. Final debug, benchmark and minified release assembly plus aggregate lint all pass.

## Current exact action

Repeat Macrobenchmark, gameplay Perfetto journeys, audio checks and 90/120 Hz renderer validation on a physical API 34+ device.

## Blockers and limitations

- Emulator numbers are diagnostic and cannot establish production FPS, frame time, thermal behavior, memory ceilings, or high-refresh sustainability.
- Stable Baseline Profile Gradle plugin 1.4.1 rejects this AGP 9 application model. Raw generator output is R8-obfuscated, so it is not safe to copy into source without compatible mapping/rewrite integration.
- Two SoundPool samples reported emulator codec decode failures; playback remained crash-free, but physical-device audio compatibility remains to be checked.
- Exact mid-session piece identity is not yet persisted.
- PDF rendering utilities are unavailable; the report's Typst source and all referenced images were inspected instead.
