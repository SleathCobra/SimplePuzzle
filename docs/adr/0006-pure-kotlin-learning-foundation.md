# ADR 0006: Pure Kotlin learning foundation boundary

Status: accepted, 2026-07-16.

## Context

The existing `core-game` reducer is intentionally jigsaw-specific, while credible learning evidence must be reusable across future activities and independent of Android, rendering, persistence, and wall-clock/global-ID side effects. Putting generic contracts into `app`, `core-data`, or `renderer-gdx` would invert existing dependencies; renaming all puzzle concepts would destabilize the verified game slice.

## Decision

Add `core-learning` as a Kotlin/JVM module for validated identifiers, versioned skill/curriculum/activity/item contracts, immutable attempts, deterministic evidence, taxonomy validation, and personal summaries. It has no Android, Compose, libGDX, Room, DataStore, WorkManager, network, system-clock, or uncontrolled random-ID dependency.

`core-game` remains jigsaw-specific and depends on `core-learning` only through a pure Jigsaw learning-item/attempt adapter. `core-data` may persist `core-learning` models. `app` orchestrates the reducer, adapter, repository, and summary UI. `renderer-gdx` remains independent of learning persistence.

## Consequences

- Future activities can share evidence contracts without inheriting puzzle-piece phases.
- Jigsaw correctness and reveal-animation completion remain separate; an answer records evidence once, while reveal completion only advances puzzle progression.
- Clocks and ID sources are injected for deterministic tests.
- Adding a new activity still requires a reviewed activity-specific adapter and content tests; `core-learning` is not a generic renderer or plugin framework.
