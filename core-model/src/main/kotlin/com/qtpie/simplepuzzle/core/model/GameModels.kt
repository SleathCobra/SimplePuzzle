package com.qtpie.simplepuzzle.core.model

@JvmInline
value class PuzzleId(val value: String) {
    init {
        require(value.isNotBlank()) { "PuzzleId cannot be blank." }
    }
}

@JvmInline
value class PieceId(val value: Int) {
    init {
        require(value >= 0) { "PieceId must be non-negative." }
    }
}

@JvmInline
value class Score(val value: Int) {
    init {
        require(value >= 0) { "Score must be non-negative." }
    }
}

enum class Difficulty {
    EASY,
    MEDIUM,
    HARD,
}

enum class GraphicsQuality {
    AUTO,
    LOW,
    MEDIUM,
    HIGH,
}

data class PlayerPreferences(
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.8f,
    val musicEnabled: Boolean = true,
    val musicVolume: Float = 0.5f,
    val hapticsEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.MEDIUM,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.AUTO,
    val reducedMotion: Boolean = false,
    val lastSelectedMode: GameModeId = GameModeId.CLASSIC,
) {
    init {
        require(soundVolume in 0f..1f) { "Sound volume must be between zero and one." }
        require(musicVolume in 0f..1f) { "Music volume must be between zero and one." }
    }
}

enum class MathOperation(val symbol: String) {
    ADD("+"),
    SUBTRACT("-");

    fun evaluate(left: Int, right: Int): Int = when (this) {
        ADD -> left + right
        SUBTRACT -> left - right
    }
}

data class MathQuestion(
    val leftOperand: Int,
    val operation: MathOperation,
    val rightOperand: Int,
    val answer: Int,
    val options: List<Int>,
) {
    init {
        require(leftOperand >= 0 && rightOperand >= 0) {
            "Question operands must be non-negative."
        }
        require(answer == operation.evaluate(leftOperand, rightOperand)) {
            "Question answer does not match its operands and operation."
        }
        require(options.size >= 2) { "A question must have at least two answer options." }
        require(options.distinct().size == options.size) { "Answer options must be unique." }
        require(answer in options) { "Answer options must contain the correct answer." }
    }

    val problem: String
        get() = "$leftOperand ${operation.symbol} $rightOperand"
}

class PieceSet private constructor(
    private val indices: Set<Int>,
) {
    val size: Int
        get() = indices.size

    operator fun contains(pieceId: PieceId): Boolean = pieceId.value in indices

    fun reveal(pieceId: PieceId, pieceCount: Int): PieceSet {
        require(pieceCount > 0) { "Piece count must be positive." }
        require(pieceId.value < pieceCount) {
            "Piece ${pieceId.value} is outside a puzzle with $pieceCount pieces."
        }
        return if (pieceId.value in indices) this else PieceSet(indices + pieceId.value)
    }

    fun remove(pieceId: PieceId, pieceCount: Int): PieceSet {
        require(pieceCount > 0) { "Piece count must be positive." }
        require(pieceId.value < pieceCount) {
            "Piece ${pieceId.value} is outside a puzzle with $pieceCount pieces."
        }
        return if (pieceId.value !in indices) this else PieceSet(indices - pieceId.value)
    }

    fun asIndices(): Set<Int> = indices

    fun hiddenIndices(pieceCount: Int): List<Int> {
        require(pieceCount > 0) { "Piece count must be positive." }
        return (0 until pieceCount).filterNot(indices::contains)
    }

    override fun equals(other: Any?): Boolean = other is PieceSet && indices == other.indices

    override fun hashCode(): Int = indices.hashCode()

    override fun toString(): String = "PieceSet(indices=$indices)"

    companion object {
        val Empty: PieceSet = PieceSet(emptySet())

        fun fromIndices(indices: Collection<Int>, pieceCount: Int): PieceSet {
            require(pieceCount > 0) { "Piece count must be positive." }
            require(indices.all { it in 0 until pieceCount }) {
                "Every revealed piece must be inside the puzzle bounds."
            }
            return PieceSet(indices.toSet())
        }
    }
}

enum class GamePhase {
    READY,
    AWAITING_ANSWER,
    MUTATING_BOARD,
    PAUSED,
    COMPLETED,
    ENDED,
}

