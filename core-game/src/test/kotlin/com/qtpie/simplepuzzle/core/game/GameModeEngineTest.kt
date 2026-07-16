package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.BoardMutationType
import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.GameAction
import com.qtpie.simplepuzzle.core.model.GameConfiguration
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GameModeCatalog
import com.qtpie.simplepuzzle.core.model.GameModeDefinition
import com.qtpie.simplepuzzle.core.model.GamePhase
import com.qtpie.simplepuzzle.core.model.GameState
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion
import com.qtpie.simplepuzzle.core.model.ModeOutcome
import com.qtpie.simplepuzzle.core.model.PuzzleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeEngineTest {
    @Test
    fun timeAttackCompletesBeforeTimeoutAndAwardsDocumentedTimeBonus() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.TimeAttack, pieceCount = 1)
        val answered = answerCorrectly(engine, started)

        val completed = finishMutation(engine, answered, sessionRemainingMillis = 30_000)

        assertEquals(GamePhase.COMPLETED, completed.phase)
        assertEquals(ModeOutcome.COMPLETED, completed.outcome)
        assertEquals(70, completed.score.value) // 10 answer points + 30 seconds * 2.
        assertTrue(completed.activeTimers.isEmpty())
    }

    @Test
    fun timeAttackTimeoutWinsRaceAgainstUncommittedFinalReveal() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.TimeAttack, pieceCount = 1)
        val timerId = timerId(started, GameTimerKind.SESSION)
        val answered = answerCorrectly(engine, started)
        val pendingId = requireNotNull(answered.pendingBoardMutation).id

        val timedOut = engine.reduce(answered, GameAction.TimerExpired(timerId)).state
        val lateRendererAck = engine.reduce(timedOut, GameAction.BoardMutationFinished(pendingId))

        assertEquals(GamePhase.ENDED, timedOut.phase)
        assertEquals(ModeOutcome.TIME_EXPIRED, timedOut.outcome)
        assertEquals(0, timedOut.revealedPieces.size)
        assertEquals(timedOut, lateRendererAck.state)
        assertTrue(lateRendererAck.events.isEmpty())
    }

    @Test
    fun survivalWrongAndQuestionTimeoutEachCostOneHeart() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.Survival, pieceCount = 3)

        val wrong = engine.reduce(
            started,
            GameAction.SelectAnswer(Int.MIN_VALUE, started.questionGeneration),
        )
        val questionTimer = timerId(wrong.state, GameTimerKind.QUESTION)
        val timedOut = engine.reduce(wrong.state, GameAction.TimerExpired(questionTimer))

        assertEquals(2, wrong.state.heartsRemaining)
        assertTrue(wrong.events.contains(GameEvent.HeartsChanged(2)))
        assertEquals(1, timedOut.state.heartsRemaining)
        assertEquals(1, timedOut.state.statistics.timeoutCount)
    }

    @Test
    fun survivalFinalHeartEndsWithChildFriendlyOutcome() {
        val engine = engine()
        var state = started(engine, GameModeCatalog.Survival, pieceCount = 3)

        repeat(3) {
            state = engine.reduce(
                state,
                GameAction.SelectAnswer(Int.MIN_VALUE, state.questionGeneration),
            ).state
        }

        assertEquals(GamePhase.ENDED, state.phase)
        assertEquals(ModeOutcome.OUT_OF_HEARTS, state.outcome)
        assertEquals(0, state.heartsRemaining)
    }

    @Test
    fun staleQuestionTimeoutAndAnswerTimeoutRaceAreDeterministic() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.Survival, pieceCount = 3)
        val oldQuestion = started.questionGeneration
        val oldTimer = timerId(started, GameTimerKind.QUESTION)

        val answerFirst = answerCorrectly(engine, started)
        val staleTimeout = engine.reduce(answerFirst, GameAction.TimerExpired(oldTimer))
        assertEquals(answerFirst, staleTimeout.state)

        val timeoutFirst = engine.reduce(started, GameAction.TimerExpired(oldTimer)).state
        val lateAnswer = engine.reduce(
            timeoutFirst,
            GameAction.SelectAnswer(timeoutFirst.question.answer, oldQuestion),
        )
        assertEquals(timeoutFirst, lateAnswer.state)
        assertTrue(lateAnswer.events.isEmpty())
    }

    @Test
    fun puzzleDecayWithNoVisiblePieceRestartsCountdownWithoutMutation() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.PuzzleDecay, pieceCount = 3)
        val expiredTimer = timerId(started, GameTimerKind.DECAY)

        val transition = engine.reduce(started, GameAction.TimerExpired(expiredTimer))

        assertEquals(GamePhase.AWAITING_ANSWER, transition.state.phase)
        assertNull(transition.state.pendingBoardMutation)
        assertTrue(transition.events.contains(GameEvent.NoPieceAvailableForDecay))
        assertNotEquals(expiredTimer, timerId(transition.state, GameTimerKind.DECAY))
    }

    @Test
    fun puzzleDecaySelectsDeterministicallyAndCommitsRemovalOnlyAfterExactAck() {
        val engine = engine()
        var state = started(engine, GameModeCatalog.PuzzleDecay, pieceCount = 3)
        state = answerAndFinish(engine, state)
        state = answerAndFinish(engine, state)
        val decayTimer = timerId(state, GameTimerKind.DECAY)

        val scheduled = engine.reduce(state, GameAction.TimerExpired(decayTimer)).state
        val mutation = requireNotNull(scheduled.pendingBoardMutation)
        assertEquals(BoardMutationType.REMOVE, mutation.type)
        assertEquals(0, mutation.pieceId.value)
        assertEquals(2, scheduled.revealedPieces.size)

        val stale = engine.reduce(
            scheduled,
            GameAction.BoardMutationFinished(mutation.id.copy(sequence = mutation.id.sequence + 1)),
        )
        assertEquals(scheduled, stale.state)

        val committed = finishMutation(engine, scheduled)
        assertEquals(1, committed.revealedPieces.size)
        assertEquals(1, committed.statistics.piecesRemoved)

        val duplicate = engine.reduce(committed, GameAction.BoardMutationFinished(mutation.id))
        assertEquals(committed, duplicate.state)
        assertTrue(duplicate.events.isEmpty())
    }

    @Test
    fun puzzleDecayWrongAnswerSchedulesRemovalAndPendingOperationsCannotConflict() {
        val engine = engine()
        var state = started(engine, GameModeCatalog.PuzzleDecay, pieceCount = 3)
        state = answerAndFinish(engine, state)

        val wrong = engine.reduce(
            state,
            GameAction.SelectAnswer(Int.MIN_VALUE, state.questionGeneration),
        ).state
        assertEquals(BoardMutationType.REMOVE, wrong.pendingBoardMutation?.type)

        val answerDuringRemoval = engine.reduce(
            wrong,
            GameAction.SelectAnswer(wrong.question.answer, wrong.questionGeneration),
        )
        assertEquals(wrong, answerDuringRemoval.state)

        val oldDecayTimer = state.activeTimers.single { it.id.kind == GameTimerKind.DECAY }.id
        val decayDuringPendingRevealState = answerCorrectly(engine, state)
        val decayDuringReveal = engine.reduce(
            decayDuringPendingRevealState,
            GameAction.TimerExpired(oldDecayTimer),
        )
        assertEquals(decayDuringPendingRevealState, decayDuringReveal.state)
    }

    @Test
    fun puzzleCanCompleteAfterPreviouslyCommittedRemovals() {
        val engine = engine()
        var state = started(engine, GameModeCatalog.PuzzleDecay, pieceCount = 2)
        state = answerAndFinish(engine, state)
        state = answerAndFinish(engine, state)
        assertEquals(GamePhase.COMPLETED, state.phase)

        // Build the same deterministic path but decay before the final replacement reveal.
        state = started(engine, GameModeCatalog.PuzzleDecay, pieceCount = 2)
        state = answerAndFinish(engine, state)
        val removal = engine.reduce(
            state,
            GameAction.TimerExpired(timerId(state, GameTimerKind.DECAY)),
        ).state
        state = finishMutation(engine, removal)
        state = answerAndFinish(engine, state)
        state = answerAndFinish(engine, state)

        assertEquals(GamePhase.COMPLETED, state.phase)
        assertEquals(2, state.revealedPieces.size)
        assertEquals(1, state.statistics.piecesRemoved)
        assertEquals(3, state.statistics.piecesRevealed)
    }

    @Test
    fun comboRushRequestsCappedBonusAndWrongAnswerPenaltyThenCanTimeout() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.ComboRush, pieceCount = 3)

        val correct = engine.reduce(
            started,
            GameAction.SelectAnswer(started.question.answer, started.questionGeneration),
        )
        val bonus = correct.events.filterIsInstance<GameEvent.TimerAdjustmentRequested>().single()
        assertEquals(2_500L, bonus.deltaMillis)
        assertEquals(60_000L, bonus.maximumRemainingMillis)

        val afterReveal = finishMutation(engine, correct.state)
        val wrong = engine.reduce(
            afterReveal,
            GameAction.SelectAnswer(Int.MIN_VALUE, afterReveal.questionGeneration),
        )
        val penalty = wrong.events.filterIsInstance<GameEvent.TimerAdjustmentRequested>().single()
        assertEquals(-4_000L, penalty.deltaMillis)
        assertEquals(1, wrong.state.combo)

        val timedOut = engine.reduce(
            wrong.state,
            GameAction.TimerExpired(timerId(wrong.state, GameTimerKind.SESSION)),
        ).state
        assertEquals(ModeOutcome.TIME_EXPIRED, timedOut.outcome)
    }

    @Test
    fun pauseResumePreservesTimersAndRestartCreatesFreshGenerations() {
        val engine = engine()
        val started = started(engine, GameModeCatalog.Survival, pieceCount = 3)
        val originalTimer = started.activeTimers.single()

        val paused = engine.reduce(started, GameAction.Pause).state
        val resumed = engine.reduce(paused, GameAction.Resume).state
        val restarted = engine.reduce(resumed, GameAction.Restart).state

        assertEquals(listOf(originalTimer), paused.activeTimers)
        assertEquals(listOf(originalTimer), resumed.activeTimers)
        assertTrue(restarted.activeTimers.all { it.id.sessionGeneration == 2L })
        assertNotEquals(originalTimer.id, restarted.activeTimers.single().id)
        assertEquals(3, restarted.heartsRemaining)
    }

    @Test
    fun sameSeedReproducesModePieceSequence() {
        fun sequence(): List<Int> {
            val engine = DefaultGameEngine(
                random = SeededRandomSource(77L),
                questionGenerator = FixedQuestionGenerator,
            )
            var state = started(engine, GameModeCatalog.Classic, pieceCount = 8)
            return List(5) {
                val answered = answerCorrectly(engine, state)
                val piece = requireNotNull(answered.pendingBoardMutation).pieceId.value
                state = finishMutation(engine, answered)
                piece
            }
        }

        assertEquals(sequence(), sequence())
    }

    private fun engine(): GameEngine = DefaultGameEngine(
        random = FirstValueRandomSource,
        questionGenerator = FixedQuestionGenerator,
    )

    private fun started(
        engine: GameEngine,
        mode: GameModeDefinition,
        pieceCount: Int,
        difficulty: Difficulty = Difficulty.MEDIUM,
    ): GameState = engine.reduce(
        engine.newGame(
            GameConfiguration(
                puzzleId = PuzzleId("mode-test"),
                pieceCount = pieceCount,
                difficulty = difficulty,
                modeId = mode.id,
                modeRules = mode,
            ),
        ),
        GameAction.Start,
    ).state

    private fun answerCorrectly(engine: GameEngine, state: GameState): GameState = engine.reduce(
        state,
        GameAction.SelectAnswer(state.question.answer, state.questionGeneration),
    ).state

    private fun answerAndFinish(engine: GameEngine, state: GameState): GameState =
        finishMutation(engine, answerCorrectly(engine, state))

    private fun finishMutation(
        engine: GameEngine,
        state: GameState,
        sessionRemainingMillis: Long? = null,
    ): GameState = engine.reduce(
        state,
        GameAction.BoardMutationFinished(
            mutationId = requireNotNull(state.pendingBoardMutation).id,
            sessionTimerRemainingMillis = sessionRemainingMillis,
        ),
    ).state

    private fun timerId(state: GameState, kind: GameTimerKind) =
        state.activeTimers.single { it.id.kind == kind }.id

    private data object FixedQuestionGenerator : QuestionGenerator {
        override fun generate(difficulty: Difficulty, random: RandomSource) = MathQuestion(
            leftOperand = 4,
            operation = MathOperation.ADD,
            rightOperand = 5,
            answer = 9,
            options = listOf(7, 8, 9, 10),
        )
    }

    private data object FirstValueRandomSource : RandomSource {
        override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int = fromInclusive
    }
}
