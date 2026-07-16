package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.GameAction
import com.qtpie.simplepuzzle.core.model.GameConfiguration
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GamePhase
import com.qtpie.simplepuzzle.core.model.GameState
import com.qtpie.simplepuzzle.core.model.BoardMutationId
import com.qtpie.simplepuzzle.core.model.BoardMutationType
import com.qtpie.simplepuzzle.core.model.PendingBoardMutation
import com.qtpie.simplepuzzle.core.model.ActiveGameTimer
import com.qtpie.simplepuzzle.core.model.GameEndReason
import com.qtpie.simplepuzzle.core.model.GameSessionStatistics
import com.qtpie.simplepuzzle.core.model.GameTimerId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import com.qtpie.simplepuzzle.core.model.ModeOutcome
import com.qtpie.simplepuzzle.core.model.PieceId
import com.qtpie.simplepuzzle.core.model.PieceSet
import com.qtpie.simplepuzzle.core.model.Score
import com.qtpie.simplepuzzle.core.model.Transition

fun interface ScoringPolicy {
    fun pointsFor(combo: Int): Int
}

object StandardScoringPolicy : ScoringPolicy {
    override fun pointsFor(combo: Int): Int {
        require(combo >= 1) { "Combo must be at least one." }
        return 10 * combo
    }
}

interface GameEngine {
    fun newGame(configuration: GameConfiguration): GameState

    fun reduce(state: GameState, action: GameAction): Transition
}