data class BoardMutationId(
    val sessionGeneration: Long,
    val sequence: Long,
) {
    init {
        require(sessionGeneration > 0) { "Mutation session generation must be positive." }
        require(sequence > 0) { "Mutation sequence must be positive." }
    }
}

enum class BoardMutationType {
    REVEAL,
    REMOVE,
}

enum class GameTimerKind {
    SESSION,
    QUESTION,
    DECAY,
    MAXIMUM_SESSION,
}

data class GameTimerId(
    val sessionGeneration: Long,
    val kind: GameTimerKind,
    val generation: Long,
) {
    init {
        require(sessionGeneration > 0) { "Timer session generation must be positive." }
        require(generation > 0) { "Timer generation must be positive." }
    }
}

data class ActiveGameTimer(
    val id: GameTimerId,
    val durationMillis: Long,
) {
    init {
        require(durationMillis > 0) { "Active timer duration must be positive." }
    }
}

enum class ModeOutcome {
    COMPLETED,
    TIME_EXPIRED,
    OUT_OF_HEARTS,
    DECAY_LIMIT_REACHED,
    ABANDONED,
}

enum class GameEndReason {
    PUZZLE_COMPLETED,
    SESSION_TIMER_EXPIRED,
    HEARTS_DEPLETED,
    MAXIMUM_SESSION_EXPIRED,
    PLAYER_ABANDONED,
}

data class GameSessionStatistics(
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val timeoutCount: Int = 0,
    val maximumCombo: Int = 1,
    val piecesRevealed: Int = 0,
    val piecesRemoved: Int = 0,
) {
    init {
        require(
            correctAnswers >= 0 && incorrectAnswers >= 0 && timeoutCount >= 0 &&
                piecesRevealed >= 0 && piecesRemoved >= 0,
        ) { "Session counters must be non-negative." }
        require(maximumCombo >= 1) { "Maximum combo must be at least one." }
    }
}

data class PendingBoardMutation(
    val id: BoardMutationId,
    val pieceId: PieceId,
    val type: BoardMutationType,
)

data class GameConfiguration(
    val puzzleId: PuzzleId,
    val pieceCount: Int,
    val difficulty: Difficulty,
    val modeId: GameModeId = GameModeId.CLASSIC,
    val modeRules: GameModeDefinition = GameModeCatalog.Classic,
    val sessionGeneration: Long = 1,
) {
    init {
        require(pieceCount > 0) { "A puzzle must contain at least one piece." }
        require(modeId == modeRules.id) {
            "Selected mode ID must match the resolved immutable mode rules."
        }
        require(sessionGeneration > 0) { "Session generation must be positive." }
    }
}

