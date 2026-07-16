# ADR 0007: Renderer-acknowledged board mutations

Status: accepted, 2026-07-16.

## Context

The reducer currently delays a reveal commit until libGDX reports that the animation finished. Puzzle Decay also needs to remove committed pieces without allowing renderer timing to mutate domain state, persistence, or completion directly. Separate reveal/removal callback state machines would make stale callbacks, surface recreation, and conflicting operations harder to reason about.

## Decision

Use one `PendingBoardMutation` model with a session-scoped `BoardMutationId`, piece ID, and `REVEAL` or `REMOVE` type. The pure reducer allows at most one pending board mutation, validates that reveals target hidden pieces and removals target committed visible pieces, and commits only an acknowledgement carrying the exact active mutation ID. Stale and duplicate acknowledgements are unchanged transitions.

The renderer command API carries the same mutation identity. `SetVisiblePieces` always represents committed domain state. On surface recreation the app resubmits that snapshot and the pending mutation; replay is visually safe and reducer acknowledgement is idempotent. Renderer completion never accesses the reducer, database, or Compose state directly.

Removal reuses the package texture and precomputed mesh. The renderer temporarily excludes the active removal piece from the static batch, draws that existing mesh range with bounded transform/alpha animation, and acknowledges only after completion. Reduced motion uses a short fade; LOW quality suppresses optional removal particles. No texture, crop, geometry, or database work is added to the render loop.

## Consequences

- final reveal completion remains renderer-acknowledged;
- temporary removals cannot directly corrupt permanent progress;
- mutation identities reject callbacks from earlier sessions and restarts;
- renderer recreation can replay one pending operation without double commits;
- future board operations must extend this typed contract and its reducer/renderer tests rather than add independent callbacks.
