# Local learning evidence policy

Status: implemented Phase 1 product policy, evidence-policy version 1. It requires educator and pilot validation.

## Raw facts and derived summaries

`LearningAttempt` is an append-only raw fact: what versioned item was shown, the structured selected and expected responses, outcome, assistance, retry ordinal, device event time, optional reliable elapsed time, and application/content/engine provenance. Existing aggregate puzzle progress is preserved but never expanded into item attempts that were not recorded.

`SkillEvidence` and `PersonalSkillSummary` are deterministic derived interpretations. They can be recomputed under a later policy without rewriting raw attempts. A correction is a new superseding/invalidation record, never an update to the earlier row.

## Initial weighting assumptions

- Each template family contributes at most its latest non-invalidated attempt to the current summary. Earlier near-duplicate exposures remain in raw history but receive zero current-summary weight.
- An unassisted first-attempt answer has full weight.
- A correct retry has 70% of full weight.
- A correct answer after hints has at most 60% of full weight.
- A correct answer after the expected answer was revealed has 20% of full weight.
- Incorrect attempts use the same assistance and recency weighting, contribute in the review direction, and are not a permanent negative score.
- Evidence up to 14 days old has full recency weight; 15–45 days has 80%; 46–90 days has 50%; older evidence has 25%.
- Elapsed response time is stored only when reliable and is ignored by status calculation.

Integer basis points are used so recomputation is deterministic and avoids unexplained floating-point thresholds.

## Status rules

- `INSUFFICIENT_EVIDENCE`: fewer than five distinct contributing template families.
- `PRACTICED_RECENTLY`: at least five families, the most recent evidence is within 14 days, and weighted correctness is at least 80%.
- `REVIEW_SUGGESTED`: at least five families and weighted correctness is below 60%.
- `DEVELOPING`: sufficient diversity that meets neither of the preceding rules.

The UI shows the evidence count, template diversity, last-practiced information, and plain-language review suggestion. It does not show the internal weighted ratio as a mastery percentage.

## Determinism and limitations

The caller supplies an explicit `asOfEpochMillis`; the policy does not read the system clock. Identical attempts, taxonomy/content references, policy version, and as-of time produce identical evidence and summary.

These thresholds are an initial transparent product policy, not a validated psychometric model. They do not implement Elo, Bayesian Knowledge Tracing, Item Response Theory, diagnosis, placement, grading, or a claim of durable mastery.
