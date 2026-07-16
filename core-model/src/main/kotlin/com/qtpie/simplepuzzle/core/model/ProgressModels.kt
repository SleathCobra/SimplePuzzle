package com.qtpie.simplepuzzle.core.model

data class PuzzleProgress(
    val puzzleId: PuzzleId,
    val revealedPieces: Int,
    val totalPieces: Int,
    val completed: Boolean,
    val bestScore: Score,
    val attempts: Int,
    val completedAtEpochMillis: Long?,
    val lastPlayedAtEpochMillis: Long,
) {
    init {
        require(totalPieces > 0) { "Total pieces must be positive." }
        require(revealedPieces in 0..totalPieces) { "Revealed pieces must be inside puzzle bounds." }
        require(attempts >= 0) { "Attempts must be non-negative." }
        require(!completed || revealedPieces == totalPieces) {
            "A completed puzzle must have every piece revealed."
        }
    }
}

data class GameSessionSummary(
    val puzzleId: PuzzleId,
    val score: Score,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val durationMillis: Long,
    val completed: Boolean,
    val endedAtEpochMillis: Long,
) {
    init {
        require(correctAnswers >= 0 && incorrectAnswers >= 0) {
            "Answer counts must be non-negative."
        }
        require(durationMillis >= 0) { "Session duration must be non-negative." }
    }
}
