package com.qtpie.simplepuzzle.core.learning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SkillLifecycle {
    ACTIVE,
    RETIRED,
}

@Serializable
data class SkillDefinition(
    val id: SkillId,
    val taxonomyVersion: TaxonomyVersion,
    val title: String,
    val description: String,
    val prerequisites: Set<SkillId> = emptySet(),
    val lifecycle: SkillLifecycle = SkillLifecycle.ACTIVE,
    val replacementId: SkillId? = null,
) {
    init {
        require(title.isNotBlank()) { "Skill title cannot be blank." }
        require(description.isNotBlank()) { "Skill description cannot be blank." }
        require(id !in prerequisites) { "A skill cannot require itself." }
        require(lifecycle == SkillLifecycle.RETIRED || replacementId == null) {
            "Only a retired skill may name a replacement."
        }
        require(replacementId != id) { "A retired skill cannot replace itself." }
    }
}

@Serializable
data class CurriculumMapping(
    val curriculumId: CurriculumId,
    val curriculumVersion: String,
    val mappingVersion: ContentVersion,
    val skillId: SkillId,
    val grade: Int,
    val quarter: Int,
    val strand: String,
    val scopeStatement: String,
) {
    init {
        require(curriculumVersion.isNotBlank()) { "Curriculum version cannot be blank." }
        require(grade > 0) { "Grade must be positive." }
        require(quarter in 1..4) { "Quarter must be between 1 and 4." }
        require(strand.isNotBlank()) { "Curriculum strand cannot be blank." }
        require(scopeStatement.isNotBlank()) { "Scope statement cannot be blank." }
    }
}

@Serializable
data class LearningActivityDefinition(
    val id: ActivityId,
    val version: ActivityVersion,
    val title: String,
    val supportedSkills: Set<SkillId>,
) {
    init {
        require(title.isNotBlank()) { "Activity title cannot be blank." }
        require(supportedSkills.isNotEmpty()) { "An activity must support at least one skill." }
    }
}

@Serializable
enum class AuthoredDifficultyBand {
    FOUNDATIONAL,
    CORE,
    STRETCH,
}

@Serializable
enum class RepresentationType {
    SYMBOLIC_EQUATION,
    NUMERAL,
    QUANTITY,
    EXPANDED_FORM,
    PLACE_VALUE,
    NUMBER_LINE,
    PROPERTY_MODEL,
}

@Serializable
sealed interface StructuredResponse

@Serializable
@SerialName("integer")
data class IntegerResponse(val value: Int) : StructuredResponse

@Serializable
data class GeneratorProvenance(
    val generatorId: String,
    val generatorVersion: Int,
    val seed: Long,
    val configuration: Map<String, String>,
) {
    init {
        require(generatorId.isNotBlank()) { "Generator ID cannot be blank." }
        require(generatorVersion > 0) { "Generator version must be positive." }
        require(configuration.keys.none(String::isBlank)) {
            "Generator configuration keys cannot be blank."
        }
    }
}

@Serializable
data class LearningItemRef(
    val itemId: LearningItemId,
    val templateId: String,
    val activityId: ActivityId,
    val activityVersion: ActivityVersion,
    val contentVersion: ContentVersion,
    val primarySkillId: SkillId,
    val secondarySkillIds: Set<SkillId> = emptySet(),
    val authoredDifficulty: AuthoredDifficultyBand,
    val representation: RepresentationType,
    val provenance: GeneratorProvenance,
) {
    init {
        require(templateId.isNotBlank()) { "Template ID cannot be blank." }
        require(primarySkillId !in secondarySkillIds) {
            "The primary skill cannot also be a secondary skill."
        }
    }
}

@Serializable
data class LearningItem(
    val ref: LearningItemRef,
    val expectedResponse: StructuredResponse,
    val reviewedMisconceptionByResponse: Map<String, String> = emptyMap(),
) {
    init {
        require(reviewedMisconceptionByResponse.keys.none(String::isBlank)) {
            "Misconception response keys cannot be blank."
        }
        require(reviewedMisconceptionByResponse.values.none(String::isBlank)) {
            "Misconception tags cannot be blank."
        }
    }
}

@Serializable
enum class AttemptOutcome {
    CORRECT,
    INCORRECT,
    INVALIDATED,
}

