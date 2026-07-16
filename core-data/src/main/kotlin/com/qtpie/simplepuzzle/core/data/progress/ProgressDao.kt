package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.GameModeId
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProgressDao {
    @Query("SELECT * FROM puzzle_progress ORDER BY puzzleId")
    abstract fun observeAll(): Flow<List<PuzzleProgressEntity>>

    @Query("SELECT * FROM puzzle_progress WHERE puzzleId = :puzzleId")
    abstract suspend fun get(puzzleId: String): PuzzleProgressEntity?

    @Upsert
    abstract suspend fun upsert(progress: PuzzleProgressEntity)

    @Insert
    abstract suspend fun insertSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions ORDER BY id")
    abstract suspend fun getSessions(): List<GameSessionEntity>

    @Query("SELECT * FROM mode_bests WHERE puzzleId = :puzzleId AND modeId = :modeId")
    abstract suspend fun getModeBest(puzzleId: String, modeId: String): ModeBestEntity?

    @Query("SELECT * FROM mode_bests ORDER BY puzzleId, modeId")
    abstract suspend fun getModeBests(): List<ModeBestEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertModeBest(best: ModeBestEntity)

    @Query("SELECT COUNT(*) FROM game_sessions")
    abstract suspend fun sessionCount(): Int

    @Query("DELETE FROM game_sessions")
    protected abstract suspend fun deleteSessions()

    @Query("DELETE FROM mode_bests")
    protected abstract suspend fun deleteModeBests()

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
                bestScore = if (summary.modeId == GameModeId.CLASSIC) {
                    maxOf(current.bestScore, summary.score.value)
                } else {
                    current.bestScore
                },
                completedAtEpochMillis = current.completedAtEpochMillis ?: summary.endedAtEpochMillis,
                lastPlayedAtEpochMillis = summary.endedAtEpochMillis,
            ),
        )
        insertSession(summary.toEntity())
        updateModeBest(summary)
    }

    @Transaction
    open suspend fun recordSession(summary: GameSessionSummary) {
        requireNotNull(get(summary.puzzleId.value)) {
            "Puzzle attempt must be started before recording a session."
        }
        insertSession(summary.toEntity())
        if (summary.completed) updateModeBest(summary)
    }

    private suspend fun updateModeBest(summary: GameSessionSummary) {
        if (!summary.completed) return
        val modeId = summary.modeId.persistedValue
        val current = getModeBest(summary.puzzleId.value, modeId)
        val mistakes = summary.incorrectAnswers + summary.timeoutCount
        upsertModeBest(
            ModeBestEntity(
                puzzleId = summary.puzzleId.value,
                modeId = modeId,
                bestScore = maxOf(current?.bestScore ?: 0, summary.score.value),
                fastestCompletionMillis = current?.fastestCompletionMillis
                    ?.let { minOf(it, summary.durationMillis) }
                    ?: summary.durationMillis,
                highestCombo = maxOf(current?.highestCombo ?: 1, summary.maximumCombo),
                fewestMistakes = current?.fewestMistakes
                    ?.let { minOf(it, mistakes) }
                    ?: mistakes,
                mostRecentCompletionEpochMillis = maxOf(
                    current?.mostRecentCompletionEpochMillis ?: Long.MIN_VALUE,
                    summary.endedAtEpochMillis,
                ),
            ),
        )
    }

    @Transaction
    open suspend fun resetAll() {
        deleteModeBests()
        deleteSessions()
        deleteProgress()
    }
}
