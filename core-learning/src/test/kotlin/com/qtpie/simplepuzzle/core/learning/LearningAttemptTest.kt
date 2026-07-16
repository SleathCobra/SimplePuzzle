package com.qtpie.simplepuzzle.core.learning

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Test

class LearningAttemptTest {
    @Test
    fun attemptRoundTripsWithVersionedItemAndAssistance() {
        val attempt = attempt(
            outcome = AttemptOutcome.CORRECT,
            selected = 12,
            expected = 12,
            ordinal = 2,
            assistance = AssistanceSummary(hintsUsed = 1),
        )

        val encoded = Json.encodeToString(LearningAttempt.serializer(), attempt)
        val decoded = Json.decodeFromString(LearningAttempt.serializer(), encoded)

        assertEquals(attempt, decoded)
        assertEquals(2, decoded.attemptOrdinal)
        assertFalse(decoded.assistance.isUnassisted)
        assertTrue(decoded.item.provenance.configuration.isNotEmpty())
    }

    @Test
    fun outcomeMustAgreeWithStructuredResponses() {
        assertThrows(IllegalArgumentException::class.java) {
            attempt(AttemptOutcome.CORRECT, selected = 11, expected = 12)
        }
        assertThrows(IllegalArgumentException::class.java) {
            attempt(AttemptOutcome.INCORRECT, selected = 12, expected = 12)
        }
    }

    @Test
    fun invalidationIsAppendOnlyAndMustNameSupersededAttempt() {
        assertThrows(IllegalArgumentException::class.java) {
            attempt(AttemptOutcome.INVALIDATED, 11, 12)
        }
        val invalidation = attempt(
            outcome = AttemptOutcome.INVALIDATED,
            selected = 11,
            expected = 12,
            supersedes = AttemptId("attempt-original"),
            reason = "content_defect",
        )

        assertEquals(AttemptId("attempt-original"), invalidation.supersedesAttemptId)
        assertEquals("content_defect", invalidation.invalidationReasonCode)
    }

    @Test
    fun misconceptionTagsMustBeExplicitAndReviewed() {
        val item = item().copy(reviewedMisconceptionByResponse = emptyMap())

        assertTrue(item.reviewedMisconceptionByResponse.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {
            item.copy(reviewedMisconceptionByResponse = mapOf("11" to ""))
        }
    }

    private fun attempt(
        outcome: AttemptOutcome,
        selected: Int,
        expected: Int,
        ordinal: Int = 1,
        assistance: AssistanceSummary = AssistanceSummary(),
        supersedes: AttemptId? = null,
        reason: String? = null,
    ) = LearningAttempt(
        attemptId = AttemptId("attempt-test-001"),
        sessionId = SessionId("session-test-001"),
        activityId = ActivityId("jm.activity.jigsaw-math"),
        activityVersion = ActivityVersion(1),
        item = item().ref,
        selectedResponse = IntegerResponse(selected),
        expectedResponse = IntegerResponse(expected),
        outcome = outcome,
        assistance = assistance,
        attemptOrdinal = ordinal,
        occurredAtEpochMillis = 1_000,
        elapsedMillis = 500,
        applicationProvenance = ApplicationProvenance("1.0", 1),
        supersedesAttemptId = supersedes,
        invalidationReasonCode = reason,
    )

    private fun item() = LearningItem(
        ref = LearningItemRef(
            itemId = LearningItemId("jm.item.test.001"),
            templateId = "jm.template.test",
            activityId = ActivityId("jm.activity.jigsaw-math"),
            activityVersion = ActivityVersion(1),
            contentVersion = ContentVersion(1),
            primarySkillId = Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING,
            authoredDifficulty = AuthoredDifficultyBand.CORE,
            representation = RepresentationType.SYMBOLIC_EQUATION,
            provenance = GeneratorProvenance("jigsaw-addition", 1, 42, mapOf("difficulty" to "MEDIUM")),
        ),
        expectedResponse = IntegerResponse(12),
    )
}
