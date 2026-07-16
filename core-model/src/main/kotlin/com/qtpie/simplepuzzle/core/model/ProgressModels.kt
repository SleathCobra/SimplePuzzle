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
    val modeId: GameModeId = GameModeId.CLASSIC,
    val outcome: ModeOutcome = if (completed) ModeOutcome.COMPLETED else ModeOutcome.ABANDONED,
    val timeoutCount: Int = 0,
    val maximumCombo: Int = 1,
    val piecesRevealed: Int = correctAnswers,
    val piecesRemoved: Int = 0,
    val livesRemaining: Int? = null,
) {
    init {
        require(correctAnswers >= 0 && incorrectAnswers >= 0) {
            "Answer counts must be non-negative."
        }
        require(durationMillis >= 0) { "Session duration must be non-negative." }
        require(timeoutCount >= 0 && piecesRevealed >= 0 && piecesRemoved >= 0) {
            "Timeout and piece counts must be non-negative."
        }
        require(maximumCombo >= 1) { "Maximum combo must be at least one." }
        require(livesRemaining == null || livesRemaining >= 0) { "Lives remaining must be non-negative." }
        require(completed == (outcome == ModeOutcome.COMPLETED)) {
            "Completed sessions must use the completed mode outcome and vice versa."
        }
    }
}

data class ModeBest(
    val puzzleId: PuzzleId,
    val modeId: GameModeId,
    val bestScore: Score,
    val fastestCompletionMillis: Long?,
    val highestCombo: Int,
    val fewestMistakes: Int?,
    val mostRecentCompletionEpochMillis: Long?,
) {
    init {
        require(fastestCompletionMillis == null || fastestCompletionMillis >= 0) {
            "Fastest completion must be non-negative."
        }
        require(highestCombo >= 1) { "Highest combo must be at least one." }
        require(fewestMistakes == null || fewestMistakes >= 0) { "Fewest mistakes must be non-negative." }
    }
}
