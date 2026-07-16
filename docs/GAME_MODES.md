# Local game modes

`GameModeCatalog` in `core-model` is the authoritative catalog and tuning source. Classic remains first, recommended, and the fallback for null or unknown persisted IDs. Every session receives one immutable resolved `GameModeDefinition`; `core-game` interprets its clock, mistake, piece-penalty, completion, and score-modifier policies without Android or renderer dependencies.

## Rules and default tuning

| Mode | Clock | Mistake / piece rule | Success and failure |
|---|---|---|---|
| Classic | None | Wrong answer resets combo; revealed pieces stay visible | Complete after every final reveal acknowledgement |
| Time Attack | 90-second session | Wrong answer resets combo | Complete the board or time expires; completion adds 2 score points per whole remaining second |
| Survival | 10/8/6-second question deadline on Easy/Medium/Hard | Start with 3 hearts; a wrong or late answer costs one | Complete the board or finish with `OUT_OF_HEARTS` |
| Puzzle Decay | 9-second decay interval and 180-second maximum run | Correct answer restarts decay; wrong answer schedules one visible piece removal; decay expiry does the same | Complete the board or reach the maximum duration |
| Combo Rush | Start at 45 seconds; maximum remaining 60 seconds | Correct answer adds 2.5 seconds; wrong answer removes 4 seconds and resets combo | Complete the board or time expires |

No heart restoration milestone is enabled. That keeps Survival state unambiguous and ensures every heart change comes from one documented rule.

## Timer semantics

The reducer never reads Android time and never receives display ticks. It emits `TimerScheduled`, `TimerCancelled`, and `TimerAdjustmentRequested`, and accepts only an exact `TimerExpired(GameTimerId)`. IDs carry session, timer kind, and generation, so old callbacks after an answer or restart are unchanged transitions.

`ModeTimerCoordinator` owns monotonic deadlines in the app layer. Pause snapshots exact remaining durations and cancels scheduled callbacks; resume anchors them to the new monotonic time. Leaving gameplay abandons the active run and cancels all timers. Compose receives coarse `TimerUiAnchor` values and interpolates countdown text/bars locally with the frame clock; that presentation does not write `GameUiState` or reduce `Tick` actions. If an answer and timeout race, the first matching semantic action changes the question/timer generation and the second is rejected.

Exact active-session restoration after process death is not implemented. Permanent completion and completed/failed/abandoned session records survive; a killed mid-session run does not resume from exact pieces or deadlines.

## Board mutations

Correct answers schedule `PendingBoardMutation(REVEAL)` and Puzzle Decay schedules `REMOVE`. The committed `PieceSet` changes only after `BoardMutationFinished` carries the exact session-scoped mutation ID. A single pending operation prevents reveal/remove conflicts. Stale and duplicate acknowledgements are ignored. The renderer controller retains the committed snapshot and uncommitted operation, so a recreated GL surface can replay it; the retained operation is cleared only when the next committed snapshot proves the reducer accepted it.

## Persistence and permanent progress

Room schema version 2 stores mode/outcome/session metrics and a `mode_bests` row keyed by puzzle ID and mode ID. Scores, fastest completion, highest combo, fewest mistakes, and most recent completion are updated independently; incomparable metrics are not collapsed into one ranking.

- Completing any mode permanently completes the puzzle.
- Classic may update aggregate partial gallery progress after acknowledged reveals.
- An unfinished challenge records session statistics but never reduces or replaces permanent progress.
- Puzzle Decay removals exist only in the active session.
- Reset All Progress transactionally clears progress, sessions, and mode bests while DataStore preferences remain.
- Unknown future mode strings remain in Room rows; runtime selection falls back to Classic without rewriting them.

DataStore stores only preferences plus `last_selected_mode`. Unknown values resolve to Classic. Classic remains the default on a new install.

## Result metrics

Mode results distinguish `COMPLETED`, `TIME_EXPIRED`, `OUT_OF_HEARTS`, `DECAY_LIMIT_REACHED`, and `ABANDONED`. Session rows include puzzle, mode, outcome, score, duration, correct and wrong answers, timeout count, maximum combo, pieces revealed/removed, remaining lives when applicable, and end timestamp. Failure never awards puzzle completion.

## Accessibility and child-friendly presentation

Mode cards expose one summary containing timer and penalty information. Classic is labeled recommended/relaxed. HUDs show only relevant timers, hearts, combo, progress, and loss feedback. Timer bars animate visually, but accessibility announcements are bounded to events such as ten seconds remaining or one heart remaining—never every tick. Failure copy uses “Great Effort,” “Try Again,” “Out of Hearts,” or “Run Over.” Survival is the visible name; no violent language or imagery is used. Reduced motion changes transient feedback to opacity/text while retaining the untimed Classic route.

## Adding another local mode

1. Add a stable `GameModeId` persisted value; never rename an existing value.
2. Add one immutable definition to `GameModeCatalog` using existing policy types. Extend a policy type only when the rule cannot be expressed clearly.
3. Add reducer tests for answer/timeout ordering, completion/failure, pause/restart, and seed reproducibility.
4. Map only the new coarse HUD/result fields in `GameViewModel`; do not implement rules as ViewModel branches.
5. Add accessible mode-card copy and child-friendly event/result text.
6. Persist the stable mode ID and add independent best-metric tests. A schema change requires a new explicit migration/export.
7. Add benchmark/device coverage for any new renderer or lifecycle behavior.

Safe tuning values are durations, heart costs/counts, timer bonuses/penalties/caps, decay interval, maximum duration, and remaining-time score points. Never tune away stable IDs, semantic timer generations, renderer acknowledgement, one-pending-mutation exclusivity, Classic fallback/default, pause semantics, or the permanent-progress rules above. True online player-versus-player play would require an authoritative backend and is outside this local/offline system.
