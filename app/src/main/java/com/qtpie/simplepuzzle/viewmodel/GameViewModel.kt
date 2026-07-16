package com.qtpie.simplepuzzle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.core.data.preferences.PreferencesRepository
import com.qtpie.simplepuzzle.core.data.learning.LearningRepository
import com.qtpie.simplepuzzle.core.data.learning.SystemLearningClock
import com.qtpie.simplepuzzle.core.data.progress.ProgressRepository
import com.qtpie.simplepuzzle.core.game.DefaultGameEngine
import com.qtpie.simplepuzzle.core.game.GameEngine
import com.qtpie.simplepuzzle.core.game.JigsawLearningAttemptFactory
import com.qtpie.simplepuzzle.core.game.SeededRandomSource
import com.qtpie.simplepuzzle.core.learning.PersonalSkillSummary
import com.qtpie.simplepuzzle.core.learning.SessionId
import com.qtpie.simplepuzzle.core.model.GameAction
import com.qtpie.simplepuzzle.core.model.GameConfiguration
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GamePhase
import com.qtpie.simplepuzzle.core.model.GameState
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.PlayerPreferences
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.Score
import com.qtpie.simplepuzzle.model.*
import com.qtpie.simplepuzzle.learning.UuidLearningIdSource
import com.qtpie.simplepuzzle.core.model.Difficulty as EngineDifficulty
import com.qtpie.simplepuzzle.core.model.MathQuestion as EngineMathQuestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

enum class SoundEffect {
    MENU_CLICK,
    PUZZLE_LOCKED,
    PUZZLE_UNLOCKED,
    ANSWER_CORRECT,
    ANSWER_WRONG,
    PIECE_PLACED,
    PIECE_REMOVED,
    COINS_ADDED,
    GAME_COMPLETE,
    NAVIGATION,
    SWITCH_ON,
    SWITCH_OFF,
    SWITCH_TOGGLE,
    MANY_FAILS
}

data class GameUiState(
    val currentQuestion: MathQuestion = MathQuestion(problem = "", answer = 0, options = emptyList()),
    val unlockedPieces: Set<Int> = emptySet(),
    val revealingPiece: Int? = null,
    val score: Int = 0,
    val combo: Int = 1,
    val shakeTrigger: Int = 0,
    val currentPuzzle: PuzzleInfo? = null,
    val isGameOver: Boolean = false,
    val coins: Int = 0,
    val timeElapsed: Int = 0,
    val bestTime: Int? = null
)

data class UserProfileState(
    val name: String = "Kate",
    val totalScore: Int = 15420,
    val puzzlesCompleted: Int = 3,
    val totalPuzzles: Int = 12,
    val totalCoins: Int = 150
)

private const val DEFAULT_GAME_SEED = 0x4A49475341574D41L

