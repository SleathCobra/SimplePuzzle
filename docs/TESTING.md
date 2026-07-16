# Testing strategy

Tests use injected fakes and deterministic state rather than arbitrary delays or broad mocking frameworks.

## Host tests

- `core-model`: piece sets beyond 64 pieces, mode catalog/fallback, mutation/timer model invariants;
- `core-game`: Classic parity; Time Attack, Survival, Puzzle Decay, Combo Rush; deterministic timer races; acknowledged reveal/removal; outcomes; pause/resume/restart; seed reproduction;
- `core-data`: temporary DataStore round trips, last-mode fallback, and one-time legacy migration with fakes;
- `asset-pipeline`: byte-identical package/catalog generation, manifest round trip, invalid definitions/packages, topology, area-preserving triangulation, and reveal ranks;
- `renderer-gdx`: command order/replay/callback replacement, fixed-step clamp, bounded pools, removal profiles, preview determinism/occupancy/package order;
- `app`: mode selection/HUD mapping, timer cancellation/pause, renderer-delayed progression, one-shot heart/time feedback, and duplicate completion handling.

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-model:test :core-game:test :core-data:testDebugUnitTest :renderer-gdx:testDebugUnitTest :asset-pipeline:test :app:testDebugUnitTest
```

Jacoco is enabled for the pure JVM modules. Coverage is supporting evidence, not a replacement for behavior assertions.

## Device tests

`core-data` device tests cover Room 1→2 preservation/normalization, mode sessions, independent best metrics, unknown mode text, challenge/permanent-progress separation, and reset. App tests cover title preview existence, repeated title/gallery teardown, mode selection, Classic/timed gameplay, and title/game surface exclusivity. Build device tests with:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:compileDebugAndroidTestKotlin :app:compileDebugAndroidTestKotlin
```

The API 37 emulator executes both current connected suites. The latest feature verification passes 5/5 Room tests and 3/3 app tests:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\gradle.ps1 :core-data:connectedDebugAndroidTest :app:connectedDebugAndroidTest
```

These counts are from the current mode/preview implementation. Historical pre-mode emulator measurements remain separately labeled in `PERFORMANCE_RESULTS.md`.

The timed HUD intentionally interpolates its display from a coarse monotonic anchor with a local frame clock. Its navigation instrumentation invokes the selected card's semantic action and advances the Compose test clock by a fixed amount before waiting on the renderer lifecycle callback; it never sleeps or waits for global Compose idleness during an active countdown.

## UI and adaptive verification

The gallery uses `GridCells.Adaptive(168.dp)`. Required visual coverage is phone, foldable, tablet, and desktop-sized previews/screenshots, including large font scale. No screenshot references were fabricated or updated without rendering support. Capture and review them on a connected environment before declaring visual parity.

## Benchmark and Baseline Profile

The `benchmark` module compiles against the minified/profileable `benchmark` app variant. Sources include cold animated-title startup, title-to-settings, title-to-mode/game, ten timed answers, Puzzle Decay reveal/removal, and timed background/resume journeys plus the Baseline Profile flow.

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
