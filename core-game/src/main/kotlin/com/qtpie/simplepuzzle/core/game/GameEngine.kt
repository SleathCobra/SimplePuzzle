package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.GameAction
import com.qtpie.simplepuzzle.core.model.GameConfiguration
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GamePhase
import com.qtpie.simplepuzzle.core.model.GameState
import com.qtpie.simplepuzzle.core.model.PieceId
import com.qtpie.simplepuzzle.core.model.PieceSet
import com.qtpie.simplepuzzle.core.model.Score
import com.qtpie.simplepuzzle.core.model.Transition

fun interface ScoringPolicy {
    fun pointsFor(combo: Int): Int
}

object StandardScoringPolicy : ScoringPolicy {
    override fun pointsFor(combo: Int): Int {
        require(combo >= 1) { "Combo must be at least one." }
        return 10 * combo
    }
}

interface GameEngine {
    fun newGame(configuration: GameConfiguration): GameState

    fun reduce(state: GameState, action: GameAction): Transition
}

class DefaultGameEngine(
    private val random: RandomSource,
    private val questionGenerator: QuestionGenerator = DefaultMathQuestionGenerator(),
    private val scoringPolicy: ScoringPolicy = StandardScoringPolicy,
) : GameEngine {
    override fun newGame(configuration: GameConfiguration): GameState = GameState(
        puzzleId = configuration.puzzleId,
        revealedPieces = PieceSet.Empty,
        pieceCount = configuration.pieceCount,
        score = Score(0),
        combo = 1,
        question = questionGenerator.generate(configuration.difficulty, random),
        difficulty = configuration.difficulty,
        phase = GamePhase.READY,
    )

    override fun reduce(state: GameState, action: GameAction): Transition = when (action) {
        GameAction.Start -> start(state)
        is GameAction.SelectAnswer -> selectAnswer(state, action.value)
        GameAction.RevealAnimationFinished -> finishReveal(state)
        GameAction.Pause -> pause(state)
        GameAction.Resume -> resume(state)
        GameAction.Restart -> restart(state)
    }

    private fun start(state: GameState): Transition {
        if (state.phase != GamePhase.READY) return Transition.unchanged(state)
        return Transition(state.copy(phase = GamePhase.AWAITING_ANSWER), emptyList())
    }

    private fun selectAnswer(state: GameState, answer: Int): Transition {
        if (state.phase != GamePhase.AWAITING_ANSWER) return Transition.unchanged(state)

        if (answer != state.question.answer) {
            val events = buildList {
                add(GameEvent.IncorrectAnswer(state.question.answer))
                if (state.combo != 1) add(GameEvent.ComboChanged(1))
            }
            return Transition(state.copy(combo = 1), events)
        }

        val hiddenPieces = state.revealedPieces.hiddenIndices(state.pieceCount)
        if (hiddenPieces.isEmpty()) {
            return Transition(
                state.copy(phase = GamePhase.COMPLETED),
                listOf(GameEvent.PuzzleCompleted),
            )
        }

        val piece = PieceId(hiddenPieces[random.nextInt(0, hiddenPieces.size)])
        val nextScore = Score(state.score.value + scoringPolicy.pointsFor(state.combo))
        val nextCombo = state.combo + 1
        return Transition(
            state = state.copy(
                score = nextScore,
                combo = nextCombo,
                phase = GamePhase.REVEALING_PIECE,
                pendingPiece = piece,
            ),
            events = listOf(
                GameEvent.CorrectAnswer(piece),
                GameEvent.ScoreChanged(nextScore),
                GameEvent.ComboChanged(nextCombo),
            ),
        )
    }

    private fun finishReveal(state: GameState): Transition {
        val piece = state.pendingPiece
        if (state.phase != GamePhase.REVEALING_PIECE || piece == null) {
            return Transition.unchanged(state)
        }

        val revealed = state.revealedPieces.reveal(piece, state.pieceCount)
        val isComplete = revealed.size == state.pieceCount
        val nextState = if (isComplete) {
            state.copy(
                revealedPieces = revealed,
                pendingPiece = null,
                phase = GamePhase.COMPLETED,
            )
        } else {
            state.copy(
                revealedPieces = revealed,
                pendingPiece = null,
                question = questionGenerator.generate(state.difficulty, random),
                phase = GamePhase.AWAITING_ANSWER,
            )
        }
        val events = buildList {
            add(GameEvent.PieceRevealed(piece))
            if (isComplete) add(GameEvent.PuzzleCompleted)
        }
        return Transition(nextState, events)
    }

    private fun pause(state: GameState): Transition {
        if (state.phase != GamePhase.AWAITING_ANSWER && state.phase != GamePhase.REVEALING_PIECE) {
            return Transition.unchanged(state)
        }
        return Transition(
            state.copy(phase = GamePhase.PAUSED, resumePhase = state.phase),
            emptyList(),
        )
    }

    private fun resume(state: GameState): Transition {
        if (state.phase != GamePhase.PAUSED) {
            return Transition.unchanged(state)
        }
        val resumePhase = state.resumePhase ?: return Transition.unchanged(state)
        return Transition(
            state.copy(phase = resumePhase, resumePhase = null),
            emptyList(),
        )
    }

    private fun restart(state: GameState): Transition = Transition(
        state = state.copy(
            revealedPieces = PieceSet.Empty,
            score = Score(0),
            combo = 1,
            question = questionGenerator.generate(state.difficulty, random),
            phase = GamePhase.AWAITING_ANSWER,
            pendingPiece = null,
            resumePhase = null,
        ),
        events = emptyList(),
    )
}
