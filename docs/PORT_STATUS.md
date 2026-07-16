# Port status

Last updated: 2026-07-16.

Current implementation details and operational knowledge are consolidated in `docs/REPOSITORY_KNOWLEDGE.md`; this file tracks scope and remaining work rather than serving as an architecture specification.

| Phase | State | Evidence / remaining work |
|---|---|---|
| 0. Toolchain | Complete | Android CLI SDK discovery, embedded JBR recovery, wrappers, doctor, Gradle validation |
| 1. Audit | Complete | baseline build/test/lint and performance audit recorded |
| 2. Pure game engine | Complete | deterministic reducer, scalable piece set, rule/ViewModel tests |
| 3. Room/DataStore | Complete for schema 2 | progress/session/preferences repositories, append-only local learning evidence, explicit `MIGRATION_1_2`, and transaction/migration tests |
| 4. Asset pipeline | Complete for format 2 | deterministic 768 px texture, 320 px thumbnail, complementary tabs, pre-triangulated meshes, manifest and geometry tests |
| 5. libGDX bridge | Complete for Cosmic Journey | one Fragment-hosted surface, explicit queue, renderer-finished progression callback |
| 6. Effects/audio | Complete for initial profiles | fixed step, pooled sparkles, reveal/shake/completion effects, quality/reduced motion, owned audio jobs/resources |
| 7. Compose shell | Complete for current scope | lifecycle collection, cached background, adaptive gallery, stable keys/content types, thumbnails, settings/haptics, emulator visual QA |
| 8. Performance | Emulator diagnostics complete | full Macrobenchmark diagnostic passes, profile/traces generated, representative startup traces analyzed; physical data remains |
| 9. Cleanup | Complete for current repository scope | legacy Compose renderer removed; final host, connected, benchmark, release and lint verification passed |
| Academy Phase 1. Local learning foundation | Complete for authorized scope | pure `core-learning` contracts, taxonomy/evidence policy v1, reproducible Jigsaw addition items, append-only Room evidence, exactly-once ViewModel adapter, cautious personal summaries, and local reset are implemented and verified |

## Current working application

- gameplay entry: `MainActivity` -> gallery -> `GameScreen`;
- active gameplay renderer: `GdxPuzzleBoard` -> `PuzzleRendererFragment` -> `PuzzleRenderer`;
- board package: `assets/puzzles/cosmic-journey`;
- persistence: Room schema 2 for puzzle/session state and local learning evidence, plus DataStore preferences;
- renderer progression: correct answer -> reveal command -> renderer callback -> reducer completion;
- release-like target: non-debuggable, R8 minified, resource-shrunk, profileable `benchmark` variant;
- native packaging: `libgdx.so` present for arm64-v8a, armeabi-v7a, x86, and x86_64;
- device validation evidence: an API 37 AVD run is recorded in `docs/PERFORMANCE_RESULTS.md`; do not assume a device is currently connected;
- renderer batching: all static revealed pieces use one preallocated index-buffer draw; the active reveal uses one additional draw.

## Verified commands

Passing:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-learning:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest :app:assembleDebug
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleDebug :app:assembleBenchmark :benchmark:assembleBenchmark :app:assembleRelease lint
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

Room instrumentation passes 5/5 tests and app instrumentation passes 6/6 on the API 37 AVD. The earlier complete emulator-suppressed Macrobenchmark run passes all 4/4 tests and emits five traces per timing journey plus 8,394 profile rules. The Academy-specific `firstIncorrectAnswerToLearningSummary` journey also passes all five iterations and verifies that an accepted answer becomes observable local evidence; all emulator timing is diagnostic. The default guard still rejects timing tests on emulators as intended. Final debug, benchmark and minified release assembly plus aggregate lint all pass.

## Current exact action

Obtain educator/curriculum review of taxonomy version 1, the generated addition distribution, regrouping classification, learner-facing wording, and evidence-policy version 1 before authorizing a supervised pilot. Number Line Expedition, teacher mode, accounts, cloud synchronization, predictive mastery, and social functionality remain unauthorized.

## Blockers and limitations

- Emulator numbers are diagnostic and cannot establish production FPS, frame time, thermal behavior, memory ceilings, or high-refresh sustainability.
- Stable Baseline Profile Gradle plugin 1.4.1 rejects this AGP 9 application model. Raw generator output is R8-obfuscated, so it is not safe to copy into source without compatible mapping/rewrite integration.
- Two SoundPool samples reported emulator codec decode failures; playback remained crash-free, but physical-device audio compatibility remains to be checked.
- Exact mid-session piece identity is not yet persisted.
- Academy taxonomy and evidence-policy thresholds are transparent product assumptions, not validated educational or psychometric conclusions; only symbolic addition currently emits evidence.
- Learning attempts are retained until Reset All Progress/app-data deletion; a broader deployment needs an approved retention policy.
- PDF rendering/extraction utilities were unavailable during knowledge consolidation. The Typst source and present reference files were inspected, but `docs/reference/report.typ` also references absent images and cannot currently reproduce the checked-in historical PDF.