class DefaultGameEngine(
    private val random: RandomSource,
    private val questionGenerator: QuestionGenerator = DefaultMathQuestionGenerator(),
    private val scoringPolicy: ScoringPolicy = StandardScoringPolicy,
) : GameEngine {
    override fun newGame(configuration: GameConfiguration): GameState = GameState(
        puzzleId = configuration.puzzleId,
        revealedPieces = PieceSet.Empty,
        pieceCount = configuration.pieceCount,
        score = Score(0),
        combo = 1,
        question = questionGenerator.generate(configuration.difficulty, random),
        difficulty = configuration.difficulty,
        modeId = configuration.modeId,
        modeRules = configuration.modeRules,
        phase = GamePhase.READY,
        sessionGeneration = configuration.sessionGeneration,
    )

    override fun reduce(state: GameState, action: GameAction): Transition = when (action) {
        GameAction.Start -> start(state)
        is GameAction.SelectAnswer -> selectAnswer(state, action)
        is GameAction.BoardMutationFinished -> finishBoardMutation(state, action)
        is GameAction.TimerExpired -> timerExpired(state, action.timerId)
        GameAction.Pause -> pause(state)
        GameAction.Resume -> resume(state)
        GameAction.Restart -> restart(state)
        GameAction.Abandon -> abandon(state)
    }

    private fun start(state: GameState): Transition {
        if (state.phase != GamePhase.READY) return Transition.unchanged(state)
        val events = mutableListOf<GameEvent>()
        var next = state.copy(phase = GamePhase.AWAITING_ANSWER)
        next = scheduleInitialTimers(next, events)
        return Transition(next, events)
    }

    private fun selectAnswer(state: GameState, action: GameAction.SelectAnswer): Transition {
        if (state.phase != GamePhase.AWAITING_ANSWER) return Transition.unchanged(state)
        if (action.questionGeneration != null && action.questionGeneration != state.questionGeneration) {
            return Transition.unchanged(state)
        }

        return if (action.value == state.question.answer) correctAnswer(state) else incorrectAnswer(state)
    }

    private fun correctAnswer(state: GameState): Transition {
        val hiddenPieces = state.revealedPieces.hiddenIndices(state.pieceCount)
        if (hiddenPieces.isEmpty()) {
            return completePuzzle(state, remainingSessionMillis = 0)
        }

        val piece = PieceId(hiddenPieces[random.nextInt(0, hiddenPieces.size)])
        val mutation = PendingBoardMutation(
            id = BoardMutationId(state.sessionGeneration, state.nextMutationSequence),
            pieceId = piece,
            type = BoardMutationType.REVEAL,
        )
        val nextScore = Score(state.score.value + scoringPolicy.pointsFor(state.combo))
        val nextCombo = state.combo + 1
        val events = mutableListOf<GameEvent>()
        var next = state.copy(
                score = nextScore,
                combo = nextCombo,
                phase = GamePhase.MUTATING_BOARD,
                pendingBoardMutation = mutation,
                nextMutationSequence = state.nextMutationSequence + 1,
                statistics = state.statistics.copy(
                    correctAnswers = state.statistics.correctAnswers + 1,
                    maximumCombo = maxOf(state.statistics.maximumCombo, nextCombo),
                ),
            )
        next = cancelTimer(next, GameTimerKind.QUESTION, events)
        next = cancelTimer(next, GameTimerKind.DECAY, events)
        events += GameEvent.CorrectAnswer(piece)
        events += GameEvent.ScoreChanged(nextScore)
        events += GameEvent.ComboChanged(nextCombo)
        events += GameEvent.BoardMutationStarted(mutation)
        val bonusMillis = state.modeRules.clock.correctAnswerBonusMillis
        if (bonusMillis > 0) {
            next.activeTimer(GameTimerKind.SESSION)?.let { timer ->
                events += GameEvent.TimerAdjustmentRequested(
                    timerId = timer.id,
                    deltaMillis = bonusMillis,
                    maximumRemainingMillis = state.modeRules.clock.maximumRemainingMillis,
                )
            }
        }
        return Transition(next, events)
    }

    private fun incorrectAnswer(state: GameState): Transition {
        val events = mutableListOf<GameEvent>()
        events += GameEvent.IncorrectAnswer(state.question.answer)
        if (state.combo != 1) events += GameEvent.ComboChanged(1)
        var next = state.copy(
            combo = 1,
            statistics = state.statistics.copy(
                incorrectAnswers = state.statistics.incorrectAnswers + 1,
            ),
        )
        val heartCost = state.modeRules.mistakes.wrongAnswerHeartCost
        if (heartCost > 0) {
            val hearts = (requireNotNull(state.heartsRemaining) - heartCost).coerceAtLeast(0)
            next = next.copy(heartsRemaining = hearts)
            events += GameEvent.HeartsChanged(hearts)
            if (hearts == 0 && state.modeRules.completion.failWhenQuestionHeartsReachZero) {
                return endMode(next, ModeOutcome.OUT_OF_HEARTS, GameEndReason.HEARTS_DEPLETED, events)
            }
        }

        val penaltyMillis = state.modeRules.clock.wrongAnswerPenaltyMillis
        if (penaltyMillis > 0) {
            next.activeTimer(GameTimerKind.SESSION)?.let { timer ->
                events += GameEvent.TimerAdjustmentRequested(timer.id, -penaltyMillis)
            }
        }

        if (state.modeRules.piecePenalty.removeRevealedPieceOnWrongAnswer) {
            next = cancelTimer(next, GameTimerKind.DECAY, events)
            val removable = next.revealedPieces.asIndices().sorted()
            if (removable.isNotEmpty()) {
                return Transition(scheduleRemoval(next, removable, events), events)
            }
            events += GameEvent.NoPieceAvailableForDecay
        }
        next = restartQuestionTimer(next, events)
        next = restartDecayTimer(next, events)
        return Transition(next, events)
    }

    private fun finishBoardMutation(
        state: GameState,
        action: GameAction.BoardMutationFinished,
    ): Transition {
        val mutation = state.pendingBoardMutation
        if (state.phase != GamePhase.MUTATING_BOARD || mutation == null || mutation.id != action.mutationId) {
            return Transition.unchanged(state)
        }

        val revealed = when (mutation.type) {
            BoardMutationType.REVEAL -> state.revealedPieces.reveal(mutation.pieceId, state.pieceCount)
            BoardMutationType.REMOVE -> state.revealedPieces.remove(mutation.pieceId, state.pieceCount)
        }
        val events = mutableListOf<GameEvent>(GameEvent.BoardMutationCommitted(mutation))
        var statistics = state.statistics
        when (mutation.type) {
            BoardMutationType.REVEAL -> {
                statistics = statistics.copy(piecesRevealed = statistics.piecesRevealed + 1)
                events += GameEvent.PieceRevealed(mutation.pieceId)
            }
            BoardMutationType.REMOVE -> {
                statistics = statistics.copy(piecesRemoved = statistics.piecesRemoved + 1)
                events += GameEvent.PieceRemoved(mutation.pieceId)
            }
        }
        var next = state.copy(
            revealedPieces = revealed,
            pendingBoardMutation = null,
            statistics = statistics,
            phase = GamePhase.AWAITING_ANSWER,
        )
        if (mutation.type == BoardMutationType.REVEAL && revealed.size == state.pieceCount) {
            return completePuzzle(
                next,
                remainingSessionMillis = action.sessionTimerRemainingMillis ?: 0,
                existingEvents = events,
            )
        }
        if (mutation.type == BoardMutationType.REVEAL) {
            next = next.copy(
                question = questionGenerator.generate(state.difficulty, random),
                questionGeneration = state.questionGeneration + 1,
            )
            next = restartQuestionTimer(next, events)
        }
        next = restartDecayTimer(next, events)
        return Transition(next, events)
    }

    private fun timerExpired(state: GameState, timerId: GameTimerId): Transition {
        if (state.phase == GamePhase.PAUSED || state.phase == GamePhase.READY || state.isTerminal()) {
            return Transition.unchanged(state)
        }
        val timer = state.activeTimers.firstOrNull { it.id == timerId }
            ?: return Transition.unchanged(state)
        val events = mutableListOf<GameEvent>(GameEvent.TimerCancelled(timer.id))
        var next = state.copy(activeTimers = state.activeTimers - timer)
        return when (timer.id.kind) {
            GameTimerKind.SESSION -> {
                if (!state.modeRules.completion.failWhenSessionTimerExpires) {
                    Transition(next, events)
                } else {
                    endMode(
                        next,
                        ModeOutcome.TIME_EXPIRED,
                        GameEndReason.SESSION_TIMER_EXPIRED,
                        events,
                    )
                }
            }
            GameTimerKind.MAXIMUM_SESSION -> {
                if (!state.modeRules.completion.failWhenMaximumSessionExpires) {
                    Transition(next, events)
                } else {
                    endMode(
                        next,
                        ModeOutcome.DECAY_LIMIT_REACHED,
                        GameEndReason.MAXIMUM_SESSION_EXPIRED,
                        events,
                    )
                }
            }
            GameTimerKind.QUESTION -> questionTimedOut(next, events)
            GameTimerKind.DECAY -> decayTimedOut(next, events)
        }
    }

    private fun questionTimedOut(
        state: GameState,
        events: MutableList<GameEvent>,
    ): Transition {
        events += GameEvent.QuestionTimedOut(state.questionGeneration)
        var next = state.copy(
            statistics = state.statistics.copy(timeoutCount = state.statistics.timeoutCount + 1),
        )
        val heartCost = state.modeRules.mistakes.questionTimeoutHeartCost
        if (heartCost > 0) {
            val hearts = (requireNotNull(state.heartsRemaining) - heartCost).coerceAtLeast(0)
            next = next.copy(heartsRemaining = hearts)
            events += GameEvent.HeartsChanged(hearts)
            if (hearts == 0 && state.modeRules.completion.failWhenQuestionHeartsReachZero) {
                return endMode(next, ModeOutcome.OUT_OF_HEARTS, GameEndReason.HEARTS_DEPLETED, events)
            }
        }
        next = next.copy(
            question = questionGenerator.generate(state.difficulty, random),
            questionGeneration = state.questionGeneration + 1,
        )
        next = restartQuestionTimer(next, events)
        return Transition(next, events)
    }

    private fun decayTimedOut(
        state: GameState,
        events: MutableList<GameEvent>,
    ): Transition {
        if (!state.modeRules.piecePenalty.removeRevealedPieceOnDecayTimeout) {
            return Transition(state, events)
        }
        val removable = state.revealedPieces.asIndices().sorted()
        if (removable.isEmpty()) {
            events += GameEvent.NoPieceAvailableForDecay
            return Transition(restartDecayTimer(state, events), events)
        }
        return Transition(scheduleRemoval(state, removable, events), events)
    }

    private fun scheduleRemoval(
        state: GameState,
        removablePieces: List<Int>,
        events: MutableList<GameEvent>,
    ): GameState {
        val piece = PieceId(removablePieces[random.nextInt(0, removablePieces.size)])
        val mutation = PendingBoardMutation(
            id = BoardMutationId(state.sessionGeneration, state.nextMutationSequence),
            pieceId = piece,
            type = BoardMutationType.REMOVE,
        )
        events += GameEvent.BoardMutationStarted(mutation)
        return state.copy(
            phase = GamePhase.MUTATING_BOARD,
            pendingBoardMutation = mutation,
            nextMutationSequence = state.nextMutationSequence + 1,
        )
    }

    private fun pause(state: GameState): Transition {
        if (state.phase != GamePhase.AWAITING_ANSWER && state.phase != GamePhase.MUTATING_BOARD) {
            return Transition.unchanged(state)
        }
        return Transition(
            state.copy(phase = GamePhase.PAUSED, resumePhase = state.phase),
            emptyList(),
        )
    }

    private fun resume(state: GameState): Transition {
        if (state.phase != GamePhase.PAUSED) {
            return Transition.unchanged(state)
        }
        val resumePhase = state.resumePhase ?: return Transition.unchanged(state)
        return Transition(
            state.copy(phase = resumePhase, resumePhase = null),
            emptyList(),
        )
    }

    private fun restart(state: GameState): Transition {
        val events = mutableListOf<GameEvent>()
        state.activeTimers.forEach { events += GameEvent.TimerCancelled(it.id) }
        var next = state.copy(
            revealedPieces = PieceSet.Empty,
            score = Score(0),
            combo = 1,
            question = questionGenerator.generate(state.difficulty, random),
            phase = GamePhase.AWAITING_ANSWER,
            pendingBoardMutation = null,
            resumePhase = null,
            sessionGeneration = state.sessionGeneration + 1,
            nextMutationSequence = 1,
            questionGeneration = 1,
            activeTimers = emptyList(),
            nextTimerGeneration = 1,
            heartsRemaining = state.modeRules.mistakes.startingHearts,
            statistics = GameSessionStatistics(),
            outcome = null,
            endReason = null,
        )
        next = scheduleInitialTimers(next, events)
        return Transition(next, events)
    }

    private fun abandon(state: GameState): Transition {
        if (state.isTerminal() || state.phase == GamePhase.READY) return Transition.unchanged(state)
        return endMode(
            state,
            ModeOutcome.ABANDONED,
            GameEndReason.PLAYER_ABANDONED,
            mutableListOf(),
        )
    }

    private fun completePuzzle(
        state: GameState,
        remainingSessionMillis: Long,
        existingEvents: MutableList<GameEvent> = mutableListOf(),
    ): Transition {
        val pointsPerSecond = state.modeRules.scoreModifier.remainingTimeBonusPointsPerSecond
        val bonusLong = (remainingSessionMillis.coerceAtLeast(0) / 1_000L) * pointsPerSecond
        val bonus = bonusLong.coerceAtMost((Int.MAX_VALUE - state.score.value).toLong()).toInt()
        var next = if (bonus > 0) state.copy(score = Score(state.score.value + bonus)) else state
        if (bonus > 0) existingEvents += GameEvent.ScoreChanged(next.score)
        existingEvents += GameEvent.PuzzleCompleted
        next = cancelAllTimers(next, existingEvents)
        next = next.copy(
            phase = GamePhase.COMPLETED,
            pendingBoardMutation = null,
            outcome = ModeOutcome.COMPLETED,
            endReason = GameEndReason.PUZZLE_COMPLETED,
        )
        existingEvents += GameEvent.ModeEnded(ModeOutcome.COMPLETED, GameEndReason.PUZZLE_COMPLETED)
        return Transition(next, existingEvents)
    }

    private fun endMode(
        state: GameState,
        outcome: ModeOutcome,
        reason: GameEndReason,
        events: MutableList<GameEvent>,
    ): Transition {
        val next = cancelAllTimers(state, events).copy(
            phase = GamePhase.ENDED,
            pendingBoardMutation = null,
            resumePhase = null,
            outcome = outcome,
            endReason = reason,
        )
        events += GameEvent.ModeEnded(outcome, reason)
        return Transition(next, events)
    }

    private fun scheduleInitialTimers(
        state: GameState,
        events: MutableList<GameEvent>,
    ): GameState {
        var next = state
        state.modeRules.clock.initialSessionMillis?.let {
            next = startTimer(next, GameTimerKind.SESSION, it, events)
        }
        state.modeRules.clock.maximumSessionMillis?.let {
            next = startTimer(next, GameTimerKind.MAXIMUM_SESSION, it, events)
        }
        next = restartQuestionTimer(next, events)
        next = restartDecayTimer(next, events)
        return next
    }

    private fun restartQuestionTimer(
        state: GameState,
        events: MutableList<GameEvent>,
    ): GameState {
        val duration = state.modeRules.clock.questionDeadlineMillis?.forDifficulty(state.difficulty)
            ?: return state
        return startTimer(state, GameTimerKind.QUESTION, duration, events)
    }

    private fun restartDecayTimer(
        state: GameState,
        events: MutableList<GameEvent>,
    ): GameState {
        val duration = state.modeRules.clock.decayIntervalMillis ?: return state
        return startTimer(state, GameTimerKind.DECAY, duration, events)
    }

    private fun startTimer(
        state: GameState,
        kind: GameTimerKind,
        durationMillis: Long,
        events: MutableList<GameEvent>,
    ): GameState {
        var next = cancelTimer(state, kind, events)
        val timer = ActiveGameTimer(
            id = GameTimerId(
                sessionGeneration = next.sessionGeneration,
                kind = kind,
                generation = next.nextTimerGeneration,
            ),
            durationMillis = durationMillis,
        )
        next = next.copy(
            activeTimers = next.activeTimers + timer,
            nextTimerGeneration = next.nextTimerGeneration + 1,
        )
        events += GameEvent.TimerScheduled(timer)
        return next
    }

    private fun cancelTimer(
        state: GameState,
        kind: GameTimerKind,
        events: MutableList<GameEvent>,
    ): GameState {
        val timer = state.activeTimer(kind) ?: return state
        events += GameEvent.TimerCancelled(timer.id)
        return state.copy(activeTimers = state.activeTimers - timer)
    }

    private fun cancelAllTimers(
        state: GameState,
        events: MutableList<GameEvent>,
    ): GameState {
        state.activeTimers.forEach { events += GameEvent.TimerCancelled(it.id) }
        return state.copy(activeTimers = emptyList())
    }

    private fun GameState.activeTimer(kind: GameTimerKind): ActiveGameTimer? =
        activeTimers.firstOrNull { it.id.kind == kind }

    private fun GameState.isTerminal(): Boolean =
        phase == GamePhase.COMPLETED || phase == GamePhase.ENDED
}
