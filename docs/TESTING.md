# Testing strategy

Tests use injected fakes and deterministic state rather than arbitrary delays or broad mocking frameworks.

## Host tests

- `core-model`: piece sets beyond 64 pieces and model invariants;
- `core-game`: deterministic questions, valid/unique choices, difficulty ranges, scoring, combo reset/growth, wrong answers, reveal separation, final completion, duplicate-event prevention, pause/resume, and restart;
- `core-data`: real temporary DataStore round trips and one-time legacy migration with fakes;
- `asset-pipeline`: byte-identical generation, manifest round trip, invalid definitions, seed-dependent topology, complementary boundaries, area-preserving pre-triangulation, and reveal ranks;
- `renderer-gdx`: command ordering, callback replacement, fixed-step accumulation/delta clamping, mesh validation, and particle-pool capacity/reuse;
- `app`: ViewModel mapping, renderer-delayed progression, wrong-answer feedback, and duplicate completion handling.

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest
```

Jacoco is enabled for the pure JVM modules. Coverage is supporting evidence, not a replacement for behavior assertions.

## Device tests

`core-data` contains an in-memory Room DAO test for transactional attempt, progress, completion, session, and reset behavior. Existing app instrumentation scaffolding also compiles. Build device tests with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestKotlin
```

The API 37 emulator executes both current connected suites:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

`core-data` passes two Room transaction/reset tests and `app` passes its current instrumentation test. Manual device journeys additionally verified system/toolbar back teardown, background/resume, settings persistence across force-stop/relaunch, reset confirmation/cancel, semantics, tabbed mesh rendering, and five repeated gameplay enter/leave cycles. Automated renderer-surface recreation and screenshot coverage remain useful additions.

## UI and adaptive verification

The gallery uses `GridCells.Adaptive(168.dp)`. Required visual coverage is phone, foldable, tablet, and desktop-sized previews/screenshots, including large font scale. No screenshot references were fabricated or updated without rendering support. Capture and review them on a connected environment before declaring visual parity.

## Benchmark and Baseline Profile

The `benchmark` module compiles against the minified/profileable `benchmark` app variant. It contains cold startup, title-to-gameplay, title-to-settings, and Baseline Profile journeys.

Assemble with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleBenchmark :benchmark:assembleBenchmark
```

Execute on API 34+ physical hardware with the connected benchmark task. The source keeps Macrobenchmark's emulator guard enabled. A one-off emulator diagnostic may explicitly suppress only `EMULATOR`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

To collect only the profile generator, also pass `-Pandroid.testInstrumentationRunnerArguments.class=com.qtpie.simplepuzzle.benchmark.BaselineProfileGenerator`. The final API 37 diagnostic run passed 4/4 benchmark tests and generated 8,394 R8-obfuscated rules (199,286 bytes). Because stable Baseline Profile Gradle plugin 1.4.1 cannot configure this AGP 9 model, those descriptors cannot be safely mapped back into a stable source profile and were not committed.

## Device constraints

The available AVD establishes functional integration but is not representative performance hardware. Physical-device work is still required for production frame time, thermal behavior, audio compatibility, memory ceilings, high-refresh behavior, and authoritative Macrobenchmark comparisons.
