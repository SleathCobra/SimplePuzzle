package com.qtpie.simplepuzzle.core.learning

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InitialEvidencePolicyTest {
    private val policy = InitialEvidencePolicy()
    private val skill = Grade2Quarter1Taxonomy.value.skills.first {
        it.id == Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING
    }
    private val now = 100L * DAY

    @Test
    fun policyIsDeterministicAndRequiresDiverseEvidence() {
        val attempts = listOf(attempt(1, "same", correct = true), attempt(2, "same", correct = true))

        val first = policy.derive(skill, attempts, now)
        val second = policy.derive(skill, attempts.reversed(), now)

        assertEquals(first, second)
        assertEquals(EvidenceStatus.INSUFFICIENT_EVIDENCE, first.second.status)
        assertEquals(1, first.second.distinctTemplateCount)
        assertEquals(1, first.first.count { it.direction == EvidenceDirection.EXCLUDED_DUPLICATE })
    }

    @Test
    fun fiveRecentCorrectTemplatesArePracticedRecently() {
        val attempts = (1..5).map { attempt(it, "template-$it", correct = true) }

        val result = policy.derive(skill, attempts, now).second

        assertEquals(EvidenceStatus.PRACTICED_RECENTLY, result.status)
        assertEquals(5, result.evidenceCount)
        assertFalse(result.reviewSuggested)
    }

    @Test
    fun diverseIncorrectEvidenceSuggestsReviewWithoutNegativeLabel() {
        val attempts = (1..5).map { attempt(it, "template-$it", correct = false) }

        val result = policy.derive(skill, attempts, now).second

        assertEquals(EvidenceStatus.REVIEW_SUGGESTED, result.status)
        assertTrue(result.reviewSuggested)
        assertFalse(result.status.name.contains("WEAK"))
        assertFalse(result.status.name.contains("FAILED"))
    }

    @Test
    fun responseTimeDoesNotChangeEvidenceOrStatus() {
        val fast = (1..5).map { attempt(it, "template-$it", correct = true, elapsed = 10) }
        val slow = (1..5).map { attempt(it, "template-$it", correct = true, elapsed = 600_000) }

        val fastResult = policy.derive(skill, fast, now)
        val slowResult = policy.derive(skill, slow, now)

        assertEquals(fastResult.second, slowResult.second)
        assertEquals(fastResult.first.map { it.weightBasisPoints }, slowResult.first.map { it.weightBasisPoints })
    }

    @Test
    fun assistanceRetryAndRecencyHaveTransparentWeights() {
        val unassisted = attempt(1, "one", correct = true)
        val retry = attempt(2, "two", correct = true, ordinal = 2)
        val hinted = attempt(3, "three", correct = true, assistance = AssistanceSummary(hintsUsed = 1))
        val revealed = attempt(4, "four", correct = true, assistance = AssistanceSummary(expectedAnswerRevealed = true))
        val old = attempt(5, "five", correct = true, occurredAt = now - 100L * DAY)

        val evidence = policy.derive(skill, listOf(unassisted, retry, hinted, revealed, old), now).first
            .associateBy { it.attemptId }

        assertEquals(10_000, evidence.getValue(unassisted.attemptId).weightBasisPoints)
        assertEquals(7_000, evidence.getValue(retry.attemptId).weightBasisPoints)
        assertEquals(6_000, evidence.getValue(hinted.attemptId).weightBasisPoints)
        assertEquals(2_000, evidence.getValue(revealed.attemptId).weightBasisPoints)
        assertEquals(2_500, evidence.getValue(old.attemptId).weightBasisPoints)
    }

    private fun attempt(
        number: Int,
        template: String,
        correct: Boolean,
        ordinal: Int = 1,
        assistance: AssistanceSummary = AssistanceSummary(),
        occurredAt: Long = now - DAY,
        elapsed: Long? = null,
    ): LearningAttempt {
        val expected = IntegerResponse(12)
        return LearningAttempt(
            attemptId = AttemptId("attempt-$number-test"),
            sessionId = SessionId("session-test-001"),
            activityId = ActivityId("jm.activity.jigsaw-math"),
            activityVersion = ActivityVersion(1),
            item = LearningItemRef(
                itemId = LearningItemId("jm.item.test.$number"),
                templateId = template,
                activityId = ActivityId("jm.activity.jigsaw-math"),
                activityVersion = ActivityVersion(1),
                contentVersion = ContentVersion(1),
                primarySkillId = skill.id,
                authoredDifficulty = AuthoredDifficultyBand.CORE,
                representation = RepresentationType.SYMBOLIC_EQUATION,
                provenance = GeneratorProvenance("test-generator", 1, number.toLong(), mapOf("case" to number.toString())),
            ),
            selectedResponse = if (correct) expected else IntegerResponse(11),
            expectedResponse = expected,
            outcome = if (correct) AttemptOutcome.CORRECT else AttemptOutcome.INCORRECT,
            assistance = assistance,
            attemptOrdinal = ordinal,
            occurredAtEpochMillis = occurredAt,
            elapsedMillis = elapsed,
            applicationProvenance = ApplicationProvenance("test", 1),
        )
    }

    private companion object {
        const val DAY = 24L * 60L * 60L * 1_000L
    }
}
