package com.qtpie.simplepuzzle.core.learning

import kotlin.math.max

interface EvidencePolicy {
    val version: EvidencePolicyVersion

    fun derive(
        skill: SkillDefinition,
        attempts: List<LearningAttempt>,
        asOfEpochMillis: Long,
    ): Pair<List<SkillEvidence>, PersonalSkillSummary>
}

class InitialEvidencePolicy : EvidencePolicy {
    override val version = EvidencePolicyVersion(1)

    override fun derive(
        skill: SkillDefinition,
        attempts: List<LearningAttempt>,
        asOfEpochMillis: Long,
    ): Pair<List<SkillEvidence>, PersonalSkillSummary> {
        require(asOfEpochMillis >= 0) { "Evidence as-of time cannot be negative." }
        val relevant = attempts
            .filter { attempt ->
                attempt.item.primarySkillId == skill.id || skill.id in attempt.item.secondarySkillIds
            }
            .sortedWith(compareBy<LearningAttempt> { it.occurredAtEpochMillis }.thenBy { it.attemptId.value })

        val invalidatedIds = relevant
            .filter { it.outcome == AttemptOutcome.INVALIDATED }
            .mapNotNullTo(mutableSetOf(), LearningAttempt::supersedesAttemptId)
        val active = relevant.filter { it.outcome != AttemptOutcome.INVALIDATED && it.attemptId !in invalidatedIds }
        val latestByTemplate = active.groupBy { it.item.templateId }.mapValues { (_, values) -> values.last() }

        val evidence = relevant.map { attempt ->
            when {
                attempt.outcome == AttemptOutcome.INVALIDATED || attempt.attemptId in invalidatedIds ->
                    SkillEvidence(attempt.attemptId, skill.id, version, 0, EvidenceDirection.INVALIDATED, "invalidated")
                latestByTemplate[attempt.item.templateId]?.attemptId != attempt.attemptId ->
                    SkillEvidence(attempt.attemptId, skill.id, version, 0, EvidenceDirection.EXCLUDED_DUPLICATE, "near_duplicate_template")
                else -> evidenceFor(skill.id, attempt, asOfEpochMillis)
            }
        }

        val contributingAttempts = latestByTemplate.values.sortedBy { it.occurredAtEpochMillis }
        val contributingEvidence = evidence.filter { it.weightBasisPoints > 0 }
        val totalWeight = contributingEvidence.sumOf(SkillEvidence::weightBasisPoints)
        val supportingWeight = contributingEvidence
            .filter { it.direction == EvidenceDirection.SUPPORTS_PRACTICE }
            .sumOf(SkillEvidence::weightBasisPoints)
        val weightedCorrectnessBasisPoints = if (totalWeight == 0) 0 else supportingWeight * 10_000 / totalWeight
        val distinctTemplates = contributingAttempts.map { it.item.templateId }.distinct().size
        val lastPracticed = active.maxOfOrNull(LearningAttempt::occurredAtEpochMillis)
        val practicedRecently = lastPracticed != null &&
            max(0L, asOfEpochMillis - lastPracticed) <= FOURTEEN_DAYS_MILLIS

        val status = when {
            distinctTemplates < MINIMUM_DISTINCT_TEMPLATES -> EvidenceStatus.INSUFFICIENT_EVIDENCE
            weightedCorrectnessBasisPoints < 6_000 -> EvidenceStatus.REVIEW_SUGGESTED
            practicedRecently && weightedCorrectnessBasisPoints >= 8_000 -> EvidenceStatus.PRACTICED_RECENTLY
            else -> EvidenceStatus.DEVELOPING
        }
        return evidence to PersonalSkillSummary(
            skillId = skill.id,
            skillTitle = skill.title,
            status = status,
            evidenceCount = contributingEvidence.size,
            distinctTemplateCount = distinctTemplates,
            lastPracticedAtEpochMillis = lastPracticed,
            reviewSuggested = status == EvidenceStatus.REVIEW_SUGGESTED,
            policyVersion = version,
        )
    }

    private fun evidenceFor(
        skillId: SkillId,
        attempt: LearningAttempt,
        asOfEpochMillis: Long,
    ): SkillEvidence {
        val recency = recencyBasisPoints(attempt.occurredAtEpochMillis, asOfEpochMillis)
        val assistance = when {
            attempt.assistance.expectedAnswerRevealed -> 2_000
            attempt.assistance.hintsUsed > 0 || attempt.assistance.workedStepsShown > 0 -> 6_000
            attempt.attemptOrdinal > 1 && attempt.outcome == AttemptOutcome.CORRECT -> 7_000
            else -> 10_000
        }
        val weight = recency * assistance / 10_000
        val correct = attempt.outcome == AttemptOutcome.CORRECT
        return SkillEvidence(
            attemptId = attempt.attemptId,
            skillId = skillId,
            policyVersion = version,
            weightBasisPoints = weight,
            direction = if (correct) EvidenceDirection.SUPPORTS_PRACTICE else EvidenceDirection.SUGGESTS_REVIEW,
            rationaleCode = buildString {
                append(if (correct) "correct" else "incorrect")
                if (attempt.attemptOrdinal > 1) append("_retry")
                if (!attempt.assistance.isUnassisted) append("_assisted")
                append("_recency_").append(recency)
            },
        )
    }

    private fun recencyBasisPoints(occurredAt: Long, asOf: Long): Int {
        val age = max(0L, asOf - occurredAt)
        return when {
            age <= FOURTEEN_DAYS_MILLIS -> 10_000
            age <= FORTY_FIVE_DAYS_MILLIS -> 8_000
            age <= NINETY_DAYS_MILLIS -> 5_000
            else -> 2_500
        }
    }

    private companion object {
        const val MINIMUM_DISTINCT_TEMPLATES = 5
        const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
        const val FOURTEEN_DAYS_MILLIS = 14L * DAY_MILLIS
        const val FORTY_FIVE_DAYS_MILLIS = 45L * DAY_MILLIS
        const val NINETY_DAYS_MILLIS = 90L * DAY_MILLIS
    }
}
