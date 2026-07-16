# ADR 0005: Room and DataStore persistence boundary

Status: accepted, 2026-07-16.

## Context

Jigsaw Math persists two materially different kinds of state. Puzzle progress and completed sessions require structured queries, atomic multi-row updates, schema history, and migration tests. User settings are small independent key/value preferences observed as a flow. The prototype kept both categories in memory and had no durable migration boundary.

## Decision

Use Room in `core-data` for puzzle progress and game session summaries. Completion updates the progress row and inserts its session in one `@Transaction`; reset deletes progress and sessions in one transaction. Export every Room schema under `core-data/schemas/`, increment the database version for schema changes, and require explicit migrations rather than destructive fallback.

Use Preferences DataStore for sound/music enablement and volume, haptics, difficulty, graphics quality, reduced motion, and the one-time legacy-migration flag. Preserve established keys or provide tested compatibility when a preference changes.

Expose both stores only through repository interfaces and domain/entity mappings. Compose and libGDX do not access either store directly. Database and DataStore work runs in structured application/ViewModel scopes, never in composition or the renderer loop.

Keep `LegacyProgressMigrator` as an idempotent import boundary guarded by the DataStore flag. The current application uses `EmptyLegacyProgressSource` because no durable legacy store was found; a real source must not be claimed or wired without a tested reader.

Large images, textures, generated geometry, and mesh blobs remain versioned application assets, not database rows.

## Consequences

- progress completion and reset are atomic and testable;
- settings updates remain asynchronous and lifecycle-observable;
- Room schema JSON is a committed review artifact;
- future exact-piece/resumable-state work requires an explicit schema version and migration;
- reset progress intentionally leaves user preferences intact;
- current aggregate progress cannot restore exact mid-session piece identity;
- unlock state, coins, background music mode, and optional confetti need separate persistence decisions before they can be described as durable.
