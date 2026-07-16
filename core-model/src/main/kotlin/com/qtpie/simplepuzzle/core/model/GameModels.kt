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
    val provenance: MathQuestionProvenance? = null,
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

data class MathQuestionProvenance(
    val generatorId: String,
    val generatorVersion: Int,
    val contentVersion: Int,
    val seed: Long,
    val configuration: Map<String, String>,
) {
    init {
        require(generatorId.isNotBlank()) { "Question generator ID cannot be blank." }
        require(generatorVersion > 0) { "Question generator version must be positive." }
        require(contentVersion > 0) { "Question content version must be positive." }
        require(configuration.keys.none(String::isBlank)) {
            "Question generator configuration keys cannot be blank."
        }
    }
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
    REVEALING_PIECE,
    PAUSED,
    COMPLETED,
}

data class GameConfiguration(
    val puzzleId: PuzzleId,
    val pieceCount: Int,
    val difficulty: Difficulty,
) {
    init {
        require(pieceCount > 0) { "A puzzle must contain at least one piece." }
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
    val phase: GamePhase,
    val pendingPiece: PieceId? = null,
    val resumePhase: GamePhase? = null,
) {
    init {
        require(pieceCount > 0) { "A puzzle must contain at least one piece." }
        require(combo >= 1) { "Combo must be at least one." }
        require(revealedPieces.asIndices().all { it in 0 until pieceCount }) {
            "Revealed pieces must be inside the puzzle bounds."
        }
        require(pendingPiece == null || pendingPiece.value < pieceCount) {
            "Pending piece must be inside the puzzle bounds."
        }
        require(pendingPiece == null || pendingPiece !in revealedPieces) {
            "A pending piece cannot already be revealed."
        }
        require(phase == GamePhase.PAUSED || resumePhase == null) {
            "Only a paused game can retain a resume phase."
        }
    }
}

sealed interface GameAction {
    data object Start : GameAction
    data class SelectAnswer(val value: Int) : GameAction
    data object RevealAnimationFinished : GameAction
    data object Pause : GameAction
    data object Resume : GameAction
    data object Restart : GameAction
}

sealed interface GameEvent {
    data class CorrectAnswer(val pieceId: PieceId) : GameEvent
    data class IncorrectAnswer(val expectedAnswer: Int) : GameEvent
    data class ScoreChanged(val score: Score) : GameEvent
    data class ComboChanged(val combo: Int) : GameEvent
    data class PieceRevealed(val pieceId: PieceId) : GameEvent
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
