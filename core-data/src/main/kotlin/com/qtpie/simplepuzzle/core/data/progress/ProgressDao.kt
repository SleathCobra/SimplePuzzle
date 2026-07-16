package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProgressDao {
    @Query("SELECT * FROM puzzle_progress ORDER BY puzzleId")
    abstract fun observeAll(): Flow<List<PuzzleProgressEntity>>

    @Query("SELECT * FROM puzzle_progress WHERE puzzleId = :puzzleId")
    abstract suspend fun get(puzzleId: String): PuzzleProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsert(progress: PuzzleProgressEntity)

    @Insert
    abstract suspend fun insertSession(session: GameSessionEntity)

    @Query("SELECT COUNT(*) FROM game_sessions")
    abstract suspend fun sessionCount(): Int

    @Query("DELETE FROM game_sessions")
    protected abstract suspend fun deleteSessions()

    @Query("DELETE FROM puzzle_progress")
    protected abstract suspend fun deleteProgress()

    @Transaction
    open suspend fun startAttempt(puzzleId: String, totalPieces: Int, now: Long) {
        val current = get(puzzleId)
        upsert(
            current?.copy(
                totalPieces = totalPieces,
                attempts = current.attempts + 1,
                lastPlayedAtEpochMillis = now,
            ) ?: PuzzleProgressEntity(
                puzzleId = puzzleId,
                revealedPieces = 0,
                totalPieces = totalPieces,
                completed = false,
                bestScore = 0,
                attempts = 1,
                completedAtEpochMillis = null,
                lastPlayedAtEpochMillis = now,
            ),
        )
    }

    @Transaction
    open suspend fun saveProgress(
        puzzleId: String,
        revealedPieces: Int,
        totalPieces: Int,
        score: Int,
        now: Long,
    ) {
        val current = get(puzzleId)
        upsert(
            (current ?: PuzzleProgressEntity(
                puzzleId = puzzleId,
                revealedPieces = 0,
                totalPieces = totalPieces,
                completed = false,
                bestScore = 0,
                attempts = 0,
                completedAtEpochMillis = null,
                lastPlayedAtEpochMillis = now,
            )).copy(
                revealedPieces = revealedPieces,
                totalPieces = totalPieces,
                bestScore = maxOf(current?.bestScore ?: 0, score),
                lastPlayedAtEpochMillis = now,
            ),
        )
    }

    @Transaction
    open suspend fun completePuzzle(summary: GameSessionSummary) {
        val puzzleId = summary.puzzleId.value
        val current = requireNotNull(get(puzzleId)) {
            "Puzzle attempt must be started before completion."
        }
        upsert(
            current.copy(
                revealedPieces = current.totalPieces,
                completed = true,
                bestScore = maxOf(current.bestScore, summary.score.value),
                completedAtEpochMillis = current.completedAtEpochMillis ?: summary.endedAtEpochMillis,
                lastPlayedAtEpochMillis = summary.endedAtEpochMillis,
            ),
        )
        insertSession(summary.toEntity())
    }

    @Transaction
    open suspend fun resetAll() {
        deleteSessions()
        deleteProgress()
    }
}
