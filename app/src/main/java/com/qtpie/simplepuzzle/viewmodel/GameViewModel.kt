package com.qtpie.simplepuzzle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.core.data.preferences.PreferencesRepository
import com.qtpie.simplepuzzle.core.data.progress.ProgressRepository
import com.qtpie.simplepuzzle.core.game.DefaultGameEngine
import com.qtpie.simplepuzzle.core.game.GameEngine
import com.qtpie.simplepuzzle.core.game.SeededRandomSource
import com.qtpie.simplepuzzle.core.model.GameAction
import com.qtpie.simplepuzzle.core.model.BoardMutationId
import com.qtpie.simplepuzzle.core.model.BoardMutationType
import com.qtpie.simplepuzzle.core.model.GameConfiguration
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GamePhase
import com.qtpie.simplepuzzle.core.model.GameModeCatalog
import com.qtpie.simplepuzzle.core.model.GameModeDefinition
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.GameState
import com.qtpie.simplepuzzle.core.model.GameSessionStatistics
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.GameTimerId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import com.qtpie.simplepuzzle.core.model.ModeOutcome
import com.qtpie.simplepuzzle.core.model.PlayerPreferences
import com.qtpie.simplepuzzle.core.model.PendingBoardMutation
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.Score
import com.qtpie.simplepuzzle.model.*
import com.qtpie.simplepuzzle.core.model.Difficulty as EngineDifficulty
import com.qtpie.simplepuzzle.core.model.MathQuestion as EngineMathQuestion
import kotlinx.coroutines.Job
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
    MANY_FAILS,
    HEART_LOSS,
    TIME_BONUS,
    MODE_FAILED,
}

