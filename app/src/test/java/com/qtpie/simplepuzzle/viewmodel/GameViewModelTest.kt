package com.qtpie.simplepuzzle.viewmodel

import com.qtpie.simplepuzzle.core.game.DefaultGameEngine
import com.qtpie.simplepuzzle.core.game.QuestionGenerator
import com.qtpie.simplepuzzle.core.game.RandomSource
import com.qtpie.simplepuzzle.core.game.DefaultMathQuestionGenerator
import com.qtpie.simplepuzzle.core.game.JigsawLearningAttemptFactory
import com.qtpie.simplepuzzle.core.data.learning.AttemptRecordingResult
import com.qtpie.simplepuzzle.core.data.learning.LearningRepository
import com.qtpie.simplepuzzle.core.data.progress.ProgressRepository
import com.qtpie.simplepuzzle.core.learning.AttemptId
import com.qtpie.simplepuzzle.core.learning.LearningAttempt
import com.qtpie.simplepuzzle.core.learning.LearningClock
import com.qtpie.simplepuzzle.core.learning.LearningIdSource
import com.qtpie.simplepuzzle.core.learning.PersonalSkillSummary
import com.qtpie.simplepuzzle.core.learning.SessionId
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.PuzzleProgress
import com.qtpie.simplepuzzle.core.model.Score
import com.qtpie.simplepuzzle.model.PuzzleInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

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

    @Test
    fun correctAnswerRecordsExactlyOneAttemptAndRevealDoesNotRecordAgain() {
        val repository = FakeLearningRepository()
        val viewModel = learningViewModel(repository)
        viewModel.selectPuzzle(puzzle(pieceCount = 3))

        viewModel.onAnswerSelected(viewModel.uiState.value.currentQuestion.answer)
        assertEquals(1, repository.recorded.size)
        assertEquals(1, repository.recorded.single().attemptOrdinal)

        viewModel.onRevealAnimationFinished(0)
        viewModel.onRevealAnimationFinished(0)

        assertEquals(1, repository.recorded.size)
        assertEquals(10, viewModel.uiState.value.score)
        assertEquals(1, viewModel.uiState.value.unlockedPieces.size)
    }

    @Test
    fun incorrectAnswerRecordsExactlyOneAttemptWithNoMisconceptionInference() {
        val repository = FakeLearningRepository()
        val viewModel = learningViewModel(repository)
        viewModel.selectPuzzle(puzzle(pieceCount = 3))

        viewModel.onAnswerSelected(Int.MIN_VALUE)

        assertEquals(1, repository.recorded.size)
        assertEquals(com.qtpie.simplepuzzle.core.learning.AttemptOutcome.INCORRECT, repository.recorded.single().outcome)
        assertEquals(0, viewModel.uiState.value.score)
    }

    @Test
    fun lifecycleRecreationSignalsDoNotDuplicateAttempt() {
        val repository = FakeLearningRepository()
        val viewModel = learningViewModel(repository)
        viewModel.selectPuzzle(puzzle(pieceCount = 3))
        viewModel.onAnswerSelected(viewModel.uiState.value.currentQuestion.answer)

        viewModel.pauseGame()
        viewModel.resumeGame()

        assertEquals(1, repository.recorded.size)
    }

    @Test
    fun delayedLearningPersistenceDoesNotBlockGameplayTransition() {
        val gate = CompletableDeferred<Unit>()
        val repository = FakeLearningRepository(gate)
        val viewModel = learningViewModel(repository)
        viewModel.selectPuzzle(puzzle(pieceCount = 3))

        viewModel.onAnswerSelected(viewModel.uiState.value.currentQuestion.answer)

        assertEquals(10, viewModel.uiState.value.score)
        assertEquals(0, viewModel.uiState.value.revealingPiece)
        assertTrue(repository.recorded.isEmpty())

        gate.complete(Unit)
        assertEquals(1, repository.recorded.size)
    }

    @Test
    fun resetWaitsForAcceptedLearningAttemptBeforeClearingAllLocalData() {
        val gate = CompletableDeferred<Unit>()
        val operations = mutableListOf<String>()
        val learningRepository = FakeLearningRepository(gate, operations)
        val progressRepository = FakeProgressRepository(operations)
        val viewModel = learningViewModel(learningRepository, progressRepository)
        viewModel.selectPuzzle(puzzle(pieceCount = 3))

        viewModel.onAnswerSelected(viewModel.uiState.value.currentQuestion.answer)
        viewModel.resetProgress()

        assertFalse("Reset must wait for the accepted attempt", progressRepository.wasReset)

        gate.complete(Unit)

        assertEquals(listOf("attempt", "reset"), operations.takeLast(2))
        assertTrue(progressRepository.wasReset)
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

    private fun learningViewModel(
        repository: LearningRepository,
        progressRepository: ProgressRepository? = null,
    ): GameViewModel {
        val generated = DefaultMathQuestionGenerator().generateFromSeed(
            com.qtpie.simplepuzzle.core.model.Difficulty.MEDIUM,
            44L,
        )
        return GameViewModel(
            gameEngine = DefaultGameEngine(
                random = FirstValueRandomSource,
                questionGenerator = QuestionGenerator { _, _ -> generated },
            ),
            progressRepository = progressRepository,
            learningRepository = repository,
            learningAttemptFactory = JigsawLearningAttemptFactory(
                clock = LearningClock { 5_000L },
                idSource = SequenceLearningIds(),
                applicationVersion = "test",
            ),
        )
    }

    private fun puzzle(pieceCount: Int) = PuzzleInfo(
        id = 99,
        name = "Test Puzzle",
        imageResId = 0,
        totalPieces = pieceCount,
    )

    private data object FirstValueRandomSource : RandomSource {
        override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int = fromInclusive
    }

    private class FakeLearningRepository(
        private val gate: CompletableDeferred<Unit>? = null,
        private val operations: MutableList<String>? = null,
    ) : LearningRepository {
        private val attemptFlow = MutableStateFlow<List<LearningAttempt>>(emptyList())
        override val attempts: Flow<List<LearningAttempt>> = attemptFlow
        override val summaries: Flow<List<PersonalSkillSummary>> = MutableStateFlow(emptyList())
        val recorded = mutableListOf<LearningAttempt>()

        override suspend fun recordAttempt(attempt: LearningAttempt): AttemptRecordingResult {
            gate?.await()
            recorded += attempt
            operations?.add("attempt")
            attemptFlow.value = recorded.toList()
            return AttemptRecordingResult.RECORDED
        }
    }

    private class FakeProgressRepository(
        private val operations: MutableList<String>,
    ) : ProgressRepository {
        override val progress: Flow<List<PuzzleProgress>> = MutableStateFlow(emptyList())
        var wasReset = false

        override suspend fun startAttempt(puzzleId: PuzzleId, totalPieces: Int) = Unit

        override suspend fun saveProgress(
            puzzleId: PuzzleId,
            revealedPieces: Int,
            totalPieces: Int,
            score: Score,
        ) = Unit

        override suspend fun completePuzzle(summary: GameSessionSummary) = Unit

        override suspend fun restore(progress: PuzzleProgress) = Unit

        override suspend fun resetAll() {
            operations += "reset"
            wasReset = true
        }
    }

    private class SequenceLearningIds : LearningIdSource {
        private var attempts = 0
        private var sessions = 0

        override fun nextAttemptId() = AttemptId("attempt-viewmodel-${++attempts}")

        override fun nextSessionId() = SessionId("session-viewmodel-${++sessions}")
    }
}
