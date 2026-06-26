package com.qtpie.simplepuzzle.viewmodel

import androidx.lifecycle.ViewModel
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class GameUiState(
    val currentQuestion: MathQuestion = generateMathQuestion(),
    val unlockedPieces: Set<Int> = emptySet(),
    val score: Int = 0,
    val combo: Int = 1,
    val shakeTrigger: Int = 0,
    val currentPuzzle: PuzzleInfo? = null,
    val isGameOver: Boolean = false
)

data class UserProfileState(
    val name: String = "AlexPuzzles",
    val totalScore: Int = 15420,
    val puzzlesCompleted: Int = 3,
    val totalPuzzles: Int = 12
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

    fun selectPuzzle(puzzle: PuzzleInfo) {
        _uiState.update { it.copy(
            currentPuzzle = puzzle,
            unlockedPieces = emptySet(),
            score = 0,
            combo = 1,
            isGameOver = false,
            currentQuestion = generateMathQuestion(_settings.value.difficulty)
        ) }
    }

    fun onAnswerSelected(option: Int) {
        val currentState = _uiState.value
        if (option == currentState.currentQuestion.answer) {
            val newScore = currentState.score + (10 * currentState.combo)
            val newCombo = currentState.combo + 1
            unlockRandomPiece()
            _uiState.update { it.copy(
                score = newScore,
                combo = newCombo,
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
                _uiState.update { it.copy(isGameOver = true) }
                _userProfile.update { it.copy(puzzlesCompleted = it.puzzlesCompleted + 1) }
            }
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
