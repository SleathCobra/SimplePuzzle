package com.qtpie.simplepuzzle.core.data.learning

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.qtpie.simplepuzzle.core.learning.AttemptOutcome
import kotlinx.coroutines.flow.Flow

@Dao
abstract class LearningDao {
    @Query("SELECT * FROM learning_attempts ORDER BY occurredAtEpochMillis, attemptId")
    abstract fun observeAttempts(): Flow<List<LearningAttemptEntity>>

    @Query("SELECT * FROM learning_attempts ORDER BY occurredAtEpochMillis, attemptId")
    abstract suspend fun getAttempts(): List<LearningAttemptEntity>

    @Query("SELECT * FROM learning_attempts WHERE attemptId = :attemptId")
    abstract suspend fun getAttempt(attemptId: String): LearningAttemptEntity?

    @Query("SELECT COUNT(*) FROM learning_attempts")
    abstract suspend fun attemptCount(): Int

    @Query("SELECT * FROM learning_sessions WHERE sessionId = :sessionId")
    abstract suspend fun getSession(sessionId: String): LearningSessionEntity?

    @Query("SELECT COUNT(*) FROM learning_sessions")
    abstract suspend fun sessionCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertAttempt(entity: LearningAttemptEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun upsertSession(entity: LearningSessionEntity)

    @Transaction
    open suspend fun recordAttempt(entity: LearningAttemptEntity, outcome: AttemptOutcome): Boolean {
        if (insertAttempt(entity) == -1L) return false

        val current = getSession(entity.sessionId)
        require(current == null || (
            current.activityId == entity.activityId && current.activityVersion == entity.activityVersion
        )) {
            "A learning session cannot change activity identity or version."
        }
        val isScoredAttempt = outcome != AttemptOutcome.INVALIDATED
        val initial = LearningSessionEntity(
            sessionId = entity.sessionId,
            activityId = entity.activityId,
            activityVersion = entity.activityVersion,
            startedAtEpochMillis = entity.occurredAtEpochMillis,
            lastAttemptAtEpochMillis = entity.occurredAtEpochMillis,
            attemptCount = 0,
            correctCount = 0,
            incorrectCount = 0,
        )
        upsertSession(
            (current ?: initial).copy(
                startedAtEpochMillis = minOf(
                    current?.startedAtEpochMillis ?: entity.occurredAtEpochMillis,
                    entity.occurredAtEpochMillis,
                ),
                lastAttemptAtEpochMillis = maxOf(
                    current?.lastAttemptAtEpochMillis ?: entity.occurredAtEpochMillis,
                    entity.occurredAtEpochMillis,
                ),
                attemptCount = (current?.attemptCount ?: 0) + if (isScoredAttempt) 1 else 0,
                correctCount = (current?.correctCount ?: 0) + if (outcome == AttemptOutcome.CORRECT) 1 else 0,
                incorrectCount = (current?.incorrectCount ?: 0) + if (outcome == AttemptOutcome.INCORRECT) 1 else 0,
            ),
        )
        return true
    }
}
