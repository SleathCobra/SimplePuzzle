package com.qtpie.simplepuzzle.viewmodel

import com.qtpie.simplepuzzle.core.game.DefaultGameEngine
import com.qtpie.simplepuzzle.core.game.QuestionGenerator
import com.qtpie.simplepuzzle.core.game.RandomSource
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion
import com.qtpie.simplepuzzle.model.PuzzleInfo
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
        assertEquals(0, revealingState.revealingPiece)

        viewModel.onRevealAnimationFinished(0)

        val revealedState = viewModel.uiState.value
        assertEquals(1, revealedState.unlockedPieces.size)
        assertEquals(null, revealedState.revealingPiece)
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
        viewModel.onRevealAnimationFinished(0)
        val completedCount = viewModel.userProfile.value.puzzlesCompleted
        val completedCoins = viewModel.userProfile.value.totalCoins
        viewModel.onRevealAnimationFinished(0)
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
        assertEquals(null, viewModel.uiState.value.revealingPiece)

        viewModel.resumeGame()
        viewModel.onAnswerSelected(answer)

        assertEquals(10, viewModel.uiState.value.score)
        assertEquals(0, viewModel.uiState.value.revealingPiece)
        viewModel.resetProgress()
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
    )

    private fun puzzle(pieceCount: Int) = PuzzleInfo(
        id = 99,
        name = "Test Puzzle",
        imageResId = 0,
        totalPieces = pieceCount,
    )

    private data object FirstValueRandomSource : RandomSource {
        override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int = fromInclusive
    }
}
