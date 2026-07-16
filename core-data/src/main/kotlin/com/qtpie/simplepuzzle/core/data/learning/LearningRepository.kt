package com.qtpie.simplepuzzle.core.data.learning

import com.qtpie.simplepuzzle.core.learning.Grade2Quarter1Taxonomy
import com.qtpie.simplepuzzle.core.learning.InitialEvidencePolicy
import com.qtpie.simplepuzzle.core.learning.LearningAttempt
import com.qtpie.simplepuzzle.core.learning.LearningClock
import com.qtpie.simplepuzzle.core.learning.PersonalSkillSummary
import com.qtpie.simplepuzzle.core.learning.SkillTaxonomy
import com.qtpie.simplepuzzle.core.learning.EvidencePolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class AttemptRecordingResult {
    RECORDED,
    DUPLICATE_IGNORED,
}

interface LearningRepository {
    val attempts: Flow<List<LearningAttempt>>
    val summaries: Flow<List<PersonalSkillSummary>>

    suspend fun recordAttempt(attempt: LearningAttempt): AttemptRecordingResult
}

object SystemLearningClock : LearningClock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

class RoomLearningRepository(
    private val dao: LearningDao,
    private val clock: LearningClock = SystemLearningClock,
    private val taxonomy: SkillTaxonomy = Grade2Quarter1Taxonomy.value,
    private val evidencePolicy: EvidencePolicy = InitialEvidencePolicy(),
) : LearningRepository {
    override val attempts: Flow<List<LearningAttempt>> = dao.observeAttempts().map { rows ->
        rows.map(LearningAttemptEntity::toModel)
    }

    override val summaries: Flow<List<PersonalSkillSummary>> = attempts.map { attempts ->
        val attemptedSkillIds = attempts.flatMap { attempt ->
            listOf(attempt.item.primarySkillId) + attempt.item.secondarySkillIds
        }.toSet()
        val asOf = clock.nowEpochMillis()
        taxonomy.skills
            .filter { it.id in attemptedSkillIds }
            .map { skill -> evidencePolicy.derive(skill, attempts, asOf).second }
            .sortedBy(PersonalSkillSummary::skillTitle)
    }

    override suspend fun recordAttempt(attempt: LearningAttempt): AttemptRecordingResult {
        val inserted = dao.recordAttempt(attempt.toEntity(), attempt.outcome)
        return if (inserted) AttemptRecordingResult.RECORDED else AttemptRecordingResult.DUPLICATE_IGNORED
    }
}
