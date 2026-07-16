package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.GameAction
import com.qtpie.simplepuzzle.core.model.GameConfiguration
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GamePhase
import com.qtpie.simplepuzzle.core.model.GameModeCatalog
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion
import com.qtpie.simplepuzzle.core.model.PieceId
import com.qtpie.simplepuzzle.core.model.PuzzleId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultGameEngineTest {
    private val questions = listOf(
        question(2, MathOperation.ADD, 3, listOf(4, 5, 6, 7)),
        question(9, MathOperation.SUBTRACT, 4, listOf(3, 4, 5, 6)),
        question(7, MathOperation.ADD, 8, listOf(13, 14, 15, 16)),
    )

    @Test
    fun classicRemainsTheDefaultUntimedMode() {
        val engine = engine()
        val state = startedState(engine, pieceCount = 3)

        assertEquals(GameModeId.CLASSIC, state.modeId)
        assertEquals(GameModeCatalog.Classic, state.modeRules)
        assertTrue(state.modeRules.clock.isUntimed)
    }

    @Test
    fun correctAnswerScoresThenWaitsForRevealCompletion() {
        val engine = engine()
        val started = startedState(engine, pieceCount = 3)

        val answered = engine.reduce(started, GameAction.SelectAnswer(started.question.answer))

        assertEquals(GamePhase.MUTATING_BOARD, answered.state.phase)
        assertEquals(10, answered.state.score.value)
        assertEquals(2, answered.state.combo)
        assertEquals(0, answered.state.revealedPieces.size)
        assertEquals(PieceId(0), answered.state.pendingBoardMutation?.pieceId)
        assertEquals(
            listOf(
                GameEvent.CorrectAnswer(PieceId(0)),
                GameEvent.ScoreChanged(com.qtpie.simplepuzzle.core.model.Score(10)),
                GameEvent.ComboChanged(2),
                GameEvent.BoardMutationStarted(requireNotNull(answered.state.pendingBoardMutation)),
            ),
            answered.events,
        )

        val revealed = finishMutation(engine, answered.state)

        assertEquals(GamePhase.AWAITING_ANSWER, revealed.state.phase)
        assertEquals(1, revealed.state.revealedPieces.size)
        assertNull(revealed.state.pendingBoardMutation)
        assertEquals(
            listOf(
                GameEvent.BoardMutationCommitted(requireNotNull(answered.state.pendingBoardMutation)),
                GameEvent.PieceRevealed(PieceId(0)),
            ),
            revealed.events,
        )
    }

    @Test
    fun wrongAnswerKeepsQuestionAndResetsCombo() {
        val engine = engine()
        var state = startedState(engine, pieceCount = 3)
        state = answerCorrectlyAndFinish(engine, state)
        assertEquals(2, state.combo)

        val wrong = engine.reduce(state, GameAction.SelectAnswer(Int.MIN_VALUE))

        assertEquals(state.question, wrong.state.question)
        assertEquals(1, wrong.state.combo)
        assertEquals(state.score, wrong.state.score)
        assertEquals(
            listOf(GameEvent.IncorrectAnswer(state.question.answer), GameEvent.ComboChanged(1)),
            wrong.events,
        )
    }

    @Test
    fun comboIncreasesScoring() {
        val engine = engine()
        var state = startedState(engine, pieceCount = 4)

        state = answerCorrectlyAndFinish(engine, state)
        state = answerCorrectlyAndFinish(engine, state)

        assertEquals(30, state.score.value)
        assertEquals(3, state.combo)
        assertEquals(2, state.revealedPieces.size)
    }

    @Test
    fun finalPieceCompletesExactlyOnce() {
        val engine = engine()
        var state = startedState(engine, pieceCount = 2)
        state = answerCorrectlyAndFinish(engine, state)

        val answer = engine.reduce(state, GameAction.SelectAnswer(state.question.answer))
        val finish = finishMutation(engine, answer.state)

        assertEquals(GamePhase.COMPLETED, finish.state.phase)
        assertEquals(2, finish.state.revealedPieces.size)
        assertEquals(1, finish.events.count { it == GameEvent.PuzzleCompleted })

        val duplicateFinish = engine.reduce(
            finish.state,
            GameAction.BoardMutationFinished(requireNotNull(answer.state.pendingBoardMutation).id),
        )
        val duplicateAnswer = engine.reduce(finish.state, GameAction.SelectAnswer(finish.state.question.answer))
        assertTrue(duplicateFinish.events.isEmpty())
        assertTrue(duplicateAnswer.events.isEmpty())
        assertEquals(finish.state, duplicateFinish.state)
        assertEquals(finish.state, duplicateAnswer.state)
    }

    @Test
    fun answerCannotBeSubmittedTwiceDuringReveal() {
        val engine = engine()
        val started = startedState(engine, pieceCount = 3)
        val first = engine.reduce(started, GameAction.SelectAnswer(started.question.answer))

        val duplicate = engine.reduce(first.state, GameAction.SelectAnswer(started.question.answer))

        assertEquals(first.state, duplicate.state)
        assertTrue(duplicate.events.isEmpty())
    }

    @Test
    fun pauseAndResumePreserveAwaitingAndRevealingPhases() {
        val engine = engine()
        val started = startedState(engine, pieceCount = 3)

        val pausedAwaiting = engine.reduce(started, GameAction.Pause).state
        assertEquals(GamePhase.PAUSED, pausedAwaiting.phase)
        assertEquals(GamePhase.AWAITING_ANSWER, pausedAwaiting.resumePhase)
        assertEquals(started, engine.reduce(pausedAwaiting, GameAction.Resume).state)

        val revealing = engine.reduce(started, GameAction.SelectAnswer(started.question.answer)).state
        val pausedRevealing = engine.reduce(revealing, GameAction.Pause).state
        assertEquals(GamePhase.MUTATING_BOARD, pausedRevealing.resumePhase)
        assertEquals(revealing, engine.reduce(pausedRevealing, GameAction.Resume).state)
    }

    @Test
    fun restartClearsProgressAndCreatesFreshQuestion() {
        val engine = engine()
        var state = startedState(engine, pieceCount = 3)
        state = answerCorrectlyAndFinish(engine, state)

        val restarted = engine.reduce(state, GameAction.Restart)

        assertEquals(GamePhase.AWAITING_ANSWER, restarted.state.phase)
        assertEquals(0, restarted.state.score.value)
        assertEquals(1, restarted.state.combo)
        assertEquals(0, restarted.state.revealedPieces.size)
        assertFalse(restarted.state.question == state.question)
    }

    @Test
    fun progressionDoesNotAssumeThirtyOrSixtyFourPieces() {
        val engine = engine()
        var state = startedState(engine, pieceCount = 70)

        repeat(70) {
            state = answerCorrectlyAndFinish(engine, state)
        }

        assertEquals(GamePhase.COMPLETED, state.phase)
        assertEquals(70, state.revealedPieces.size)
    }

    private fun engine(): GameEngine = DefaultGameEngine(
        random = FirstValueRandomSource,
        questionGenerator = CyclingQuestionGenerator(questions),
    )

    private fun startedState(engine: GameEngine, pieceCount: Int) = engine.reduce(
        engine.newGame(
            GameConfiguration(
                puzzleId = PuzzleId("test-puzzle"),
                pieceCount = pieceCount,
                difficulty = Difficulty.MEDIUM,
            ),
        ),
        GameAction.Start,
    ).state

    private fun answerCorrectlyAndFinish(engine: GameEngine, state: com.qtpie.simplepuzzle.core.model.GameState): com.qtpie.simplepuzzle.core.model.GameState {
        val answered = engine.reduce(state, GameAction.SelectAnswer(state.question.answer))
        return finishMutation(engine, answered.state).state
    }

    private fun finishMutation(
        engine: GameEngine,
        state: com.qtpie.simplepuzzle.core.model.GameState,
    ) = engine.reduce(
        state,
        GameAction.BoardMutationFinished(requireNotNull(state.pendingBoardMutation).id),
    )

    private fun question(
        left: Int,
        operation: MathOperation,
        right: Int,
        options: List<Int>,
    ) = MathQuestion(
        leftOperand = left,
        operation = operation,
        rightOperand = right,
        answer = operation.evaluate(left, right),
        options = options,
    )

    private class CyclingQuestionGenerator(
        private val questions: List<MathQuestion>,
    ) : QuestionGenerator {
        private var index = 0

        override fun generate(difficulty: Difficulty, random: RandomSource): MathQuestion =
            questions[index++ % questions.size]
    }

    private data object FirstValueRandomSource : RandomSource {
        override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int = fromInclusive
    }
}
