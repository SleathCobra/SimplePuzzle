# Testing strategy

Tests use injected fakes and deterministic state rather than arbitrary delays or broad mocking frameworks.

## Host tests

- `core-model`: piece sets beyond 64 pieces and model invariants;
- `core-learning`: stable-ID validation, taxonomy uniqueness/cycle rejection, immutable attempt correction metadata, deterministic evidence, insufficient evidence, item diversity, near-duplicate handling, recency, assistance/retries, and response-time exclusion;
- `core-game`: deterministic addition questions and exact seed/configuration reproduction, valid/unique choices, Jigsaw learning-item/attempt mapping, scoring, combo reset/growth, wrong answers, reveal separation, final completion, duplicate-event prevention, pause/resume, and restart;
- `core-data`: real temporary DataStore round trips, one-time legacy migration with fakes, and learning entity/domain mapping;
- `asset-pipeline`: byte-identical generation, manifest round trip, invalid definitions, seed-dependent topology, complementary boundaries, area-preserving pre-triangulation, and reveal ranks;
- `renderer-gdx`: command ordering, callback replacement, fixed-step accumulation/delta clamping, mesh validation, and particle-pool capacity/reuse;
- `app`: ViewModel mapping, one attempt per accepted correct/wrong answer, delayed-persistence independence, renderer-delayed progression without a duplicate attempt, wrong-answer feedback, and duplicate completion handling.

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-learning:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest
```

Jacoco is enabled for the pure JVM modules. Coverage is supporting evidence, not a replacement for behavior assertions.

## Device tests

`core-data` contains in-memory Room tests for transactional progress/completion, append-only learning attempts, duplicate prevention, learning-session consistency, reset, and the explicit schema-1-to-2 migration. The app suite exercises all learner-summary statuses, its empty state, semantics, and large font scale. Build device tests with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestKotlin
```

The API 37 emulator executes both current connected suites:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

The current API 37 run passes 5 `core-data` tests and 6 app tests. Manual device journeys additionally verified system/toolbar back teardown, background/resume, settings persistence across force-stop/relaunch, learning evidence after a correct renderer-finished reveal, transactional learning reset, semantics, tabbed mesh rendering, and repeated gameplay enter/leave cycles. Automated renderer-surface recreation and screenshot coverage remain useful additions.

## UI and adaptive verification

The gallery uses `GridCells.Adaptive(168.dp)`. Required visual coverage is phone, foldable, tablet, and desktop-sized previews/screenshots, including large font scale. No screenshot references were fabricated or updated without rendering support. Capture and review them on a connected environment before declaring visual parity.

## Benchmark and Baseline Profile

The `benchmark` module compiles against the minified/profileable `benchmark` app variant. It contains cold startup, title-to-gameplay, title-to-settings, incorrect-answer-to-local-summary, and Baseline Profile journeys. The learning journey verifies that an attempt becomes visible after supported gameplay teardown; it does not treat an emulator timing sample as production evidence.

Assemble with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :app:assembleBenchmark :benchmark:assembleBenchmark
```

Execute on API 34+ physical hardware with the connected benchmark task. The source keeps Macrobenchmark's emulator guard enabled. A one-off emulator diagnostic may explicitly suppress only `EMULATOR`:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

To collect only the profile generator, also pass `-Pandroid.testInstrumentationRunnerArguments.class=com.qtpie.simplepuzzle.benchmark.BaselineProfileGenerator`. The final API 37 diagnostic run passed 4/4 benchmark tests and generated 8,394 R8-obfuscated rules (199,286 bytes). Because stable Baseline Profile Gradle plugin 1.4.1 cannot configure this AGP 9 model, those descriptors cannot be safely mapped back into a stable source profile and were not committed.

To run only the focused Academy journey:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 --% :benchmark:connectedBenchmarkAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.qtpie.simplepuzzle.benchmark.NavigationBenchmark#firstIncorrectAnswerToLearningSummary -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.suppressErrors=EMULATOR
```

The post-Phase-1 focused run passed 1/1 test across five iterations. The earlier full diagnostic remains a 4/4 pre-Phase-1 record; a new full-suite run is not implied by the focused result. Raw traces and JSON stay under ignored build output.

## Device constraints

The available AVD establishes functional integration but is not representative performance hardware. Physical-device work is still required for production frame time, thermal behavior, audio compatibility, memory ceilings, high-refresh behavior, and authoritative Macrobenchmark comparisons.
