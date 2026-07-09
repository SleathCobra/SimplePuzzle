package com.qtpie.simplepuzzle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val name: String = "AlexPuzzles",
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

    private val _puzzles = MutableStateFlow(listOf(
        PuzzleInfo(1, "Cosmic Journey", R.drawable.puzzle, 30, false, false, "Space"),
        PuzzleInfo(2, "Aqua Dreams", R.drawable.puzzle, 30, false, false, "Nature"),
        PuzzleInfo(3, "Fantasy Castle", R.drawable.puzzle, 16, false, true, "Fantasy"),
        PuzzleInfo(4, "Hidden City", R.drawable.puzzle, 30, true, false, "Fantasy"),
        PuzzleInfo(5, "Forest Path", R.drawable.puzzle, 16, true, false, "Nature")
    ))
    val puzzles: StateFlow<List<PuzzleInfo>> = _puzzles.asStateFlow()

    private var timerJob: Job? = null

    fun selectPuzzle(puzzle: PuzzleInfo) {
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
        } else {
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
        timerJob?.cancel()
        val finalTime = _uiState.value.timeElapsed
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
            
            _puzzles.update { list ->
                list.map { p ->
                    if (p.id == currentPuzzle.id) {
                        p.copy(isCompleted = true, bestTime = newBestTime)
                    } else p
                }
            }
            _uiState.update { it.copy(bestTime = newBestTime) }
        }
    }

    fun updateSettings(newSettings: UserSettings) {
        _settings.value = newSettings
    }

    fun resetProgress() {
        _userProfile.update { UserProfileState() }
        _uiState.update { GameUiState() }
    }
}
