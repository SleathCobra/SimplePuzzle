# ADR 0006: Game-mode policy architecture

Status: accepted, 2026-07-16.

## Context

Classic gameplay is currently expressed directly by one pure reducer while elapsed-time display is maintained by an Android `delay(1000)` loop. Adding Time Attack, Survival, Puzzle Decay, and Combo Rush as independent ViewModel branches would duplicate scoring/progression logic, mix Android timing with domain rules, and make answer/timeout races difficult to reproduce. The ordinary untimed learning experience must remain the default.

## Decision

Represent every local mode with a stable `GameModeId` and one immutable `GameModeDefinition` composed from clock, mistake, piece-penalty, completion, and score-modifier policies. `GameModeCatalog` is the sole tuning source and resolves unknown persisted IDs to Classic. `GameConfiguration` carries both the selected ID and resolved definition so a running session never observes mutable or remote tuning.

`DefaultGameEngine` remains the only pure deterministic reducer. Shared answer, score, combo, question, board-mutation, and completion behavior stays centralized; small policy handlers interpret the resolved rules. Mode-specific Android, Compose, Room, DataStore, and libGDX branches are forbidden in `core-game`.

The app layer owns a lifecycle-aware coordinator backed by an injected monotonic clock. The reducer emits and accepts semantic timer directives/expirations identified by session and timer generations; it is never reduced for display frames or timer ticks. Compose receives coarse timer anchors and animates presentation locally. Pause/resume preserves remaining monotonic duration, navigation cancels the session, and stale callbacks are ignored by identity.

Classic has no global or question timer and is the safe default. Challenge sessions may persist results, but unfinished challenges never reduce permanent gallery progress. Completing any mode permanently completes the puzzle. Exact mid-session process-death restoration remains outside the current persistence scope and must be described as such.

## Consequences

- mode tuning is discoverable, deterministic, and testable in one catalog;
- new local modes compose existing policies instead of adding unrelated ViewModel flows;
- timer behavior can be tested with fake monotonic time and no real delays;
- UI can render mode-specific HUD/results without owning gameplay rules;
- Room stores stable string IDs so future unknown values can be preserved while runtime behavior falls back safely;
- adding a mode requires catalog, reducer-policy, UI, persistence, accessibility, and deterministic test coverage;
- true online player-versus-player play would require an authoritative backend and remains explicitly outside this local/offline architecture.
