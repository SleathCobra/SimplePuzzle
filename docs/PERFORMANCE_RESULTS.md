# Performance results

Updated 2026-07-16.

## Evidence boundary

Functional and diagnostic measurements were collected on the `Medium_Phone` Android Emulator: Android 17/API 37, x86_64, 1080 x 2400 at 420 dpi, 60 Hz, four virtual CPUs, and AEHD hardware graphics acceleration. Macrobenchmark used the non-debuggable, R8-minified, resource-shrunk, profileable `benchmark` app variant. Debug screenshots and `dumpsys gfxinfo` are supporting evidence only.

These results do not represent real-device FPS, frame time, memory limits, thermals, power, or high-refresh behavior. The Macrobenchmark source retains the emulator guard; `EMULATOR` was suppressed only on explicit one-off diagnostic commands.

## Game modes and animated-title evidence status

The mode/timer/removal/title-preview implementation added on 2026-07-16 has host, build and functional API 37 emulator evidence, but no new Macrobenchmark or Perfetto measurement is recorded yet. Room instrumentation passes 5/5 and app instrumentation passes 3/3, including repeated title/gallery recreation and Classic-to-timed renderer handoff. Android CLI deployment, screenshots and layout inspection confirmed the animated and reduced-motion title, mode selection, all five mode HUDs, a Survival out-of-hearts result, and a 30-piece Classic completion result. The preview uses precomputed meshes and one package texture, package changes recreate the Fragment at an idle operation boundary, the preview is capped at 30 FPS, timer display interpolation does not write `StateFlow`, and preview code has no repository dependency. These are structural and functional observations, not measured frame-time, memory, power, or leak results; the screenshots are ignored local verification artifacts under `build/` and historical numbers below predate this feature work.

## Runtime verification

- Android CLI deployed the APK and produced title/game/gallery/settings screenshots plus semantics layouts.
- A correct answer reveals a format-2 tabbed piece on one libGDX surface; three nonadjacent silhouettes and the later batched renderer were visually inspected.
- Leaving gameplay initially reproduced a libGDX pause-synchronization timeout followed by `SIGKILL`. Controlled Fragment removal while its surface is attached fixes the ordering; five repeated enter/leave cycles kept the same process and no SurfaceView remained in the gallery.
- Supporting memory snapshots were about 144 MiB PSS before and 146 MiB after the five-cycle stress journey. This two-snapshot AVD variance is not proof of leak freedom, but it did not expose monotonic retention.
- A debug `dumpsys gfxinfo` sample reported 1.18% modern jank and 50th/90th/95th/99th frame times of 23/26/27/30 ms. The legacy metric reported 96.81% jank, illustrating why this debug-emulator sample is not a production claim.
- Reduced motion and LOW graphics persisted through force-stop/relaunch; defaults were restored afterward. Reset progress displayed an explicit destructive confirmation and Cancel preserved data.
- Before the mode/preview work, Room instrumentation passed 2/2 tests and app instrumentation passed 1/1; the current expanded counts are recorded above.

## Macrobenchmark diagnostic

The full one-off emulator-suppressed run completed all four tests: cold startup, title-to-settings, title-to-gameplay, and Baseline Profile generation. Navigation setup force-stops the target before each iteration so every measured journey begins at the title instead of resuming a previous task destination.

After removing the full-screen background decode and eager audio loads, five cold-start iterations reported:

| Metric | Diagnostic AVD result |
|---|---:|
| time to initial display | min 1,191.9 ms; median 1,305.9 ms; max 1,356.9 ms |
| frame CPU duration | P50 53.1 ms; P90 95.2 ms; P95 222.1 ms; P99 340.3 ms |
| frame overrun | P50 55.6 ms; P90 111.9 ms; P95 241.7 ms; P99 344.1 ms |

The same final run measured title-to-settings frame CPU at P50/P90/P95/P99 44.8/59.9/64.6/67.1 ms and title-to-gameplay at 47.1/63.9/81.9/107.6 ms. These AVD navigation values are diagnostic only.

The profile generator produced 8,394 rules (199,286 bytes). The rules contain R8-obfuscated descriptors. Stable Baseline Profile Gradle plugin 1.4.1 cannot configure this AGP 9 application model, so there is no safe mapping/rewrite step and the raw profile was not committed.

## Perfetto startup analysis

Two representative AVD cold-start traces were inspected with `trace_processor`. The pre-change trace included the bitmap/eager-audio implementation; the post-change trace used the cached Compose background, lazy SFX loading, and two-frame music deferral.

| Trace fact | Before | After |
|---|---:|---:|
| startup interval | 1,297.64 ms | 1,046.17 ms |
| app main-thread CPU | 380.09 ms | 325.67 ms |
| Compose recomposition wall / CPU | 148.02 / 76.61 ms | 38.96 / 32.78 ms |
| 768 x 1376 background decode wall / CPU | 89.98 / 40.70 ms | absent |
| startup image decode | background above | 320 x 320 thumbnail, 9.43 ms wall |
| SoundDecoder/MediaCodec CPU inside startup | at least 183.10 ms | absent |
| first Choreographer slice wall / CPU | 371.50 / 124.11 ms | 305.52 / 81.03 ms |

The 251.47 ms representative total delta must not be attributed wholly to app changes: the earlier trace also contained a 204.17 ms `attachApplication` system_server Binder stall, versus 27.08 ms in the later trace. Verified app-owned improvements are the eliminated background decode, eliminated eager audio decode, lower main-thread/recomposition CPU, and smaller first-frame slice.

The post trace's remaining largest startup work was emulator graphics initialization: EGL context creation took 237.50 ms wall / 165.26 ms CPU on RenderThread. There were no app GC, LMK, memory-pressure, or verity events in either representative startup interval.

The final benchmark's retained iteration 4 trace independently revalidated the architectural changes. Its cold-start interval was 1,319.37 ms; app main-thread CPU was 331.97 ms, RenderThread CPU was 321.62 ms, and Compose recomposition was 43.93 ms. The former full-screen decode and eager audio threads remained absent. Its only application resource decode was the 320 x 320 thumbnail (10.64 ms), while the first 323.92 ms frame overlapped 318.70 ms of EGL-context creation. No app GC or LMK event occurred. The trace processor evidence log is stored beside the ignored trace as `StartupBenchmark_coldLaunchToTitle_iter004_2026-07-16-00-38-02.perfetto-trace_analysis.md`.

## Artifact evidence

| Artifact | Size |
|---|---:|
| original 1024 px puzzle source | 2,169,305 bytes |
| generated 768 px active texture | 1,705,709 bytes |
| generated 320 px thumbnail | 311,804 bytes |
| format-2 manifest, 30 tabbed pieces | 96,892 bytes |
| current debug APK | 31,242,738 bytes |
| minified benchmark APK | 8,327,905 bytes |
| minified release APK | 8,315,585 bytes |

APK sizes are packaging facts, not runtime performance results.

## Remaining measurements

- repeat Macrobenchmark and Baseline Profile generation on a physical API 34+ device;
- capture gameplay Perfetto journeys for first reveal, ten rapid answers, completion, background/resume, and repeated navigation;
- measure steady-state renderer frames, allocations, GPU memory, thermals, and 90/120 Hz behavior;
- verify SoundPool/MediaPlayer compatibility on physical hardware—the API 37 AVD reported codec failures for two short samples while remaining crash-free;
- automate renderer surface recreation and multi-window/configuration journeys.