class GameViewModel(
    private val gameEngine: GameEngine = DefaultGameEngine(
        random = SeededRandomSource(DEFAULT_GAME_SEED),
    ),
    private val preferencesRepository: PreferencesRepository? = null,
    private val progressRepository: ProgressRepository? = null,
    private val learningRepository: LearningRepository? = null,
    private val learningAttemptFactory: JigsawLearningAttemptFactory? = null,
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    private val _learningSummaries = MutableStateFlow<List<PersonalSkillSummary>>(emptyList())
    val learningSummaries: StateFlow<List<PersonalSkillSummary>> = _learningSummaries.asStateFlow()

    private val _soundEvent = MutableSharedFlow<SoundEffect>()
    val soundEvent: SharedFlow<SoundEffect> = _soundEvent.asSharedFlow()

    private var wrongAnswersInARow = 0
    private var engineState: GameState? = null
    private var persistenceJob: Job? = null
    private var learningPersistenceJob: Job? = null
    private var correctAnswers = 0
    private var incorrectAnswers = 0
    private var learningSessionId: SessionId? = null
    private var questionAttemptOrdinal: Int = 1

    private val _puzzles = MutableStateFlow(listOf(
        PuzzleInfo(1, "Cosmic Journey", R.drawable.cosmic_journey_thumbnail, 30, false, false, "Space", unlockCost = 0),
        PuzzleInfo(2, "Aqua Dreams", R.drawable.cosmic_journey_thumbnail, 30, false, false, "Nature", unlockCost = 0),
        PuzzleInfo(3, "Fantasy Castle", R.drawable.cosmic_journey_thumbnail, 16, false, true, "Fantasy", unlockCost = 0),
        PuzzleInfo(4, "Hidden City", R.drawable.cosmic_journey_thumbnail, 30, true, false, "Fantasy", unlockCost = 100),
        PuzzleInfo(5, "Forest Path", R.drawable.cosmic_journey_thumbnail, 16, true, false, "Nature", unlockCost = 150)
    ))
    val puzzles: StateFlow<List<PuzzleInfo>> = _puzzles.asStateFlow()

    private var timerJob: Job? = null

    init {
        preferencesRepository?.preferences
            ?.onEach { persisted ->
                _settings.update { current -> persisted.toUiSettings(current) }
            }
            ?.launchIn(viewModelScope)

        progressRepository?.progress
            ?.onEach { persisted ->
                val byId = persisted.associateBy { it.puzzleId.value.removePrefix("puzzle-").toIntOrNull() }
                _puzzles.update { puzzles ->
                    puzzles.map { puzzle ->
                        val progress = byId[puzzle.id] ?: return@map puzzle
                        puzzle.copy(
                            isCompleted = progress.completed,
                            maxScore = progress.bestScore.value,
                        )
                    }
                }
                _userProfile.update { profile ->
                    profile.copy(
                        puzzlesCompleted = persisted.count { it.completed },
                        totalScore = persisted.sumOf { it.bestScore.value },
                    )
                }
            }
            ?.launchIn(viewModelScope)

        learningRepository?.summaries
            ?.onEach { summaries -> _learningSummaries.value = summaries }
            ?.launchIn(viewModelScope)
    }

    private fun playSound(effect: SoundEffect) {
        if (_settings.value.soundEffectsEnabled) {
            viewModelScope.launch {
                _soundEvent.emit(effect)
            }
        }
    }

    fun selectPuzzle(puzzle: PuzzleInfo) {
        if (puzzle.isLocked) {
            playSound(SoundEffect.PUZZLE_LOCKED)
            return
        }
        playSound(SoundEffect.MENU_CLICK)
        timerJob?.cancel()
        val startedState = gameEngine.reduce(
            state = gameEngine.newGame(
                GameConfiguration(
                    puzzleId = PuzzleId("puzzle-${puzzle.id}"),
                    pieceCount = puzzle.totalPieces,
                    difficulty = _settings.value.difficulty.toEngineDifficulty(),
                ),
            ),
            action = GameAction.Start,
        ).state
        engineState = startedState
        learningSessionId = learningAttemptFactory?.newSessionId()
        questionAttemptOrdinal = 1
        correctAnswers = 0
        incorrectAnswers = 0
        _uiState.update { it.copy(
            currentPuzzle = puzzle,
            unlockedPieces = startedState.revealedPieces.asIndices(),
            revealingPiece = null,
            score = startedState.score.value,
            combo = startedState.combo,
            isGameOver = false,
            currentQuestion = startedState.question.toUiQuestion(),
            coins = 0,
            timeElapsed = 0,
            bestTime = puzzle.bestTime
        ) }
        enqueuePersistence {
            progressRepository?.startAttempt(startedState.puzzleId, startedState.pieceCount)
        }
        startTimer()
    }

    fun unlockPuzzle(puzzle: PuzzleInfo) {
        if (!puzzle.isLocked) return
        val currentCoins = _userProfile.value.totalCoins
        if (currentCoins >= puzzle.unlockCost) {
            playSound(SoundEffect.PUZZLE_UNLOCKED)
            _userProfile.update { it.copy(totalCoins = it.totalCoins - puzzle.unlockCost) }
            _puzzles.update { list ->
                list.map { p ->
                    if (p.id == puzzle.id) p.copy(isLocked = false) else p
                }
            }
        } else {
            playSound(SoundEffect.PUZZLE_LOCKED)
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(timeElapsed = it.timeElapsed + 1) }
            }
        }
    }

    fun onAnswerSelected(option: Int) {
        val currentEngineState = engineState ?: return
        val comboBeforeAnswer = currentEngineState.combo
        val answerTransition = gameEngine.reduce(
            currentEngineState,
            GameAction.SelectAnswer(option),
        )
        if (answerTransition.state == currentEngineState && answerTransition.events.isEmpty()) {
            return
        }

        var finalState = answerTransition.state
        val events = answerTransition.events.toMutableList()
        var coinReward = 0
        var shouldShake = false

        if (events.any { it is GameEvent.CorrectAnswer }) {
            correctAnswers++
            wrongAnswersInARow = 0
            playSound(SoundEffect.ANSWER_CORRECT)
            coinReward = 5 * comboBeforeAnswer
            playSound(SoundEffect.COINS_ADDED)

        } else if (events.any { it is GameEvent.IncorrectAnswer }) {
            incorrectAnswers++
            wrongAnswersInARow++
            if (wrongAnswersInARow == 3) {
                playSound(SoundEffect.MANY_FAILS)
            } else {
                playSound(SoundEffect.ANSWER_WRONG)
            }
            shouldShake = true
        }

        engineState = finalState
        _uiState.update { current ->
            current.copy(
                currentQuestion = finalState.question.toUiQuestion(),
                unlockedPieces = finalState.revealedPieces.asIndices(),
                revealingPiece = finalState.pendingPiece?.value,
                score = finalState.score.value,
                combo = finalState.combo,
                isGameOver = finalState.phase == GamePhase.COMPLETED,
                coins = current.coins + coinReward,
                shakeTrigger = if (shouldShake) current.shakeTrigger + 1 else current.shakeTrigger,
            )
        }

        val sessionId = learningSessionId
        val attemptFactory = learningAttemptFactory
        val repository = learningRepository
        if (sessionId != null && attemptFactory != null && repository != null) {
            val attempt = attemptFactory.create(
                sessionId = sessionId,
                question = currentEngineState.question,
                difficulty = currentEngineState.difficulty,
                selectedAnswer = option,
                attemptOrdinal = questionAttemptOrdinal,
            )
            enqueueLearningPersistence {
                repository.recordAttempt(attempt)
            }
        }
        if (events.any { it is GameEvent.IncorrectAnswer }) {
            questionAttemptOrdinal++
        }

    }

    fun onRevealAnimationFinished(pieceIndex: Int) {
        val currentEngineState = engineState ?: return
        if (currentEngineState.phase != GamePhase.REVEALING_PIECE ||
            currentEngineState.pendingPiece?.value != pieceIndex
        ) {
            return
        }
        val transition = gameEngine.reduce(currentEngineState, GameAction.RevealAnimationFinished)
        if (transition.state == currentEngineState) return

        val finalState = transition.state
        engineState = finalState
        questionAttemptOrdinal = 1
        playSound(SoundEffect.PIECE_PLACED)
        _uiState.update { current ->
            current.copy(
                currentQuestion = finalState.question.toUiQuestion(),
                unlockedPieces = finalState.revealedPieces.asIndices(),
                revealingPiece = null,
                isGameOver = finalState.phase == GamePhase.COMPLETED,
            )
        }

        if (transition.events.any { it is GameEvent.PieceRevealed }) {
            enqueuePersistence {
                progressRepository?.saveProgress(
                    puzzleId = finalState.puzzleId,
                    revealedPieces = finalState.revealedPieces.size,
                    totalPieces = finalState.pieceCount,
                    score = finalState.score,
                )
            }
        }
        if (transition.events.any { it == GameEvent.PuzzleCompleted }) {
            completePuzzle()
        }
    }

    fun pauseGame() {
        val currentState = engineState ?: return
        val transition = gameEngine.reduce(currentState, GameAction.Pause)
        if (transition.state == currentState) return

        engineState = transition.state
        timerJob?.cancel()
        timerJob = null
    }

    fun resumeGame() {
        val currentState = engineState ?: return
        val transition = gameEngine.reduce(currentState, GameAction.Resume)
        if (transition.state == currentState) return

        engineState = transition.state
        if (transition.state.phase != GamePhase.COMPLETED) {
            startTimer()
        }
    }

    private fun completePuzzle() {
        playSound(SoundEffect.GAME_COMPLETE)
        timerJob?.cancel()
        val finalTime = _uiState.value.timeElapsed
        val finalScore = _uiState.value.score
        val currentPuzzle = _uiState.value.currentPuzzle

        _userProfile.update {
            it.copy(
                puzzlesCompleted = it.puzzlesCompleted + if (currentPuzzle?.isCompleted == true) 0 else 1,
                totalCoins = it.totalCoins + _uiState.value.coins
            )
        }

        if (currentPuzzle != null) {
            val newBestTime = if (currentPuzzle.bestTime == null || finalTime < currentPuzzle.bestTime) finalTime else currentPuzzle.bestTime
            val newMaxScore = if (finalScore > currentPuzzle.maxScore) finalScore else currentPuzzle.maxScore
            
            _puzzles.update { list ->
                list.map { p ->
                    if (p.id == currentPuzzle.id) {
                        p.copy(isCompleted = true, bestTime = newBestTime, maxScore = newMaxScore)
                    } else p
                }
            }
            _uiState.update { it.copy(bestTime = newBestTime) }
        }

        val completedEngineState = engineState ?: return
        enqueuePersistence {
            progressRepository?.completePuzzle(
                GameSessionSummary(
                    puzzleId = completedEngineState.puzzleId,
                    score = completedEngineState.score,
                    correctAnswers = correctAnswers,
                    incorrectAnswers = incorrectAnswers,
                    durationMillis = finalTime * 1_000L,
                    completed = true,
                    endedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        val old = _settings.value
        if (old.difficulty != newSettings.difficulty) {
            playSound(SoundEffect.SWITCH_TOGGLE)
        } else if (old.soundEffectsEnabled != newSettings.soundEffectsEnabled ||
            old.backgroundMusicEnabled != newSettings.backgroundMusicEnabled ||
            old.hapticFeedbackEnabled != newSettings.hapticFeedbackEnabled ||
            old.showSpecialConfetti != newSettings.showSpecialConfetti) {
            
            val newValue = if (old.soundEffectsEnabled != newSettings.soundEffectsEnabled) newSettings.soundEffectsEnabled
            else if (old.backgroundMusicEnabled != newSettings.backgroundMusicEnabled) newSettings.backgroundMusicEnabled
            else if (old.hapticFeedbackEnabled != newSettings.hapticFeedbackEnabled) newSettings.hapticFeedbackEnabled
            else newSettings.showSpecialConfetti
            
            if (newValue) playSound(SoundEffect.SWITCH_ON) else playSound(SoundEffect.SWITCH_OFF)
        }
        _settings.value = newSettings
        preferencesRepository?.let { repository ->
            viewModelScope.launch {
                repository.setPreferences(newSettings.toPlayerPreferences())
            }
        }
    }

    fun resetProgress() {
        timerJob?.cancel()
        engineState = null
        learningSessionId = null
        questionAttemptOrdinal = 1
        val pendingLearningPersistence = learningPersistenceJob
        val clearUi = {
            _userProfile.update { UserProfileState() }
            _uiState.update { GameUiState() }
        }
        if (progressRepository == null) {
            clearUi()
        } else {
            enqueuePersistence {
                // Room clears progression and learning evidence in one transaction.
                // Order that transaction after every already-accepted answer so a
                // delayed learning write cannot repopulate the database after reset.
                pendingLearningPersistence?.join()
                progressRepository.resetAll()
                clearUi()
            }
        }
    }

    private fun enqueuePersistence(block: suspend () -> Unit) {
        val previous = persistenceJob
        persistenceJob = viewModelScope.launch {
            previous?.join()
            block()
        }
    }

    private fun enqueueLearningPersistence(block: suspend () -> Unit) {
        val previous = learningPersistenceJob
        learningPersistenceJob = viewModelScope.launch {
            previous?.join()
            block()
        }
    }

    class Factory(
        private val preferencesRepository: PreferencesRepository,
        private val progressRepository: ProgressRepository,
        private val learningRepository: LearningRepository,
        private val applicationVersion: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameViewModel::class.java))
            return GameViewModel(
                preferencesRepository = preferencesRepository,
                progressRepository = progressRepository,
                learningRepository = learningRepository,
                learningAttemptFactory = JigsawLearningAttemptFactory(
                    clock = SystemLearningClock,
                    idSource = UuidLearningIdSource(),
                    applicationVersion = applicationVersion,
                ),
            ) as T
        }
    }
}

