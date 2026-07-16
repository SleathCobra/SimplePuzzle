# ADR 0008: Title-preview renderer lifecycle

Status: accepted, 2026-07-16.

## Context

The title previously decoded and displayed a static generated thumbnail in Compose. A calm animated board should reuse the generated texture, meshes, fixed-step animation, and pools without rebuilding the removed per-piece Compose scene graph or creating gameplay state. The Android libGDX Fragment backend also requires removal while its surface is attached; title navigation must not recreate the earlier pause timeout or overlap a gameplay surface.

## Decision

Host the decorative board in one `PuzzlePreviewRendererFragment` with its own controller and the shared `PuzzleRenderer` configured by immutable `PuzzlePreviewConfiguration`. `PreviewSequenceGenerator` is pure and deterministic for a seed. It chooses an initial 25–45 percent occupancy and bounded reveal/removal steps while keeping occupancy between 20 and 80 percent. The preview reads only a package already validated by the application puzzle catalog.

The preview has no `GameState`, reducer, ViewModel session, Room, DataStore, sound, haptic, score, or question dependency. It loads one generated package during renderer creation, uses the existing texture/precomputed mesh/removal primitive, and caps rendering at 30 FPS. LOW/MEDIUM/HIGH select one, up to two, or up to three operations per step. Reduced motion keeps the deterministic initial partial board static.

Title navigation synchronously removes `PuzzlePreviewRendererFragment` with `commitNow` before the Compose route changes. Returning to title creates it again. Gameplay uses a different tag/container/controller, and title-to-gallery removal happens before gameplay can be entered. Fragment `onRelease` remains the configuration/recomposition safety net. Activity pause/resume is delegated to the backend, whose fixed-step clock resets and clamps resumed delta.

The source-controlled `puzzles/catalog.json` is authoritative. Its build-generated runtime catalog contains only packages whose generated manifests agree with the definition and package path. Invalid packages are omitted from preview candidates; if no valid candidate remains, Compose shows the generated thumbnail fallback. The current repository has one valid package and loops it. With multiple valid packages, the renderer emits a package-switch signal only after its active operation queue is idle; the host synchronously replaces the preview Fragment, so the next manifest/texture is loaded by `create()` and never by `render()`.

## Consequences

- the title uses one bounded GPU surface and no per-piece Composables;
- decorative activity cannot mutate progress or emit gameplay feedback;
- title and gameplay renderer ownership is mutually exclusive;
- reduced motion and missing-package fallback remain calm and usable;
- future multi-package switching must preserve controlled Fragment teardown and perform package I/O only during renderer creation.
