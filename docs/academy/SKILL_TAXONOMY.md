# Grade 2 Quarter 1 skill taxonomy

Status: implemented for Academy Phase 1, taxonomy version 1.

## Identity rules

Canonical skills use lowercase namespaced IDs beginning with `math.`. IDs describe a mathematical construct, not a country, grade, activity, learner, renderer, or screen. Curriculum mappings separately identify the Philippine DepEd MATATAG Grade 2 Quarter 1 context.

An ID is immutable once attempts reference it. Wording, translations, and curriculum alignment may receive new versioned metadata without rewriting historical attempts. A materially different construct receives a new ID. Retired skills remain readable, are marked `RETIRED`, and may name a replacement; their historical attempts are not reassigned silently.

## Version-1 skills

| Stable ID | English title | Initial Jigsaw evidence |
|---|---|---|
| `math.whole-number.represent-to-1000` | Represent whole numbers to 1,000 | no reviewed item yet |
| `math.whole-number.skip-count-to-1000` | Skip-count within 1,000 | no reviewed item yet |
| `math.whole-number.compare-to-1000` | Compare whole numbers to 1,000 | no reviewed item yet |
| `math.whole-number.order-to-1000` | Order whole numbers to 1,000 | no reviewed item yet |
| `math.place-value.hundreds-tens-ones` | Identify hundreds, tens, and ones | no reviewed item yet |
| `math.place-value.digit-place-and-value` | Determine a digit’s place and value | no reviewed item yet |
| `math.addition.number-line-count-up` | Add by moving and counting up on a number line | no reviewed item yet |
| `math.addition.expanded-form` | Add using expanded form | no reviewed item yet |
| `math.addition.to-1000-without-regrouping` | Add to 1,000 without regrouping | reviewed symbolic Jigsaw items |
| `math.addition.to-1000-with-regrouping` | Add to 1,000 with regrouping | reviewed symbolic Jigsaw items |
| `math.addition.property.zero` | Understand the zero property of addition | no reviewed item yet |
| `math.addition.property.commutative` | Understand the commutative property of addition | no reviewed item yet |
| `math.addition.property.associative` | Understand the associative property of addition | no reviewed item yet |

No prerequisite edges are asserted in version 1 because the owner decision defines scope but does not establish a reviewed prerequisite graph. The validator still rejects duplicate IDs, unknown references, self-dependencies, and cycles when reviewed edges are later added.

## Adding or changing a skill

1. Obtain curriculum/educator review for the construct, learner wording, mapping, and any prerequisite relationship.
2. Add the canonical skill and versioned mapping in `core-learning/src/main/kotlin/com/qtpie/simplepuzzle/core/learning/Grade2Quarter1Taxonomy.kt`.
3. Run taxonomy uniqueness, mapping, and cycle tests.
4. Add or update deterministic content tests showing which items map to it.
5. Update this document and the local data inventory if stored interpretation changes.
6. Never reinterpret existing attempts in place; introduce a mapping/content/policy version and recompute derived summaries explicitly.

## Tagging a question

Only an item intentionally authored or generated for a reviewed construct receives that skill. Current random distractors have no reviewed misconception meaning, so selecting one records an incorrect structured response but no misconception tag. Representation, authored difficulty, generator/content versions, deterministic seed, and template family are stored with the item reference.