@Serializable
data class AssistanceSummary(
    val hintsUsed: Int = 0,
    val workedStepsShown: Int = 0,
    val expectedAnswerRevealed: Boolean = false,
) {
    init {
        require(hintsUsed >= 0) { "Hint count cannot be negative." }
        require(workedStepsShown >= 0) { "Worked-step count cannot be negative." }
    }

    val isUnassisted: Boolean
        get() = hintsUsed == 0 && workedStepsShown == 0 && !expectedAnswerRevealed
}

@Serializable
data class ApplicationProvenance(
    val applicationVersion: String,
    val engineVersion: Int,
) {
    init {
        require(applicationVersion.isNotBlank()) { "Application version cannot be blank." }
        require(engineVersion > 0) { "Engine version must be positive." }
    }
}

@Serializable
data class LearningAttempt(
    val attemptId: AttemptId,
    val sessionId: SessionId,
    val activityId: ActivityId,
    val activityVersion: ActivityVersion,
    val item: LearningItemRef,
    val selectedResponse: StructuredResponse,
    val expectedResponse: StructuredResponse,
    val outcome: AttemptOutcome,
    val assistance: AssistanceSummary,
    val attemptOrdinal: Int,
    val occurredAtEpochMillis: Long,
    val elapsedMillis: Long? = null,
    val applicationProvenance: ApplicationProvenance,
    val supersedesAttemptId: AttemptId? = null,
    val invalidationReasonCode: String? = null,
) {
    init {
        require(activityId == item.activityId && activityVersion == item.activityVersion) {
            "Attempt activity must match the learning item."
        }
        require(attemptOrdinal > 0) { "Attempt ordinal must be positive." }
        require(occurredAtEpochMillis >= 0) { "Attempt time cannot be negative." }
        require(elapsedMillis == null || elapsedMillis >= 0) { "Elapsed time cannot be negative." }
        when (outcome) {
            AttemptOutcome.CORRECT -> require(selectedResponse == expectedResponse) {
                "A correct attempt must match the expected response."
            }
            AttemptOutcome.INCORRECT -> require(selectedResponse != expectedResponse) {
                "An incorrect attempt must differ from the expected response."
            }
            AttemptOutcome.INVALIDATED -> {
                requireNotNull(supersedesAttemptId) {
                    "An invalidation must identify the superseded attempt."
                }
                require(!invalidationReasonCode.isNullOrBlank()) {
                    "An invalidation must provide a reason code."
                }
            }
        }
        require(supersedesAttemptId != attemptId) { "An attempt cannot supersede itself." }
    }
}

@Serializable
enum class EvidenceDirection {
    SUPPORTS_PRACTICE,
    SUGGESTS_REVIEW,
    EXCLUDED_DUPLICATE,
    INVALIDATED,
}

@Serializable
data class SkillEvidence(
    val attemptId: AttemptId,
    val skillId: SkillId,
    val policyVersion: EvidencePolicyVersion,
    val weightBasisPoints: Int,
    val direction: EvidenceDirection,
    val rationaleCode: String,
) {
    init {
        require(weightBasisPoints in 0..10_000) { "Evidence weight must use 0..10,000 basis points." }
        require(rationaleCode.isNotBlank()) { "Evidence rationale cannot be blank." }
    }
}

@Serializable
enum class EvidenceStatus {
    INSUFFICIENT_EVIDENCE,
    DEVELOPING,
    PRACTICED_RECENTLY,
    REVIEW_SUGGESTED,
}

@Serializable
data class PersonalSkillSummary(
    val skillId: SkillId,
    val skillTitle: String,
    val status: EvidenceStatus,
    val evidenceCount: Int,
    val distinctTemplateCount: Int,
    val lastPracticedAtEpochMillis: Long?,
    val reviewSuggested: Boolean,
    val policyVersion: EvidencePolicyVersion,
) {
    init {
        require(skillTitle.isNotBlank()) { "Skill title cannot be blank." }
        require(evidenceCount >= 0) { "Evidence count cannot be negative." }
        require(distinctTemplateCount >= 0) { "Template count cannot be negative." }
        require(reviewSuggested == (status == EvidenceStatus.REVIEW_SUGGESTED)) {
            "Review suggestion must agree with status."
        }
    }
}

fun interface LearningClock {
    fun nowEpochMillis(): Long
}

interface LearningIdSource {
    fun nextAttemptId(): AttemptId

    fun nextSessionId(): SessionId
}
