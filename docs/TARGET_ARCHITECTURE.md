# Target architecture

## Boundaries

The migration target is a buildable set of one-way dependencies:

```text
app / feature UI
  -> core-game -> (core-model, core-learning)
  -> core-data -> (core-model, core-learning)
  -> core-learning
  -> renderer-gdx -> core-model

asset-pipeline -> core-model
benchmark -> app benchmark artifact
```

Compose owns conventional Android UI and accessibility. `core-game` owns deterministic Jigsaw behavior and its learning adapter. `core-learning` owns versioned, activity-independent formative evidence contracts and policy. `core-data` owns persistence and migration. `renderer-gdx` owns frame-timed board state and native graphics resources. `asset-pipeline` owns all expensive, reproducible puzzle preprocessing.

## State ownership

- Domain state is immutable and changes only through `GameAction` reduction.
- Compose observes coarse lifecycle-aware screen state.
- One-shot renderer work is expressed as `RendererCommand` and acknowledged explicitly.
- Renderer animation arrays and particle pools never enter `StateFlow`.
- Room and DataStore expose repository flows and never run from composition or `render()`.
- Accepted answers create append-only evidence independently of renderer acknowledgement; the acknowledgement advances only logical puzzle progression.

## Renderer invariants

- image decode, manifest I/O, texture creation, shader creation, and mesh upload occur during renderer creation or asset changes;
- `render()` performs bounded command draining, fixed-step updates, and reusable GPU draws;
- delta time is clamped after resume;
- transient effects are pooled;
- renderer resources have one owner and deterministic disposal;
- configuration changes reuse an Activity-scoped command controller while the Fragment backend recreates the surface resources;
- future multi-puzzle support swaps packages at a lifecycle boundary, not inside the hot loop.

## Persistence target

Room schema 2 stores aggregate puzzle progress, best scores, append-only local learning attempts, completion timestamps, and game/learning session summaries. DataStore stores preferences only. A future schema version will persist exact revealed piece IDs and resumable reducer state; large images and mesh blobs remain generated assets, never database rows.

## Asset implementation

Format 2 implements deterministic tab topology, complementary adjacent boundaries, triangulated concave silhouettes, reveal ordering, schema/version/hash validation, and dedicated textures/thumbnails. Generator output is cacheable, reproducible, and Android-runtime independent. Optional quality-specific textures and a shared effects atlas remain future package extensions.

## Verification target

Host tests cover rules, mapping, serialization, generation, timing, pools, and command ordering. Device tests cover Room, navigation, lifecycle/surface recreation, accessibility, screenshots at multiple window sizes, and repeated gameplay entry/exit. Macrobenchmark and Perfetto measurements use the minified profileable variant on declared hardware; performance claims always include device, OS, refresh rate, build, iterations, and raw trace/report location.
