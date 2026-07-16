package com.qtpie.simplepuzzle.core.data.learning

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.qtpie.simplepuzzle.core.learning.LearningAttempt
import kotlinx.serialization.json.Json

@Entity(
    tableName = "learning_attempts",
    indices = [
        Index("sessionId"),
        Index("skillId"),
        Index("templateId"),
        Index("occurredAtEpochMillis"),
    ],
)
data class LearningAttemptEntity(
    @PrimaryKey val attemptId: String,
    val sessionId: String,
    val activityId: String,
    val activityVersion: Int,
    val skillId: String,
    val templateId: String,
    val occurredAtEpochMillis: Long,
    val supersedesAttemptId: String?,
    val payloadJson: String,
)

@Entity(tableName = "learning_sessions")
data class LearningSessionEntity(
    @PrimaryKey val sessionId: String,
    val activityId: String,
    val activityVersion: Int,
    val startedAtEpochMillis: Long,
    val lastAttemptAtEpochMillis: Long,
    val attemptCount: Int,
    val correctCount: Int,
    val incorrectCount: Int,
)

internal val LearningJson = Json {
    encodeDefaults = true
    explicitNulls = true
    ignoreUnknownKeys = true
    classDiscriminator = "responseType"
}

internal fun LearningAttempt.toEntity(json: Json = LearningJson) = LearningAttemptEntity(
    attemptId = attemptId.value,
    sessionId = sessionId.value,
    activityId = activityId.value,
    activityVersion = activityVersion.value,
    skillId = item.primarySkillId.value,
    templateId = item.templateId,
    occurredAtEpochMillis = occurredAtEpochMillis,
    supersedesAttemptId = supersedesAttemptId?.value,
    payloadJson = json.encodeToString(LearningAttempt.serializer(), this),
)

internal fun LearningAttemptEntity.toModel(json: Json = LearningJson): LearningAttempt =
    json.decodeFromString(LearningAttempt.serializer(), payloadJson)
