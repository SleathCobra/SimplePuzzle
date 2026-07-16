# Academy Phase 1 local data inventory

Status: implemented for Academy Phase 1. All learning evidence remains on the Android device.

| Data | Purpose | Stored where | Retention / deletion |
|---|---|---|---|
| Attempt and session opaque IDs | append-only identity and duplicate prevention | Room | until Reset All Progress or app data removal |
| Activity/item/template/content/policy versions and deterministic generator provenance | reproduce and interpret what was shown | Room attempt payload | same as attempt |
| Canonical skill ID and representation/difficulty metadata | derive personal practice summaries | Room attempt payload/index | same as attempt |
| Structured selected/expected integer response and correctness | raw formative evidence and content debugging | Room attempt payload | same as attempt |
| Assistance and retry ordinal | avoid treating assisted/repeated work as independent evidence | Room attempt payload | same as attempt |
| Device event time and optional reliable elapsed time | recency/usability context | Room attempt payload | same as attempt; elapsed time never changes status alone |
| Derived personal skill summary | learner-facing recent practice guidance | recomputed from Room; not authoritative raw data | disappears when attempts are deleted |
| Existing puzzle progress and game session aggregates | preserve current gameplay progression and scores | existing Room tables | existing reset behavior, now in the same transaction as evidence deletion |

The implementation stores no learner name or account identifier in learning contracts. It adds no email, date of birth, precise location, advertising ID, contacts, photos, audio/video, free text, remote analytics, crash-reporting SDK, or network transmission.

Android cloud backup and device-transfer rules exclude the Room database containing evidence. Reset All Progress deletes puzzle progress, session aggregates, learning attempts, and learning session summaries transactionally; DataStore preferences remain unchanged. Uninstalling the app or clearing app data also removes the local database.

No automatic export exists in Phase 1. Inspect local evidence only with development/database tooling on a controlled test device; do not copy a real child’s database into source control, issue trackers, logs, screenshots, or benchmark artifacts.
