# ADR 0007: Append-only local attempts and derived evidence

Status: accepted, 2026-07-16.

## Context

Current Room data contains aggregate puzzle progress and completed-session summaries. Those rows cannot truthfully reconstruct which mathematical item was shown or selected. Personal learning summaries need item-level provenance while preserving existing saves and avoiding permanent or opaque learner labels.

## Decision

Room schema version 2 adds append-only learning attempts and a transactionally maintained local learning-session summary. Attempt IDs are primary keys and inserts use ignore-on-conflict semantics for idempotency; production code exposes no update/delete-by-attempt operation. Corrections are new records with explicit supersession/invalidation metadata. Existing aggregate rows remain untouched and are never expanded into fabricated attempts.

Derived `SkillEvidence` and `PersonalSkillSummary` are recomputed with a named deterministic policy. The first policy uses correctness, assistance, retry, recency, and template diversity; response time never changes status by itself. `INSUFFICIENT_EVIDENCE` is a first-class result.

Reset All Progress deletes existing progress/session rows and learning evidence in one Room transaction. Preferences remain in DataStore. The Room database is excluded from automatic Android cloud backup/device transfer during the local child-data pilot.

## Consequences

- Raw evidence remains auditable under later policy versions.
- Duplicate lifecycle/configuration delivery cannot create a second row for the same attempt ID.
- Storage grows with practice and requires a future measured retention policy before broad deployment.
- Phase 1 stays local-only and introduces no account, cloud ID, synchronization queue, analytics SDK, or network transport.
