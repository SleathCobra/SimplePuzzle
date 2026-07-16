# Pre-port performance audit

Audit date: 2026-07-15. This section preserves the baseline static audit and its original source references. No FPS, frame-time, allocation, energy, or memory figures were measured because no device was connected. A benchmark/profileable variant has since been implemented; current evidence is recorded in `PERFORMANCE_RESULTS.md`. Findings below were inferred bottlenecks unless explicitly labeled as measured.

## Five highest-probability jank causes

1. **The puzzle is a per-piece Compose scene graph.** `JigsawPuzzle.kt:207` creates an empty-slot node for every hidden piece, while `JigsawPuzzle.kt:239` creates an `AnimatedVisibility`, shadow, clip, backgrounds, borders, layout and custom draw chain for every visible piece. `GameScreen.kt:145` feeds a full-board image through that hierarchy. A 30-piece puzzle therefore causes broad composition/layout/draw work and many overlapping transparent operations for what should be one batched renderer surface.

2. **Jigsaw paths are regenerated while drawing.** `JigsawPuzzle.kt:334` calls `shape.createOutline(...)` inside `drawWithContent` for every visible piece and draw pass, then performs three path strokes with newly described gradients. This violates the target invariant that geometry is precomputed and reused.

3. **A cosmetic progress effect drives continuous Compose work.** `GameScreen.kt:235` creates an infinite transition whose changing value is consumed while constructing the gradient at `GameScreen.kt:263`. Lint separately reports state-backed use of the non-lambda `Modifier.offset` at `JigsawPuzzle.kt:261`, which forces recomposition during drag. These are UI-thread animation costs competing with puzzle drawing.

4. **Coarse root state collection broadens invalidation.** `MainActivity.kt:52` collects game, settings, profile and catalog flows at the navigation root using non-lifecycle-aware `collectAsState`. The timer writes a new `GameUiState` every second at `GameViewModel.kt:127`, so the root navigation content and gameplay parameters are invalidated even when only the timer text changed.

5. **Gameplay-resolution bitmap reuse replaces an asset strategy.** `GalleryScreen.kt:189`, `GameScreen.kt:156`, and `StartScreen.kt:122` all use the same 1024 x 1024, 2,169,305-byte densityless PNG. Gallery cards have no thumbnails and the title preview instantiates the expensive piece renderer too. First-use `painterResource` decoding occurs from composition, and every card retains a reference to the full-resolution asset.

## Additional findings

### CPU, allocation, and recomposition

- `GameViewModel.kt:169` allocates a range set and difference set for every correct answer, then selects an uncontrolled random piece.
- one correct answer produces multiple `StateFlow` writes: piece state first, then score/combo/coins/question. Consumers can observe intermediate state and recompose twice.
- `StartScreen.kt:45` keeps a mutable `Set` in composition and repeatedly copies it on arbitrary delays; it reuses the full puzzle renderer for decoration.
- `GalleryScreen.kt:43` recreates the category list and `GalleryScreen.kt:135` filters the puzzle list during composition.
- `GalleryScreen.kt:143` supplies neither item keys nor content types.
- repeated transparent backgrounds, borders, per-piece shadows, and clips increase GPU overdraw and off-screen rendering risk.

### Bitmap and asset handling

- the source has one puzzle bitmap and no thumbnail/active-texture distinction;
- both PNGs are in a densityless `drawable` folder, which lint flags;
- there is no manifest, source hash, geometry cache, atlas, or validation stage;
- runtime uses Compose clipping to simulate pieces rather than precomputed vertices and UVs.

### State, persistence, and lifecycle

- state is lost on process death and no legacy migration contract exists;
- `collectAsState` continues to be rooted in composition rather than explicit lifecycle state;
- timer pause/resume follows ViewModel lifetime, not foreground/gameplay visibility;
- sound events and UI mutations share one ViewModel without an explicit renderer command boundary;
- `MusicManager.kt:73` creates a new unmanaged main-thread scope for each transition, so fades can overlap and outlive the intended owner;
- `MediaPlayer.create` occurs inside that main-thread job at `MusicManager.kt:89`;
- caught audio exceptions are silently discarded;
- reset progress is immediate and non-transactional, with no confirmation.

### Build and release behavior

- at baseline, release optimization was disabled and no profileable variant, Macrobenchmark module, Baseline Profile generator, R8 verification, or resource shrinking configuration existed;
- lint baseline is 0 errors, 55 warnings, and 2 hints; warnings are not suppressed for this audit.

## Observed versus expected

Observed:

- baseline assemble, unit test, and lint tasks pass;
- APK and reports are produced;
- code and assets contain the patterns cited above;
- no Android device is connected.

Expected architectural improvements, not yet measured:

- one libGDX surface should substantially reduce composition/layout work for the board;
- precomputed meshes and one active texture should remove draw-time path creation and runtime slicing risk;
- dedicated thumbnails should reduce gallery decode and memory pressure;
- fine-grained lifecycle-aware UI state and explicit commands should reduce invalidation and replay bugs;
- structured renderer/audio ownership should prevent jobs and native resources outliving gameplay.

## Resolution status

- Findings 1 and 2: replaced in gameplay by one libGDX surface consuming precomputed mesh data. The legacy Compose renderer was removed after replacement verification, including connected-emulator gameplay and lifecycle validation.
- Finding 3: the infinite progress gradient was replaced by a bounded progress animation; renderer animation remains off Compose state.
- Finding 4: root collection is now lifecycle-aware. Further screen-level state slicing can still reduce parameter invalidation.
- Finding 5: the generator emits a 768 px active texture and 320 px thumbnail; title/gallery consume the thumbnail.
- Music fade scopes are now owned/cancelled; Room/DataStore operations use structured scopes and transactional repository APIs.
- A non-debuggable, profileable, minified/resource-shrunk benchmark target and separate Macrobenchmark/BaselineProfileRule module now compile.

These changes are verified structurally, by build/tests, and by connected-emulator functional, lifecycle, and diagnostic Macrobenchmark runs recorded in `docs/PERFORMANCE_RESULTS.md`. Physical-device frame pacing, thermals, memory ceilings, audio compatibility, and high-refresh behavior remain unmeasured.

## Measurement plan

Create repeatable Macrobenchmark journeys for cold start, title-to-gameplay, first reveal, ten rapid answers, completion, gallery scroll, settings changes, background/resume, and repeated gameplay entry/exit. Use a profileable/release-like target, Baseline Profiles, Perfetto, allocation diagnostics, and `dumpsys gfxinfo`. Record device, OS, refresh rate, build type, iterations, thermal state, and raw outputs in `docs/PERFORMANCE_RESULTS.md`; never substitute debug impressions for measurements.