private fun Difficulty.toEngineDifficulty(): EngineDifficulty = when (this) {
    Difficulty.Easy -> EngineDifficulty.EASY
    Difficulty.Medium -> EngineDifficulty.MEDIUM
    Difficulty.Hard -> EngineDifficulty.HARD
}

private fun EngineMathQuestion.toUiQuestion(): MathQuestion = MathQuestion(
    problem = problem,
    answer = answer,
    options = options,
)

private fun PlayerPreferences.toUiSettings(current: UserSettings): UserSettings = current.copy(
    soundEffectsEnabled = soundEnabled,
    soundEffectsVolume = soundVolume,
    backgroundMusicEnabled = musicEnabled,
    backgroundMusicVolume = musicVolume,
    hapticFeedbackEnabled = hapticsEnabled,
    difficulty = when (difficulty) {
        EngineDifficulty.EASY -> Difficulty.Easy
        EngineDifficulty.MEDIUM -> Difficulty.Medium
        EngineDifficulty.HARD -> Difficulty.Hard
    },
    graphicsQuality = graphicsQuality,
    reducedMotion = reducedMotion,
)

private fun UserSettings.toPlayerPreferences(): PlayerPreferences = PlayerPreferences(
    soundEnabled = soundEffectsEnabled,
    soundVolume = soundEffectsVolume,
    musicEnabled = backgroundMusicEnabled,
    musicVolume = backgroundMusicVolume,
    hapticsEnabled = hapticFeedbackEnabled,
    difficulty = difficulty.toEngineDifficulty(),
    graphicsQuality = graphicsQuality,
    reducedMotion = reducedMotion,
)
