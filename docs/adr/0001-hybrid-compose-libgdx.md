# ADR 0001: Hybrid Compose and libGDX UI

Status: accepted, 2026-07-15.

## Context

The prototype modeled each puzzle piece as a Compose subtree with clipping, shadows, borders, animation, and a translated full image. This mixed conventional UI and frame-timed rendering, causing broad invalidation and repeated path work.

## Decision

Compose remains the application shell and gameplay HUD. One `AndroidFragmentApplication` surface renders the puzzle board. `MainActivity` is a `FragmentActivity`; Compose hosts a `FragmentContainerView`; `PuzzleRendererFragment` creates the libGDX listener with `initializeForView`.

An Activity-scoped `PuzzleRendererHostViewModel` owns only the command controller, allowing surface recreation without retaining an Activity in renderer code. Compose sends snapshots/commands and receives reveal acknowledgements on the main thread. Fragment removal disposes the backend surface; the listener disposes all GPU resources. Navigation exit removes the Fragment synchronously while its view is attached and only then pops the Compose route, preserving libGDX's pause synchronization contract. `MainActivity.exit()` remains a no-op because the embedded backend must never finish the Activity that owns the Compose shell.

libGDX 1.13.1 and KTX 1.13.1-rc1 use the matching release line. The later libGDX 1.14 line was not paired with a released compatible KTX line during implementation, so the build was not mixed speculatively.

## Consequences

- board animation no longer drives Compose state per frame;
- native libraries are extracted reproducibly for four ABIs through the AGP variant API;
- gameplay requires Fragment lifecycle integration inside the Compose Activity;
- emulator testing verified recreation/removal behavior, including five repeated enter/leave cycles with a stable process;
- other application screens stay idiomatic Compose.
