package com.qtpie.simplepuzzle.core.model

/** Stable local/persistence identifier. Stored values must not depend on enum ordinals. */
enum class GameModeId(val persistedValue: String) {
    CLASSIC("classic"),
    TIME_ATTACK("time-attack"),
    SURVIVAL("survival"),
    PUZZLE_DECAY("puzzle-decay"),
    COMBO_RUSH("combo-rush"),
}

data class DifficultyDurations(
    val easyMillis: Long,
    val mediumMillis: Long,
    val hardMillis: Long,
) {
    init {
        require(easyMillis > 0 && mediumMillis > 0 && hardMillis > 0) {
            "Difficulty timer durations must be positive."
        }
    }

    fun forDifficulty(difficulty: Difficulty): Long = when (difficulty) {
        Difficulty.EASY -> easyMillis
        Difficulty.MEDIUM -> mediumMillis
        Difficulty.HARD -> hardMillis
    }
}

data class ClockPolicy(
    val initialSessionMillis: Long? = null,
    val questionDeadlineMillis: DifficultyDurations? = null,
    val decayIntervalMillis: Long? = null,
    val maximumSessionMillis: Long? = null,
    val correctAnswerBonusMillis: Long = 0,
    val wrongAnswerPenaltyMillis: Long = 0,
    val maximumRemainingMillis: Long? = null,
) {
    init {
        require(initialSessionMillis == null || initialSessionMillis > 0) {
            "Initial session duration must be positive."
        }
        require(decayIntervalMillis == null || decayIntervalMillis > 0) {
            "Decay interval must be positive."
        }
        require(maximumSessionMillis == null || maximumSessionMillis > 0) {
            "Maximum session duration must be positive."
        }
        require(correctAnswerBonusMillis >= 0 && wrongAnswerPenaltyMillis >= 0) {
            "Timer bonuses and penalties must be non-negative."
        }
        require(maximumRemainingMillis == null || maximumRemainingMillis > 0) {
            "Maximum remaining duration must be positive."
        }
        require(
            maximumRemainingMillis == null ||
                initialSessionMillis == null ||
                maximumRemainingMillis >= initialSessionMillis,
        ) { "Maximum remaining duration cannot be below the initial duration." }
    }

    val isUntimed: Boolean
        get() = initialSessionMillis == null &&
            questionDeadlineMillis == null &&
            decayIntervalMillis == null &&
            maximumSessionMillis == null
}

data class MistakePolicy(
    val startingHearts: Int? = null,
    val wrongAnswerHeartCost: Int = 0,
    val questionTimeoutHeartCost: Int = 0,
    val resetComboOnWrongAnswer: Boolean = true,
) {
    init {
        require(startingHearts == null || startingHearts > 0) {
            "Starting hearts must be positive."
        }
        require(wrongAnswerHeartCost >= 0 && questionTimeoutHeartCost >= 0) {
            "Heart costs must be non-negative."
        }
        require(startingHearts != null || (wrongAnswerHeartCost == 0 && questionTimeoutHeartCost == 0)) {
            "A mode cannot spend hearts without a starting heart count."
        }
    }
}

data class PiecePenaltyPolicy(
    val removeRevealedPieceOnWrongAnswer: Boolean = false,
    val removeRevealedPieceOnDecayTimeout: Boolean = false,
)

data class CompletionPolicy(
    val completeWhenAllPiecesCommitted: Boolean = true,
    val failWhenSessionTimerExpires: Boolean = false,
    val failWhenQuestionHeartsReachZero: Boolean = false,
    val failWhenMaximumSessionExpires: Boolean = false,
)

data class ScoreModifierPolicy(
    val remainingTimeBonusPointsPerSecond: Int = 0,
) {
    init {
        require(remainingTimeBonusPointsPerSecond >= 0) {
            "Remaining-time bonus must be non-negative."
        }
    }
}

data class GameModeDefinition(
    val id: GameModeId,
    val visibleName: String,
    val summary: String,
    val clock: ClockPolicy,
    val mistakes: MistakePolicy,
    val piecePenalty: PiecePenaltyPolicy,
    val completion: CompletionPolicy,
    val scoreModifier: ScoreModifierPolicy = ScoreModifierPolicy(),
    val recommended: Boolean = false,
) {
    init {
        require(visibleName.isNotBlank()) { "Mode visible name cannot be blank." }
        require(summary.isNotBlank()) { "Mode summary cannot be blank." }
    }
}