data class GameState(
    val puzzleId: PuzzleId,
    val revealedPieces: PieceSet,
    val pieceCount: Int,
    val score: Score,
    val combo: Int,
    val question: MathQuestion,
    val difficulty: Difficulty,
    val modeId: GameModeId,
    val modeRules: GameModeDefinition,
    val phase: GamePhase,
    val pendingBoardMutation: PendingBoardMutation? = null,
    val resumePhase: GamePhase? = null,
    val sessionGeneration: Long = 1,
    val nextMutationSequence: Long = 1,
    val questionGeneration: Long = 1,
    val activeTimers: List<ActiveGameTimer> = emptyList(),
    val nextTimerGeneration: Long = 1,
    val heartsRemaining: Int? = modeRules.mistakes.startingHearts,
    val statistics: GameSessionStatistics = GameSessionStatistics(),
    val outcome: ModeOutcome? = null,
    val endReason: GameEndReason? = null,
) {
    init {
        require(pieceCount > 0) { "A puzzle must contain at least one piece." }
        require(modeId == modeRules.id) { "Game mode ID and resolved rules must match." }
        require(combo >= 1) { "Combo must be at least one." }
        require(revealedPieces.asIndices().all { it in 0 until pieceCount }) {
            "Revealed pieces must be inside the puzzle bounds."
        }
        require(sessionGeneration > 0) { "Session generation must be positive." }
        require(nextMutationSequence > 0) { "Next mutation sequence must be positive." }
        require(questionGeneration > 0) { "Question generation must be positive." }
        require(nextTimerGeneration > 0) { "Next timer generation must be positive." }
        require(activeTimers.map { it.id.kind }.distinct().size == activeTimers.size) {
            "Only one active timer of each kind is allowed."
        }
        require(activeTimers.all { it.id.sessionGeneration == sessionGeneration }) {
            "Every active timer must belong to the active session generation."
        }
        require(heartsRemaining == null || heartsRemaining >= 0) {
            "Remaining hearts must be non-negative."
        }
        require(modeRules.mistakes.startingHearts != null || heartsRemaining == null) {
            "Only a heart-based mode can retain hearts."
        }
        require(
            modeRules.mistakes.startingHearts == null ||
                heartsRemaining in 0..modeRules.mistakes.startingHearts,
        ) { "Remaining hearts cannot exceed the mode maximum." }
        require((outcome == null) == (endReason == null)) {
            "Mode outcome and end reason must be set together."
        }
        require(outcome == null || phase == GamePhase.COMPLETED || phase == GamePhase.ENDED) {
            "Only a terminal phase can contain a mode outcome."
        }
        require(pendingBoardMutation == null || pendingBoardMutation.pieceId.value < pieceCount) {
            "Pending piece must be inside the puzzle bounds."
        }
        require(
            pendingBoardMutation == null ||
                pendingBoardMutation.id.sessionGeneration == sessionGeneration,
        ) { "A pending mutation must belong to the active session generation." }
        require(
            pendingBoardMutation == null ||
                pendingBoardMutation.type != BoardMutationType.REVEAL ||
                pendingBoardMutation.pieceId !in revealedPieces,
        ) { "A pending reveal cannot target an already revealed piece." }
        require(
            pendingBoardMutation == null ||
                pendingBoardMutation.type != BoardMutationType.REMOVE ||
                pendingBoardMutation.pieceId in revealedPieces,
        ) { "A pending removal must target a revealed piece." }
        val mutationPhase = phase == GamePhase.MUTATING_BOARD ||
            (phase == GamePhase.PAUSED && resumePhase == GamePhase.MUTATING_BOARD)
        require(mutationPhase == (pendingBoardMutation != null)) {
            "Only an active or paused board-mutation phase may contain a pending mutation."
        }
        require(phase == GamePhase.PAUSED || resumePhase == null) {
            "Only a paused game can retain a resume phase."
        }
    }
}

sealed interface GameAction {
    data object Start : GameAction
    data class SelectAnswer(
        val value: Int,
        val questionGeneration: Long? = null,
    ) : GameAction
    data class BoardMutationFinished(
        val mutationId: BoardMutationId,
        val sessionTimerRemainingMillis: Long? = null,
    ) : GameAction
    data class TimerExpired(val timerId: GameTimerId) : GameAction
    data object Pause : GameAction
    data object Resume : GameAction
    data object Restart : GameAction
    data object Abandon : GameAction
}

sealed interface GameEvent {
    data class CorrectAnswer(val pieceId: PieceId) : GameEvent
    data class IncorrectAnswer(val expectedAnswer: Int) : GameEvent
    data class ScoreChanged(val score: Score) : GameEvent
    data class ComboChanged(val combo: Int) : GameEvent
    data class BoardMutationStarted(val mutation: PendingBoardMutation) : GameEvent
    data class BoardMutationCommitted(val mutation: PendingBoardMutation) : GameEvent
    data class PieceRevealed(val pieceId: PieceId) : GameEvent
    data class PieceRemoved(val pieceId: PieceId) : GameEvent
    data class TimerScheduled(val timer: ActiveGameTimer) : GameEvent
    data class TimerCancelled(val timerId: GameTimerId) : GameEvent
    data class TimerAdjustmentRequested(
        val timerId: GameTimerId,
        val deltaMillis: Long,
        val maximumRemainingMillis: Long? = null,
    ) : GameEvent
    data class HeartsChanged(val heartsRemaining: Int) : GameEvent
    data class QuestionTimedOut(val questionGeneration: Long) : GameEvent
    data object NoPieceAvailableForDecay : GameEvent
    data class ModeEnded(val outcome: ModeOutcome, val reason: GameEndReason) : GameEvent
    data object PuzzleCompleted : GameEvent
}

data class Transition(
    val state: GameState,
    val events: List<GameEvent>,
) {
    companion object {
        fun unchanged(state: GameState): Transition = Transition(state, emptyList())
    }
}
