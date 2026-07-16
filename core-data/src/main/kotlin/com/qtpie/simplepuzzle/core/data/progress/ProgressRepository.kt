package com.qtpie.simplepuzzle.core.data.progress

import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.PuzzleProgress
import com.qtpie.simplepuzzle.core.model.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun interface TimeSource {
    fun nowEpochMillis(): Long
}

object SystemTimeSource : TimeSource {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

interface ProgressRepository {
    val progress: Flow<List<PuzzleProgress>>

    suspend fun startAttempt(puzzleId: PuzzleId, totalPieces: Int)

    suspend fun saveProgress(puzzleId: PuzzleId, revealedPieces: Int, totalPieces: Int, score: Score)

    suspend fun completePuzzle(summary: GameSessionSummary)

    suspend fun restore(progress: PuzzleProgress)

    suspend fun resetAll()
}

class RoomProgressRepository(
    private val dao: ProgressDao,
    private val timeSource: TimeSource = SystemTimeSource,
) : ProgressRepository {
    override val progress: Flow<List<PuzzleProgress>> = dao.observeAll().map { rows ->
        rows.map(PuzzleProgressEntity::toModel)
    }

    override suspend fun startAttempt(puzzleId: PuzzleId, totalPieces: Int) {
        dao.startAttempt(puzzleId.value, totalPieces, timeSource.nowEpochMillis())
    }

    override suspend fun saveProgress(
        puzzleId: PuzzleId,
        revealedPieces: Int,
        totalPieces: Int,
        score: Score,
    ) {
        dao.saveProgress(
            puzzleId = puzzleId.value,
            revealedPieces = revealedPieces,
            totalPieces = totalPieces,
            score = score.value,
            now = timeSource.nowEpochMillis(),
        )
    }

    override suspend fun completePuzzle(summary: GameSessionSummary) {
        dao.completePuzzle(summary)
    }

    override suspend fun restore(progress: PuzzleProgress) {
        dao.upsert(
            PuzzleProgressEntity(
                puzzleId = progress.puzzleId.value,
                revealedPieces = progress.revealedPieces,
                totalPieces = progress.totalPieces,
                completed = progress.completed,
                bestScore = progress.bestScore.value,
                attempts = progress.attempts,
                completedAtEpochMillis = progress.completedAtEpochMillis,
                lastPlayedAtEpochMillis = progress.lastPlayedAtEpochMillis,
            ),
        )
    }

    override suspend fun resetAll() {
        dao.resetAll()
    }
}
