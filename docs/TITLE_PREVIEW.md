# Animated title puzzle preview

## Ownership

`StartScreen` hosts `TitlePuzzlePreview`, which owns one `FragmentContainerView` containing `PuzzlePreviewRendererFragment`. The fragment owns a preview-only controller and configures the shared libGDX `PuzzleRenderer`; it does not use `GameViewModel`, `GameState`, audio, haptics, Room, or DataStore. Compose exposes one stable “Animated jigsaw puzzle preview” description and never creates one node per piece.

The preview reuses the generated package texture, precomputed mesh ranges, shader transform, fixed-step clock, reveal/removal animation, and bounded `ParticlePool`. It creates no texture or mesh in `render()` and performs no manifest I/O after `create()`.

## Puzzle selection and catalog

`puzzles/catalog.json` is the source-controlled authority. `generatePuzzleCatalog` validates each source definition against its generated package and writes `app/src/main/assets/puzzles/catalog.json`. `AndroidPuzzleCatalog` parses that runtime file once at application startup, validates format, ID, piece count, manifest, and texture presence, and exposes only valid preview candidates. One invalid package is skipped rather than crashing the title. With no valid package, StartScreen uses the generated Cosmic Journey thumbnail as a static fallback.

A process-scoped seed selects one valid package for the title. The current catalog contains only Cosmic Journey, so it loops gracefully. With two or more real packages, `PreviewPackageSequence` chooses a deterministic non-repeating order and a 15–25 second interval. `PuzzleRenderer` waits until all queued piece operations finish, emits one idle-boundary signal, and stops scheduling preview work. The host then synchronously replaces the preview Fragment with the next package, keeping manifest/texture replacement out of the render loop and avoiding a mid-animation transition.

## Deterministic sequence

`PreviewSequenceGenerator(pieceCount, seed, quality)` is pure. It:

- selects an initial occupancy from 25–45 percent;
- maintains a 20–80 percent bound;
- selects non-conflicting reveal/remove operations;
- avoids immediately toggling the same piece when another valid piece exists;
- biases toward reveal when low and removal when high;
- produces deterministic delays and operations for a seed.

All operations in a step are chosen before animation and are executed sequentially. The renderer starts the next step only after the current bounded queue completes.

## Quality and reduced motion

| Profile | Operations per step | Delay | Effects | Frame cap |
|---|---:|---:|---|---:|
| LOW | 1 | 1.3–1.8 s | no optional removal particles | 30 FPS |
| MEDIUM/AUTO | 1–2 | 0.9–1.3 s | restrained bounded particles | 30 FPS |
| HIGH | 1–3 | 0.8–1.2 s | richer but pool-bounded effects | 30 FPS |

Reduced motion keeps the deterministic initial partial board static. It uses no rotations, bouncing, particle bursts, or frequent crossfades.

## Lifecycle and teardown

The preview and gameplay use different Fragment tags and container IDs, but are never active together. Play and Settings handlers first find the preview Fragment and synchronously `commitNow { remove(fragment) }` while its GL surface is attached, then navigate. Returning to title recreates the surface. AndroidView `onRelease` provides the same removal behavior for configuration/recomposition cleanup. Backgrounding pauses the backend; resume resets the fixed-step accumulator and clamps delta.

Gameplay toolbar and system back both use the gameplay controlled teardown: abandon/cancel session timers, synchronously remove `PuzzleRendererFragment`, then pop to the gallery. Repeated title/gallery/game navigation is covered by instrumentation and benchmark journeys.

## Performance and accessibility invariants

- one active puzzle texture and precomputed geometry;
- no runtime crop, triangulation, thumbnail generation, database access, audio, or haptics;
- no per-frame Compose or StateFlow update;
- no recurring large render-loop collection;
- bounded active operation queue and particle arrays;
- no simultaneous title/gameplay renderer surface;
- stable preview description only; piece changes are never announced;
- preview bounds do not overlap or intercept Play/Settings controls.