data class GameUiState(
    val currentQuestion: MathQuestion = generateMathQuestion(),
    val unlockedPieces: Set<Int> = emptySet(),
    val pendingBoardMutation: PendingBoardMutation? = null,
    val sessionGeneration: Long = 1,
    val score: Int = 0,
    val combo: Int = 1,
    val shakeTrigger: Int = 0,
    val currentPuzzle: PuzzleInfo? = null,
    val isGameOver: Boolean = false,
    val mode: GameModeDefinition = GameModeCatalog.Classic,
    val heartsRemaining: Int? = null,
    val outcome: ModeOutcome? = null,
    val statistics: GameSessionStatistics = GameSessionStatistics(),
    val questionGeneration: Long = 1,
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

enum class GameFeedbackType {
    TIME_BONUS,
    TIME_PENALTY,
    HEART_LOST,
    PIECE_REMOVED,
}

data class GameFeedbackEvent(
    val id: Long,
    val type: GameFeedbackType,
    val amount: Long = 0,
)

private const val DEFAULT_GAME_SEED = 0x4A49475341574D41L

class GameViewModel(
    private val gameEngine: GameEngine = DefaultGameEngine(
        random = SeededRandomSource(DEFAULT_GAME_SEED),
    ),
    private val preferencesRepository: PreferencesRepository? = null,
    private val progressRepository: ProgressRepository? = null,
    private val gameClock: GameClock = MonotonicGameClock,
    timerScheduler: TimerScheduler? = null,
    initialPuzzles: List<PuzzleInfo> = listOf(
        PuzzleInfo(
            id = "cosmic-journey",
            name = "Cosmic Journey",
            imageResId = R.drawable.cosmic_journey_thumbnail,
            assetRoot = "puzzles/cosmic-journey",
            totalPieces = 30,
            unlockCost = 0,
        ),
    ),
) : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    private val _soundEvent = MutableSharedFlow<SoundEffect>()
    val soundEvent: SharedFlow<SoundEffect> = _soundEvent.asSharedFlow()

    private val _gameFeedback = MutableSharedFlow<GameFeedbackEvent>()
    val gameFeedback: SharedFlow<GameFeedbackEvent> = _gameFeedback.asSharedFlow()

    private val _selectedPuzzleForMode = MutableStateFlow<PuzzleInfo?>(null)
    val selectedPuzzleForMode: StateFlow<PuzzleInfo?> = _selectedPuzzleForMode.asStateFlow()

    private var wrongAnswersInARow = 0
    private var engineState: GameState? = null
    private var persistenceJob: Job? = null
    private var correctAnswers = 0
    private var incorrectAnswers = 0
    private var sessionGenerationCounter = 0L
    private var nextFeedbackId = 1L
    private val fastestModeCompletionMillis = mutableMapOf<Pair<String, GameModeId>, Long>()

    private val modeTimerCoordinator = ModeTimerCoordinator(
        clock = gameClock,
        scheduler = timerScheduler ?: CoroutineTimerScheduler(viewModelScope),
        onTimerExpired = ::onTimerExpired,
    )
    val timerUiState: StateFlow<ModeTimerUiState> = modeTimerCoordinator.uiState

    private val _puzzles = MutableStateFlow(initialPuzzles)
    val puzzles: StateFlow<List<PuzzleInfo>> = _puzzles.asStateFlow()

    init {
        preferencesRepository?.preferences
            ?.onEach { persisted ->
                _settings.update { current -> persisted.toUiSettings(current) }
            }
            ?.launchIn(viewModelScope)

        progressRepository?.progress
            ?.onEach { persisted ->
                val byId = persisted.associateBy { it.puzzleId.value }
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

        progressRepository?.let { repository ->
            viewModelScope.launch {
                repository.getModeBests().forEach { best ->
                    best.fastestCompletionMillis?.let { duration ->
                        fastestModeCompletionMillis[best.puzzleId.value to best.modeId] = duration
                    }
                }
            }
        }
    }

    private fun playSound(effect: SoundEffect) {
        if (_settings.value.soundEffectsEnabled) {
            viewModelScope.launch {
                _soundEvent.emit(effect)
            }
        }
    }

    fun selectPuzzle(
        puzzle: PuzzleInfo,
        modeId: GameModeId = GameModeId.CLASSIC,
    ) {
        if (puzzle.isLocked) {
            playSound(SoundEffect.PUZZLE_LOCKED)
            return
        }
        playSound(SoundEffect.MENU_CLICK)
        modeTimerCoordinator.cancelAll()
        sessionGenerationCounter++
        val mode = GameModeCatalog.definition(modeId)
        val startTransition = gameEngine.reduce(
            state = gameEngine.newGame(
                GameConfiguration(
                    puzzleId = PuzzleId(puzzle.id),
                    pieceCount = puzzle.totalPieces,
                    difficulty = _settings.value.difficulty.toEngineDifficulty(),
                    modeId = mode.id,
                    modeRules = mode,
                    sessionGeneration = sessionGenerationCounter,
                ),
            ),
            action = GameAction.Start,
        )
        val startedState = startTransition.state
        engineState = startedState
        modeTimerCoordinator.startSession(startedState.sessionGeneration)
        modeTimerCoordinator.apply(startTransition.events)
        correctAnswers = 0
        incorrectAnswers = 0
        _uiState.update { it.copy(
            currentPuzzle = puzzle,
            unlockedPieces = startedState.revealedPieces.asIndices(),
            pendingBoardMutation = null,
            sessionGeneration = startedState.sessionGeneration,
            score = startedState.score.value,
            combo = startedState.combo,
            isGameOver = false,
            currentQuestion = startedState.question.toUiQuestion(),
            questionGeneration = startedState.questionGeneration,
            mode = startedState.modeRules,
            heartsRemaining = startedState.heartsRemaining,
            outcome = null,
            statistics = startedState.statistics,
            coins = 0,
            timeElapsed = 0,
            bestTime = fastestModeCompletionMillis[puzzle.id to modeId]
                ?.let { (it / 1_000L).toInt() }
                ?: if (modeId == GameModeId.CLASSIC) puzzle.bestTime else null
        ) }
        enqueuePersistence {
            progressRepository?.startAttempt(startedState.puzzleId, startedState.pieceCount)
        }
        preferencesRepository?.let { repository ->
            viewModelScope.launch { repository.setLastSelectedMode(modeId) }
        }
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

    fun onAnswerSelected(option: Int) {
        val currentEngineState = engineState ?: return
        val comboBeforeAnswer = currentEngineState.combo
        val answerTransition = gameEngine.reduce(
            currentEngineState,
            GameAction.SelectAnswer(option, currentEngineState.questionGeneration),
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
                questionGeneration = finalState.questionGeneration,
                unlockedPieces = finalState.revealedPieces.asIndices(),
                pendingBoardMutation = finalState.pendingBoardMutation,
                sessionGeneration = finalState.sessionGeneration,
                score = finalState.score.value,
                combo = finalState.combo,
                isGameOver = finalState.phase == GamePhase.COMPLETED,
                heartsRemaining = finalState.heartsRemaining,
                outcome = finalState.outcome,
                statistics = finalState.statistics,
                coins = current.coins + coinReward,
                shakeTrigger = if (shouldShake) current.shakeTrigger + 1 else current.shakeTrigger,
            )
        }
        modeTimerCoordinator.apply(events)
        emitGameFeedback(events)
        if (events.any { it is GameEvent.ModeEnded } && finalState.phase == GamePhase.ENDED) {
            finishFailedRun()
        }

    }

    fun onBoardMutationFinished(mutationId: BoardMutationId) {
        val currentEngineState = engineState ?: return
        if (currentEngineState.phase != GamePhase.MUTATING_BOARD ||
            currentEngineState.pendingBoardMutation?.id != mutationId
        ) {
            return
        }
        val committedMutation = requireNotNull(currentEngineState.pendingBoardMutation)
        val transition = gameEngine.reduce(
            currentEngineState,
            GameAction.BoardMutationFinished(
                mutationId = mutationId,
                sessionTimerRemainingMillis = modeTimerCoordinator.remainingMillis(GameTimerKind.SESSION),
            ),
        )
        if (transition.state == currentEngineState) return

        val finalState = transition.state
        engineState = finalState
        playSound(
            if (committedMutation.type == BoardMutationType.REVEAL) {
                SoundEffect.PIECE_PLACED
            } else {
                SoundEffect.PIECE_REMOVED
            },
        )
        _uiState.update { current ->
            current.copy(
                currentQuestion = finalState.question.toUiQuestion(),
                questionGeneration = finalState.questionGeneration,
                unlockedPieces = finalState.revealedPieces.asIndices(),
                pendingBoardMutation = null,
                isGameOver = finalState.phase == GamePhase.COMPLETED,
                outcome = finalState.outcome,
                statistics = finalState.statistics,
            )
        }
        modeTimerCoordinator.apply(transition.events)
        emitGameFeedback(transition.events)

        if (transition.events.any { it is GameEvent.PieceRevealed } &&
            finalState.modeId == GameModeId.CLASSIC
        ) {
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
        modeTimerCoordinator.pause()
        val transition = gameEngine.reduce(currentState, GameAction.Pause)
        if (transition.state == currentState) return

        engineState = transition.state
    }

    fun resumeGame() {
        val currentState = engineState ?: return
        val transition = gameEngine.reduce(currentState, GameAction.Resume)
        if (transition.state == currentState) return

        engineState = transition.state
        modeTimerCoordinator.resume()
    }

    private fun completePuzzle() {
        playSound(SoundEffect.GAME_COMPLETE)
        val completedEngineState = engineState ?: return
        val finalDurationMillis = modeTimerCoordinator.elapsedMillis()
        val finalTime = (finalDurationMillis / 1_000L).toInt()
        val finalScore = _uiState.value.score
        val currentPuzzle = _uiState.value.currentPuzzle

        _userProfile.update {
            it.copy(
                puzzlesCompleted = it.puzzlesCompleted + if (currentPuzzle?.isCompleted == true) 0 else 1,
                totalCoins = it.totalCoins + _uiState.value.coins
            )
        }

        if (currentPuzzle != null) {
            val isClassic = completedEngineState.modeId == GameModeId.CLASSIC
            val newBestTime = if (isClassic && (currentPuzzle.bestTime == null || finalTime < currentPuzzle.bestTime)) {
                finalTime
            } else {
                currentPuzzle.bestTime
            }
            val modeBestKey = currentPuzzle.id to completedEngineState.modeId
            val newModeBestMillis = fastestModeCompletionMillis[modeBestKey]
                ?.let { minOf(it, finalDurationMillis) }
                ?: finalDurationMillis
            fastestModeCompletionMillis[modeBestKey] = newModeBestMillis
            val newMaxScore = if (isClassic && finalScore > currentPuzzle.maxScore) {
                finalScore
            } else {
                currentPuzzle.maxScore
            }
            
            _puzzles.update { list ->
                list.map { p ->
                    if (p.id == currentPuzzle.id) {
                        p.copy(isCompleted = true, bestTime = newBestTime, maxScore = newMaxScore)
                    } else p
                }
            }
            _uiState.update { it.copy(bestTime = (newModeBestMillis / 1_000L).toInt()) }
        }

        modeTimerCoordinator.cancelAll()
        _uiState.update { it.copy(timeElapsed = finalTime) }
        enqueuePersistence {
            progressRepository?.completePuzzle(
                GameSessionSummary(
                    puzzleId = completedEngineState.puzzleId,
                    score = completedEngineState.score,
                    correctAnswers = completedEngineState.statistics.correctAnswers,
                    incorrectAnswers = completedEngineState.statistics.incorrectAnswers,
                    durationMillis = finalDurationMillis,
                    completed = true,
                    endedAtEpochMillis = System.currentTimeMillis(),
                    modeId = completedEngineState.modeId,
                    outcome = requireNotNull(completedEngineState.outcome),
                    timeoutCount = completedEngineState.statistics.timeoutCount,
                    maximumCombo = completedEngineState.statistics.maximumCombo,
                    piecesRevealed = completedEngineState.statistics.piecesRevealed,
                    piecesRemoved = completedEngineState.statistics.piecesRemoved,
                    livesRemaining = completedEngineState.heartsRemaining,
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
        modeTimerCoordinator.cancelAll()
        engineState = null
        fastestModeCompletionMillis.clear()
        val clearUi = {
            _userProfile.update { UserProfileState() }
            _uiState.update { GameUiState() }
        }
        if (progressRepository == null) {
            clearUi()
        } else {
            enqueuePersistence {
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

    private fun onTimerExpired(timerId: GameTimerId) {
        val currentState = engineState ?: return
        val transition = gameEngine.reduce(currentState, GameAction.TimerExpired(timerId))
        if (transition.state == currentState && transition.events.isEmpty()) return
        engineState = transition.state
        val finalState = transition.state
        _uiState.update { current ->
            current.copy(
                currentQuestion = finalState.question.toUiQuestion(),
                questionGeneration = finalState.questionGeneration,
                unlockedPieces = finalState.revealedPieces.asIndices(),
                pendingBoardMutation = finalState.pendingBoardMutation,
                score = finalState.score.value,
                combo = finalState.combo,
                heartsRemaining = finalState.heartsRemaining,
                outcome = finalState.outcome,
                statistics = finalState.statistics,
                isGameOver = finalState.phase == GamePhase.COMPLETED || finalState.phase == GamePhase.ENDED,
            )
        }
        modeTimerCoordinator.apply(transition.events)
        emitGameFeedback(transition.events)
        if (transition.events.any { it is GameEvent.ModeEnded }) {
            if (finalState.phase == GamePhase.COMPLETED) completePuzzle() else finishFailedRun()
        }
    }

    fun choosePuzzleForMode(puzzle: PuzzleInfo): Boolean {
        if (puzzle.isLocked) {
            playSound(SoundEffect.PUZZLE_LOCKED)
            return false
        }
        playSound(SoundEffect.MENU_CLICK)
        _selectedPuzzleForMode.value = puzzle
        return true
    }

    fun startSelectedMode(modeId: GameModeId): Boolean {
        val puzzle = _selectedPuzzleForMode.value ?: return false
        selectPuzzle(puzzle, modeId)
        return true
    }

    private fun finishFailedRun() {
        val elapsedSeconds = (modeTimerCoordinator.elapsedMillis() / 1_000L).toInt()
        val failedState = engineState
        val failedOutcome = failedState?.outcome
        modeTimerCoordinator.cancelAll()
        playSound(SoundEffect.MODE_FAILED)
        _uiState.update { it.copy(timeElapsed = elapsedSeconds, isGameOver = true) }
        if (failedState != null && failedOutcome != null) {
            enqueuePersistence {
                progressRepository?.recordSession(
                    GameSessionSummary(
                        puzzleId = failedState.puzzleId,
                        score = failedState.score,
                        correctAnswers = failedState.statistics.correctAnswers,
                        incorrectAnswers = failedState.statistics.incorrectAnswers,
                        durationMillis = elapsedSeconds * 1_000L,
                        completed = false,
                        endedAtEpochMillis = System.currentTimeMillis(),
                        modeId = failedState.modeId,
                        outcome = failedOutcome,
                        timeoutCount = failedState.statistics.timeoutCount,
                        maximumCombo = failedState.statistics.maximumCombo,
                        piecesRevealed = failedState.statistics.piecesRevealed,
                        piecesRemoved = failedState.statistics.piecesRemoved,
                        livesRemaining = failedState.heartsRemaining,
                    ),
                )
            }
        }
    }

    private fun emitGameFeedback(events: List<GameEvent>) {
        events.forEach { event ->
            val feedback = when (event) {
                is GameEvent.TimerAdjustmentRequested -> GameFeedbackEvent(
                    id = nextFeedbackId++,
                    type = if (event.deltaMillis >= 0) {
                        GameFeedbackType.TIME_BONUS
                    } else {
                        GameFeedbackType.TIME_PENALTY
                    },
                    amount = event.deltaMillis,
                )
                is GameEvent.HeartsChanged -> GameFeedbackEvent(
                    id = nextFeedbackId++,
                    type = GameFeedbackType.HEART_LOST,
                    amount = event.heartsRemaining.toLong(),
                )
                is GameEvent.PieceRemoved -> GameFeedbackEvent(
                    id = nextFeedbackId++,
                    type = GameFeedbackType.PIECE_REMOVED,
                )
                else -> null
            }
            if (feedback != null) {
                when (feedback.type) {
                    GameFeedbackType.TIME_BONUS -> playSound(SoundEffect.TIME_BONUS)
                    GameFeedbackType.HEART_LOST -> playSound(SoundEffect.HEART_LOSS)
                    GameFeedbackType.PIECE_REMOVED -> playSound(SoundEffect.PIECE_REMOVED)
                    GameFeedbackType.TIME_PENALTY -> Unit
                }
                viewModelScope.launch { _gameFeedback.emit(feedback) }
            }
        }
    }

    fun restartGame() {
        val currentState = engineState ?: return
        val transition = gameEngine.reduce(currentState, GameAction.Restart)
        engineState = transition.state
        sessionGenerationCounter = maxOf(sessionGenerationCounter, transition.state.sessionGeneration)
        correctAnswers = 0
        incorrectAnswers = 0
        wrongAnswersInARow = 0
        modeTimerCoordinator.startSession(transition.state.sessionGeneration)
        modeTimerCoordinator.apply(transition.events)
        val restarted = transition.state
        _uiState.update { current ->
            current.copy(
                currentQuestion = restarted.question.toUiQuestion(),
                questionGeneration = restarted.questionGeneration,
                unlockedPieces = emptySet(),
                pendingBoardMutation = null,
                sessionGeneration = restarted.sessionGeneration,
                score = 0,
                combo = 1,
                heartsRemaining = restarted.heartsRemaining,
                outcome = null,
                statistics = restarted.statistics,
                isGameOver = false,
                coins = 0,
                timeElapsed = 0,
            )
        }
        enqueuePersistence {
            progressRepository?.startAttempt(restarted.puzzleId, restarted.pieceCount)
        }
    }

    /** Ends a navigation-abandoned run without altering permanent puzzle progress. */
    fun endSessionForNavigation() {
        val currentState = engineState
        val elapsedMillis = modeTimerCoordinator.elapsedMillis()
        if (currentState != null &&
            currentState.phase != GamePhase.COMPLETED &&
            currentState.phase != GamePhase.ENDED &&
            currentState.phase != GamePhase.READY
        ) {
            val transition = gameEngine.reduce(currentState, GameAction.Abandon)
            engineState = transition.state
            modeTimerCoordinator.apply(transition.events)
            val abandoned = transition.state
            if (abandoned.outcome == ModeOutcome.ABANDONED) {
                enqueuePersistence {
                    progressRepository?.recordSession(
                        GameSessionSummary(
                            puzzleId = abandoned.puzzleId,
                            score = abandoned.score,
                            correctAnswers = abandoned.statistics.correctAnswers,
                            incorrectAnswers = abandoned.statistics.incorrectAnswers,
                            durationMillis = elapsedMillis,
                            completed = false,
                            endedAtEpochMillis = System.currentTimeMillis(),
                            modeId = abandoned.modeId,
                            outcome = ModeOutcome.ABANDONED,
                            timeoutCount = abandoned.statistics.timeoutCount,
                            maximumCombo = abandoned.statistics.maximumCombo,
                            piecesRevealed = abandoned.statistics.piecesRevealed,
                            piecesRemoved = abandoned.statistics.piecesRemoved,
                            livesRemaining = abandoned.heartsRemaining,
                        ),
                    )
                }
            }
        }
        modeTimerCoordinator.cancelAll()
    }

    override fun onCleared() {
        modeTimerCoordinator.cancelAll()
        super.onCleared()
    }

    class Factory(
        private val preferencesRepository: PreferencesRepository,
        private val progressRepository: ProgressRepository,
        private val puzzles: List<PuzzleInfo>,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            require(modelClass.isAssignableFrom(GameViewModel::class.java))
            return GameViewModel(
                preferencesRepository = preferencesRepository,
                progressRepository = progressRepository,
                initialPuzzles = puzzles,
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
