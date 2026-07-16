package com.qtpie.simplepuzzle.core.data.learning

import com.qtpie.simplepuzzle.core.learning.ActivityId
import com.qtpie.simplepuzzle.core.learning.ActivityVersion
import com.qtpie.simplepuzzle.core.learning.ApplicationProvenance
import com.qtpie.simplepuzzle.core.learning.AssistanceSummary
import com.qtpie.simplepuzzle.core.learning.AttemptId
import com.qtpie.simplepuzzle.core.learning.AttemptOutcome
import com.qtpie.simplepuzzle.core.learning.AuthoredDifficultyBand
import com.qtpie.simplepuzzle.core.learning.ContentVersion
import com.qtpie.simplepuzzle.core.learning.GeneratorProvenance
import com.qtpie.simplepuzzle.core.learning.Grade2Quarter1Skills
import com.qtpie.simplepuzzle.core.learning.IntegerResponse
import com.qtpie.simplepuzzle.core.learning.LearningAttempt
import com.qtpie.simplepuzzle.core.learning.LearningItemId
import com.qtpie.simplepuzzle.core.learning.LearningItemRef
import com.qtpie.simplepuzzle.core.learning.RepresentationType
import com.qtpie.simplepuzzle.core.learning.SessionId
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningEntityMappingTest {
    @Test
    fun entityRoundTripPreservesEveryRawAttemptField() {
        val attempt = attempt()

        val entity = attempt.toEntity()
        val decoded = entity.toModel()

        assertEquals(attempt, decoded)
        assertEquals(attempt.item.primarySkillId.value, entity.skillId)
        assertEquals(attempt.item.templateId, entity.templateId)
        assertEquals(attempt.sessionId.value, entity.sessionId)
    }

    private fun attempt() = LearningAttempt(
        attemptId = AttemptId("attempt-mapping-001"),
        sessionId = SessionId("session-mapping-001"),
        activityId = ActivityId("jm.activity.jigsaw-math"),
        activityVersion = ActivityVersion(1),
        item = LearningItemRef(
            itemId = LearningItemId("jm.item.mapping.001"),
            templateId = "jm.template.mapping",
            activityId = ActivityId("jm.activity.jigsaw-math"),
            activityVersion = ActivityVersion(1),
            contentVersion = ContentVersion(1),
            primarySkillId = Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING,
            authoredDifficulty = AuthoredDifficultyBand.CORE,
            representation = RepresentationType.SYMBOLIC_EQUATION,
            provenance = GeneratorProvenance("test-generator", 1, 44, mapOf("difficulty" to "MEDIUM")),
        ),
        selectedResponse = IntegerResponse(9),
        expectedResponse = IntegerResponse(9),
        outcome = AttemptOutcome.CORRECT,
        assistance = AssistanceSummary(),
        attemptOrdinal = 1,
        occurredAtEpochMillis = 123,
        elapsedMillis = null,
        applicationProvenance = ApplicationProvenance("test", 1),
    )
}
