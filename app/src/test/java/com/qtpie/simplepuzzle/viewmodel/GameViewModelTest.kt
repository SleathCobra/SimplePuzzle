package com.qtpie.simplepuzzle.viewmodel

import com.qtpie.simplepuzzle.core.game.DefaultGameEngine
import com.qtpie.simplepuzzle.core.game.QuestionGenerator
import com.qtpie.simplepuzzle.core.game.RandomSource
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import com.qtpie.simplepuzzle.core.model.ModeOutcome
import com.qtpie.simplepuzzle.model.PuzzleInfo
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GameViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun correctAnswerMapsReducerStateIntoOneUiSnapshot() {
        val viewModel = viewModel()
        viewModel.selectPuzzle(puzzle(pieceCount = 3))

        val answer = viewModel.uiState.value.currentQuestion.answer
        viewModel.onAnswerSelected(answer)

        val revealingState = viewModel.uiState.value
        assertEquals(10, revealingState.score)
        assertEquals(2, revealingState.combo)
        assertEquals(0, revealingState.unlockedPieces.size)
        assertEquals(0, revealingState.pendingBoardMutation?.pieceId?.value)

        viewModel.onBoardMutationFinished(requireNotNull(revealingState.pendingBoardMutation).id)

        val revealedState = viewModel.uiState.value
        assertEquals(1, revealedState.unlockedPieces.size)
        assertEquals(null, revealedState.pendingBoardMutation)
        assertEquals(5, revealedState.coins)
        assertFalse(revealedState.isGameOver)
        viewModel.resetProgress()
    }

    @Test
    fun wrongAnswerDoesNotAdvancePuzzleAndTriggersFeedbackState() {
        val viewModel = viewModel()
        viewModel.selectPuzzle(puzzle(pieceCount = 3))

        viewModel.onAnswerSelected(Int.MIN_VALUE)

        val state = viewModel.uiState.value
        assertEquals(0, state.score)
        assertEquals(1, state.combo)
        assertTrue(state.unlockedPieces.isEmpty())
        assertEquals(1, state.shakeTrigger)
        viewModel.resetProgress()
    }

    @Test
    fun completionEventIsAppliedOnlyOnce() {
        val viewModel = viewModel()
        viewModel.selectPuzzle(puzzle(pieceCount = 1))
        val answer = viewModel.uiState.value.currentQuestion.answer

        viewModel.onAnswerSelected(answer)
        val mutationId = requireNotNull(viewModel.uiState.value.pendingBoardMutation).id
        viewModel.onBoardMutationFinished(mutationId)
        val completedCount = viewModel.userProfile.value.puzzlesCompleted
        val completedCoins = viewModel.userProfile.value.totalCoins
        viewModel.onBoardMutationFinished(mutationId)
        viewModel.onAnswerSelected(answer)

        assertTrue(viewModel.uiState.value.isGameOver)
        assertEquals(completedCount, viewModel.userProfile.value.puzzlesCompleted)
        assertEquals(completedCoins, viewModel.userProfile.value.totalCoins)
        viewModel.resetProgress()
    }

    @Test
    fun lifecyclePauseBlocksAnswersUntilResume() {
        val viewModel = viewModel()
        viewModel.selectPuzzle(puzzle(pieceCount = 3))
        val answer = viewModel.uiState.value.currentQuestion.answer

        viewModel.pauseGame()
        viewModel.onAnswerSelected(answer)

        assertEquals(0, viewModel.uiState.value.score)
        assertEquals(null, viewModel.uiState.value.pendingBoardMutation)

        viewModel.resumeGame()
        viewModel.onAnswerSelected(answer)

        assertEquals(10, viewModel.uiState.value.score)
        assertEquals(0, viewModel.uiState.value.pendingBoardMutation?.pieceId?.value)
        viewModel.resetProgress()
    }

    @Test
    fun modeSelectionStartsSurvivalWithModeSpecificHudStateAndDeadline() {
        val viewModel = viewModel()

        assertTrue(viewModel.choosePuzzleForMode(puzzle(pieceCount = 3)))
        assertTrue(viewModel.startSelectedMode(GameModeId.SURVIVAL))

        assertEquals(GameModeId.SURVIVAL, viewModel.uiState.value.mode.id)
        assertEquals(3, viewModel.uiState.value.heartsRemaining)
        assertEquals(
            8_000L,
            viewModel.timerUiState.value.timer(GameTimerKind.QUESTION)?.durationMillis,
        )
        viewModel.endSessionForNavigation()
    }

    @Test
    fun heartLossAndTimeBonusAreNonReplayedOneShotEvents() = runTest {
        val survival = viewModel()
        survival.selectPuzzle(puzzle(pieceCount = 3), GameModeId.SURVIVAL)
        val heartEvent = async { survival.gameFeedback.first() }
        yield()
        survival.onAnswerSelected(Int.MIN_VALUE)
        assertEquals(GameFeedbackType.HEART_LOST, heartEvent.await().type)

        val rush = viewModel()
        rush.selectPuzzle(puzzle(pieceCount = 3), GameModeId.COMBO_RUSH)
        val bonusEvent = async { rush.gameFeedback.first() }
        yield()
        rush.onAnswerSelected(rush.uiState.value.currentQuestion.answer)
        val bonus = bonusEvent.await()
        assertEquals(GameFeedbackType.TIME_BONUS, bonus.type)
        assertEquals(2_500L, bonus.amount)
        assertTrue(bonus.id > 0)
        survival.endSessionForNavigation()
        rush.endSessionForNavigation()
    }

    @Test
    fun navigationCancelsTimersAndAnyModeCanCompleteThePuzzle() {
        val viewModel = viewModel()
        viewModel.selectPuzzle(puzzle(pieceCount = 1), GameModeId.TIME_ATTACK)
        viewModel.onAnswerSelected(viewModel.uiState.value.currentQuestion.answer)
        viewModel.onBoardMutationFinished(requireNotNull(viewModel.uiState.value.pendingBoardMutation).id)

        assertEquals(ModeOutcome.COMPLETED, viewModel.uiState.value.outcome)
        assertTrue(viewModel.uiState.value.isGameOver)

        viewModel.selectPuzzle(puzzle(pieceCount = 3), GameModeId.COMBO_RUSH)
        assertTrue(viewModel.timerUiState.value.timers.isNotEmpty())
        viewModel.endSessionForNavigation()
        assertTrue(viewModel.timerUiState.value.timers.isEmpty())
    }

    private fun viewModel() = GameViewModel(
        gameEngine = DefaultGameEngine(
            random = FirstValueRandomSource,
            questionGenerator = QuestionGenerator { _, _ ->
                MathQuestion(
                    leftOperand = 4,
                    operation = MathOperation.ADD,
                    rightOperand = 5,
                    answer = 9,
                    options = listOf(7, 8, 9, 10),
                )
            },
        ),
        gameClock = GameClock { 0L },
        timerScheduler = TimerScheduler { _, _ -> TimerCancellation { } },
    )

    private fun puzzle(pieceCount: Int) = PuzzleInfo(
        id = "test-puzzle",
        name = "Test Puzzle",
        imageResId = 0,
        assetRoot = "puzzles/test-puzzle",
        totalPieces = pieceCount,
    )

    private data object FirstValueRandomSource : RandomSource {
        override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int = fromInclusive
    }
}
