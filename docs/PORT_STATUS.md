# Port status

Last updated: 2026-07-16.

Current implementation details and operational knowledge are consolidated in `docs/REPOSITORY_KNOWLEDGE.md`; this file tracks scope and remaining work rather than serving as an architecture specification.

| Phase | State | Evidence / remaining work |
|---|---|---|
| 0. Toolchain | Complete | Android CLI SDK discovery, embedded JBR recovery, wrappers, doctor, Gradle validation |
| 1. Audit | Complete | baseline build/test/lint and performance audit recorded |
| 2. Pure game engine | Complete | deterministic reducer, scalable piece set, rule/ViewModel tests |
| 3. Room/DataStore | Complete for schema 2 | progress, mode-session, per-mode-best and preference repositories; explicit 1→2 migration and transaction tests |
| 4. Asset pipeline | Complete for format 2 | deterministic 768 px texture, 320 px thumbnail, complementary tabs, pre-triangulated meshes, manifest and geometry tests |
| 5. libGDX bridge | Complete for Cosmic Journey | one Fragment-hosted surface, explicit queue, renderer-acknowledged reveal/removal mutations |
| 6. Effects/audio | Complete for initial profiles | fixed step, pooled sparkles, reveal/shake/completion effects, quality/reduced motion, owned audio jobs/resources |
| 7. Compose shell | Complete for current scope | lifecycle collection, cached background, adaptive gallery, stable keys/content types, thumbnails, settings/haptics, emulator visual QA |
| 8. Performance | Emulator diagnostics complete | full Macrobenchmark diagnostic passes, profile/traces generated, representative startup traces analyzed; physical data remains |
| 9. Cleanup | Complete for current repository scope | legacy Compose renderer removed; final host, connected, benchmark, release and lint verification passed |
| 10. Local game modes and title preview | Complete for the current catalog | Classic, Time Attack, Survival, Puzzle Decay and Combo Rush; acknowledged reveal/removal; Room 2; mode UI/results; catalog-backed animated title preview; host and API 37 emulator coverage |

## Current working application

- gameplay entry: `MainActivity` -> gallery -> mode selection -> `GameScreen`;
- active gameplay renderer: `GdxPuzzleBoard` -> `PuzzleRendererFragment` -> `PuzzleRenderer`;
- title renderer: `TitlePuzzlePreview` -> `PuzzlePreviewRendererFragment` -> the shared precomputed-mesh renderer in preview configuration;
- board catalog: generated `assets/puzzles/catalog.json`, currently containing the one real `assets/puzzles/cosmic-journey` package;
- persistence: Room schema 2 plus DataStore;
- renderer progression: reducer mutation -> reveal/remove command -> exact mutation acknowledgement -> reducer commit;
- challenge clocks: semantic deadlines coordinated from a monotonic app clock; smooth HUD interpolation remains local to Compose;
- release-like target: non-debuggable, R8 minified, resource-shrunk, profileable `benchmark` variant;
- native packaging: `libgdx.so` present for arm64-v8a, armeabi-v7a, x86, and x86_64;
- device validation evidence: an API 37 AVD run is recorded in `docs/PERFORMANCE_RESULTS.md`; do not assume a device is currently connected;
- renderer batching: all static revealed pieces use one preallocated index-buffer draw; the active reveal uses one additional draw.

## Verified commands

Passing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest :app:assembleDebug
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug :app:assembleBenchmark :benchmark:assembleBenchmark :app:assembleRelease lint
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

Room instrumentation passes 5/5 tests and app instrumentation passes 3/3 on the API 37 AVD. The app suite covers repeated title/gallery teardown and Classic/timed gameplay renderer handoff. Android CLI screenshots/layouts also cover all five HUDs, mode selection, animated/reduced-motion title, Survival failure, and Classic completion. The earlier one-off emulator-suppressed Macrobenchmark run passes all 4/4 historical journeys and emits five traces per timing journey plus 8,394 profile rules; those measurements predate the mode/preview work. The default guard still rejects timing tests on emulators as intended. Debug, benchmark and minified release assembly plus aggregate lint pass.

## Current exact action

Run the expanded mode/preview Macrobenchmark and Perfetto journeys on a declared physical API 34+ device, then record only measured results and device context.

## Blockers and limitations

- Emulator numbers are diagnostic and cannot establish production FPS, frame time, thermal behavior, memory ceilings, or high-refresh sustainability.
- Stable Baseline Profile Gradle plugin 1.4.1 rejects this AGP 9 application model. Raw generator output is R8-obfuscated, so it is not safe to copy into source without compatible mapping/rewrite integration.
- Two SoundPool samples reported emulator codec decode failures; playback remained crash-free, but physical-device audio compatibility remains to be checked.
- Exact mid-session process-death restoration, including transient piece identity and live timer deadlines, remains outside the current persistence boundary.
- The authoritative catalog currently has one valid generated puzzle package, so the title preview loops Cosmic Journey; package switching is implemented and deterministically tested but awaits a second real package.
- PDF rendering/extraction utilities were unavailable during knowledge consolidation. The Typst source and present reference files were inspected, but `docs/reference/report.typ` also references absent images and cannot currently reproduce the checked-in historical PDF.
