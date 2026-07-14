package com.qtpie.simplepuzzle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.model.*
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
    val currentQuestion: MathQuestion = generateMathQuestion(),
    val unlockedPieces: Set<Int> = emptySet(),
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

class GameViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(UserSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private val _userProfile = MutableStateFlow(UserProfileState())
    val userProfile: StateFlow<UserProfileState> = _userProfile.asStateFlow()

    private val _soundEvent = MutableSharedFlow<SoundEffect>()
    val soundEvent: SharedFlow<SoundEffect> = _soundEvent.asSharedFlow()

    private var wrongAnswersInARow = 0

    private val _puzzles = MutableStateFlow(listOf(
        PuzzleInfo(1, "Cosmic Journey", R.drawable.puzzle, 30, false, false, "Space", unlockCost = 0),
        PuzzleInfo(2, "Aqua Dreams", R.drawable.puzzle, 30, false, false, "Nature", unlockCost = 0),
        PuzzleInfo(3, "Fantasy Castle", R.drawable.puzzle, 16, false, true, "Fantasy", unlockCost = 0),
        PuzzleInfo(4, "Hidden City", R.drawable.puzzle, 30, true, false, "Fantasy", unlockCost = 100),
        PuzzleInfo(5, "Forest Path", R.drawable.puzzle, 16, true, false, "Nature", unlockCost = 150)
    ))
    val puzzles: StateFlow<List<PuzzleInfo>> = _puzzles.asStateFlow()

    private var timerJob: Job? = null

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
        _uiState.update { it.copy(
            currentPuzzle = puzzle,
            unlockedPieces = emptySet(),
            score = 0,
            combo = 1,
            isGameOver = false,
            currentQuestion = generateMathQuestion(_settings.value.difficulty),
            coins = 0,
            timeElapsed = 0,
            bestTime = puzzle.bestTime
        ) }
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
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                _uiState.update { it.copy(timeElapsed = it.timeElapsed + 1) }
            }
        }
    }

    fun onAnswerSelected(option: Int) {
        val currentState = _uiState.value
        if (option == currentState.currentQuestion.answer) {
            wrongAnswersInARow = 0
            playSound(SoundEffect.ANSWER_CORRECT)
            val newScore = currentState.score + (10 * currentState.combo)
            val newCombo = currentState.combo + 1
            val coinReward = 5 * currentState.combo
            unlockRandomPiece()
            _uiState.update { it.copy(
                score = newScore,
                combo = newCombo,
                coins = it.coins + coinReward,
                currentQuestion = generateMathQuestion(_settings.value.difficulty)
            ) }
            playSound(SoundEffect.COINS_ADDED)
        } else {
            wrongAnswersInARow++
            if (wrongAnswersInARow == 3) {
                playSound(SoundEffect.MANY_FAILS)
            } else {
                playSound(SoundEffect.ANSWER_WRONG)
            }
            _uiState.update { it.copy(
                combo = 1,
                shakeTrigger = it.shakeTrigger + 1
            ) }
        }
    }

    private fun unlockRandomPiece() {
        val currentState = _uiState.value
        val totalPieces = currentState.currentPuzzle?.totalPieces ?: 16
        val remaining = (0 until totalPieces).toSet() - currentState.unlockedPieces
        if (remaining.isNotEmpty()) {
            val randomPiece = remaining.random()
            val nextUnlocked = currentState.unlockedPieces + randomPiece
            _uiState.update { it.copy(unlockedPieces = nextUnlocked) }
            
            if (nextUnlocked.size == totalPieces) {
                completePuzzle()
            }
        }
    }

    private fun completePuzzle() {
        playSound(SoundEffect.GAME_COMPLETE)
        timerJob?.cancel()
        val finalTime = _uiState.value.timeElapsed
        val finalScore = _uiState.value.score
        val currentPuzzle = _uiState.value.currentPuzzle
        
        _uiState.update { it.copy(isGameOver = true) }
        
        _userProfile.update { 
            it.copy(
                puzzlesCompleted = it.puzzlesCompleted + 1,
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
    }

    fun resetProgress() {
        _userProfile.update { UserProfileState() }
        _uiState.update { GameUiState() }
    }
}