/**
 * Authoritative local mode catalog and tuning source.
 *
 * Persisted unknown values deliberately fall back to Classic without rewriting the unknown row.
 */
object GameModeCatalog {
    val Classic = GameModeDefinition(
        id = GameModeId.CLASSIC,
        visibleName = "Classic",
        summary = "Solve at your own pace.",
        clock = ClockPolicy(),
        mistakes = MistakePolicy(),
        piecePenalty = PiecePenaltyPolicy(),
        completion = CompletionPolicy(),
        recommended = true,
    )

    val TimeAttack = GameModeDefinition(
        id = GameModeId.TIME_ATTACK,
        visibleName = "Time Attack",
        summary = "Complete the puzzle before time runs out.",
        clock = ClockPolicy(initialSessionMillis = 90_000),
        mistakes = MistakePolicy(),
        piecePenalty = PiecePenaltyPolicy(),
        completion = CompletionPolicy(failWhenSessionTimerExpires = true),
        scoreModifier = ScoreModifierPolicy(remainingTimeBonusPointsPerSecond = 2),
    )

    val Survival = GameModeDefinition(
        id = GameModeId.SURVIVAL,
        visibleName = "Survival",
        summary = "Three hearts. Wrong or late answers cost one.",
        clock = ClockPolicy(
            questionDeadlineMillis = DifficultyDurations(
                easyMillis = 10_000,
                mediumMillis = 8_000,
                hardMillis = 6_000,
            ),
        ),
        mistakes = MistakePolicy(
            startingHearts = 3,
            wrongAnswerHeartCost = 1,
            questionTimeoutHeartCost = 1,
        ),
        piecePenalty = PiecePenaltyPolicy(),
        completion = CompletionPolicy(failWhenQuestionHeartsReachZero = true),
    )

    val PuzzleDecay = GameModeDefinition(
        id = GameModeId.PUZZLE_DECAY,
        visibleName = "Puzzle Decay",
        summary = "Keep solving before revealed pieces disappear.",
        clock = ClockPolicy(
            decayIntervalMillis = 9_000,
            maximumSessionMillis = 180_000,
        ),
        mistakes = MistakePolicy(),
        piecePenalty = PiecePenaltyPolicy(
            removeRevealedPieceOnWrongAnswer = true,
            removeRevealedPieceOnDecayTimeout = true,
        ),
        completion = CompletionPolicy(failWhenMaximumSessionExpires = true),
    )

    val ComboRush = GameModeDefinition(
        id = GameModeId.COMBO_RUSH,
        visibleName = "Combo Rush",
        summary = "Correct answers add time. Build a huge combo.",
        clock = ClockPolicy(
            initialSessionMillis = 45_000,
            correctAnswerBonusMillis = 2_500,
            wrongAnswerPenaltyMillis = 4_000,
            maximumRemainingMillis = 60_000,
        ),
        mistakes = MistakePolicy(),
        piecePenalty = PiecePenaltyPolicy(),
        completion = CompletionPolicy(failWhenSessionTimerExpires = true),
    )

    val all: List<GameModeDefinition> = listOf(
        Classic,
        TimeAttack,
        Survival,
        PuzzleDecay,
        ComboRush,
    ).also { definitions ->
        require(definitions.map(GameModeDefinition::id).distinct().size == definitions.size) {
            "Game mode IDs must be unique."
        }
    }

    private val byId = all.associateBy(GameModeDefinition::id)
    private val byPersistedValue = all.associateBy { it.id.persistedValue }

    fun definition(id: GameModeId): GameModeDefinition = requireNotNull(byId[id])

    fun idOrClassic(persistedValue: String?): GameModeId =
        byPersistedValue[persistedValue]?.id ?: GameModeId.CLASSIC

    fun definitionOrClassic(persistedValue: String?): GameModeDefinition =
        byPersistedValue[persistedValue] ?: Classic
}
