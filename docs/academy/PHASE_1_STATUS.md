# Academy Phase 1 status

Last updated: 2026-07-16.

## Scope gate

Owner decisions are recorded in `docs/academy/FOUNDATION_SCOPE.md`. The approved scope is Philippine DepEd MATATAG Grade 2 Quarter 1 Number and Algebra, English mathematical terminology, local-only supplementary formative practice, and no diagnostic/grading/placement claims.

## Baseline

- `android info`, process-local `android describe`, the toolchain doctor, and Gradle Wrapper validation passed.
- The original seven-module graph was confirmed with `projects`; Phase 1 then added the eighth module, `core-learning`.
- `:core-model:test`, `:core-game:test`, `:core-data:testDebugUnitTest`, and `:app:assembleDebug` passed before source changes.
- No connected device was present during the baseline doctor run.
- Android CLI’s combined `platform-tools|build-tools|platforms` package filter listed installed platform-tools but returned exit code 1; the doctor independently validated platform-tools and installed build-tools.
- Existing uncommitted `docs/studies/jigsaw-math-academy-platform.md` is preserved.

## Implementation state

Complete for the authorized local foundation:

- `core-learning` is a pure Kotlin/JVM boundary with validated stable IDs, taxonomy version 1, immutable versioned items/attempts, explicit supersession/invalidation fields, and deterministic evidence-policy version 1.
- `DefaultMathQuestionGenerator` emits deterministic addition items with generator/content/configuration provenance. `JigsawLearningItemFactory` maps reviewed symbolic additions to regrouping or no-regrouping skills without inferring misconceptions from random distractors.
- Room schema 2 adds append-only `learning_attempts` and transactionally updated `learning_sessions`; `MIGRATION_1_2` preserves schema-1 progress/session rows and creates no historical item attempts.
- `GameViewModel` records one attempt for each accepted answer through an independent serialized persistence queue. Renderer acknowledgement advances the puzzle but creates no attempt.
- `LearningSummaryScreen` displays cautious local statuses, recent evidence, item variety, last-practiced information, and an explicit empty/insufficient-evidence state.
- Reset All Progress removes existing progress/session data and local learning evidence in one Room transaction. Android backup/transfer excludes the Room database.

## Verification

- Pure and Android host tests cover ID/taxonomy validation, deterministic reproduction, retry/assistance representation, policy determinism/diversity/recency/response-time exclusion, mappings, and ViewModel exactly-once behavior.
- Connected API 37 emulator tests pass: 5 `core-data` migration/DAO/reset tests and 6 app summary accessibility/large-text tests.
- Manual benchmark-APK validation recorded a correct answer, renderer-finished piece reveal, one evidence summary, and transactional reset back to the empty state.
- The focused five-iteration Macrobenchmark `firstIncorrectAnswerToLearningSummary` passes and verifies local evidence observability. Its emulator frame metrics are diagnostic only and are recorded in `docs/PERFORMANCE_RESULTS.md`.

## Limitations requiring validation

- The 13 skill definitions and all evidence thresholds are product policy, not educator-validated curriculum or psychometric conclusions.
- Current Jigsaw content emits evidence only for symbolic addition with/without regrouping. Other approved taxonomy skills have no reviewed content yet.
- Derived summaries recompute when Room emits; time passing alone does not refresh an already displayed recency status until a later repository emission or screen recreation.
- Attempts are intentionally retained until Reset All Progress/app-data deletion; broader deployment needs a measured retention policy.
- No physical-device performance result was produced.

Number Line Expedition, teacher mode, learner accounts, cloud/backend work, synchronization, challenges, social functionality, and predictive mastery remain deliberately out of scope.
