package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.PuzzleProgress
import com.qtpie.simplepuzzle.core.model.Score

@Entity(tableName = "puzzle_progress")
data class PuzzleProgressEntity(
    @PrimaryKey val puzzleId: String,
    val revealedPieces: Int,
    val totalPieces: Int,
    val completed: Boolean,
    val bestScore: Int,
    val attempts: Int,
    val completedAtEpochMillis: Long?,
    val lastPlayedAtEpochMillis: Long,
)

@Entity(
    tableName = "game_sessions",
    foreignKeys = [
        ForeignKey(
            entity = PuzzleProgressEntity::class,
            parentColumns = ["puzzleId"],
            childColumns = ["puzzleId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("puzzleId")],
)
data class GameSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val puzzleId: String,
    val score: Int,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val durationMillis: Long,
    val completed: Boolean,
    val endedAtEpochMillis: Long,
)

internal fun PuzzleProgressEntity.toModel() = PuzzleProgress(
    puzzleId = PuzzleId(puzzleId),
    revealedPieces = revealedPieces,
    totalPieces = totalPieces,
    completed = completed,
    bestScore = Score(bestScore),
    attempts = attempts,
    completedAtEpochMillis = completedAtEpochMillis,
    lastPlayedAtEpochMillis = lastPlayedAtEpochMillis,
)

internal fun GameSessionSummary.toEntity() = GameSessionEntity(
    puzzleId = puzzleId.value,
    score = score.value,
    correctAnswers = correctAnswers,
    incorrectAnswers = incorrectAnswers,
    durationMillis = durationMillis,
    completed = completed,
    endedAtEpochMillis = endedAtEpochMillis,
)
