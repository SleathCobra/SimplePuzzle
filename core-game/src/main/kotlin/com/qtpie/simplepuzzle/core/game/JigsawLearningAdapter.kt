package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.learning.ActivityId
import com.qtpie.simplepuzzle.core.learning.ActivityVersion
import com.qtpie.simplepuzzle.core.learning.ApplicationProvenance
import com.qtpie.simplepuzzle.core.learning.AssistanceSummary
import com.qtpie.simplepuzzle.core.learning.AttemptOutcome
import com.qtpie.simplepuzzle.core.learning.AuthoredDifficultyBand
import com.qtpie.simplepuzzle.core.learning.ContentVersion
import com.qtpie.simplepuzzle.core.learning.GeneratorProvenance
import com.qtpie.simplepuzzle.core.learning.Grade2Quarter1Skills
import com.qtpie.simplepuzzle.core.learning.IntegerResponse
import com.qtpie.simplepuzzle.core.learning.LearningAttempt
import com.qtpie.simplepuzzle.core.learning.LearningClock
import com.qtpie.simplepuzzle.core.learning.LearningIdSource
import com.qtpie.simplepuzzle.core.learning.LearningItem
import com.qtpie.simplepuzzle.core.learning.LearningItemId
import com.qtpie.simplepuzzle.core.learning.LearningItemRef
import com.qtpie.simplepuzzle.core.learning.RepresentationType
import com.qtpie.simplepuzzle.core.learning.SessionId
import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion

object JigsawLearningContract {
    val activityId = ActivityId("jm.activity.jigsaw-math")
    val activityVersion = ActivityVersion(1)
    const val ENGINE_VERSION = 1
}

class JigsawLearningItemFactory {
    fun fromQuestion(question: MathQuestion, difficulty: Difficulty): LearningItem {
        require(question.operation == MathOperation.ADD) {
            "Only owner-approved addition questions emit Academy Phase 1 evidence."
        }
        require(question.answer <= 1_000) {
            "Grade 2 Quarter 1 addition evidence must have a sum no greater than 1,000."
        }
        val source = requireNotNull(question.provenance) {
            "A learning item requires deterministic question provenance."
        }
        require(source.generatorId == DefaultMathQuestionGenerator.GENERATOR_ID) {
            "Unsupported question generator ${source.generatorId}."
        }
        require(source.generatorVersion == DefaultMathQuestionGenerator.GENERATOR_VERSION) {
            "Unsupported question generator version ${source.generatorVersion}."
        }
        val regrouping = requiresRegrouping(question.leftOperand, question.rightOperand)
        val skill = if (regrouping) {
            Grade2Quarter1Skills.ADD_WITH_REGROUPING
        } else {
            Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING
        }
        val templateId = templateId(question, difficulty, regrouping)
        val seedPart = source.seed.toULong().toString(16)
        val optionCount = requireNotNull(source.configuration["optionCount"]) {
            "Jigsaw item provenance is missing optionCount."
        }.toInt()
        val itemRef = LearningItemRef(
            itemId = LearningItemId(
                "jm.item.jigsaw-add.g${source.generatorVersion}.c${source.contentVersion}." +
                    "o$optionCount.$seedPart.${difficulty.name.lowercase()}",
            ),
            templateId = templateId,
            activityId = JigsawLearningContract.activityId,
            activityVersion = JigsawLearningContract.activityVersion,
            contentVersion = ContentVersion(source.contentVersion),
            primarySkillId = skill,
            authoredDifficulty = when (difficulty) {
                Difficulty.EASY -> AuthoredDifficultyBand.FOUNDATIONAL
                Difficulty.MEDIUM -> AuthoredDifficultyBand.CORE
                Difficulty.HARD -> AuthoredDifficultyBand.STRETCH
            },
            representation = RepresentationType.SYMBOLIC_EQUATION,
            provenance = GeneratorProvenance(
                generatorId = source.generatorId,
                generatorVersion = source.generatorVersion,
                seed = source.seed,
                configuration = source.configuration.toSortedMap(),
            ),
        )
        return LearningItem(
            ref = itemRef,
            expectedResponse = IntegerResponse(question.answer),
            // Current random distractors were not authored as reviewed
            // misconceptions, so no misconception is inferred from a choice.
            reviewedMisconceptionByResponse = emptyMap(),
        )
    }

    fun reproduce(item: LearningItemRef): LearningItem {
        require(item.activityId == JigsawLearningContract.activityId) {
            "Cannot reproduce a non-Jigsaw learning item."
        }
        val difficulty = Difficulty.valueOf(
            requireNotNull(item.provenance.configuration["difficulty"]) {
                "Jigsaw item provenance is missing difficulty."
            },
        )
        val optionCount = requireNotNull(item.provenance.configuration["optionCount"]) {
            "Jigsaw item provenance is missing optionCount."
        }.toInt()
        val question = DefaultMathQuestionGenerator(optionCount).generateFromSeed(
            difficulty,
            item.provenance.seed,
        )
        return fromQuestion(question, difficulty).also { reproduced ->
            require(reproduced.ref == item) { "Reproduced item metadata differs from the stored reference." }
        }
    }

    private fun templateId(question: MathQuestion, difficulty: Difficulty, regrouping: Boolean): String {
        val leftDigits = question.leftOperand.toString().length
        val rightDigits = question.rightOperand.toString().length
        val onesBand = ((question.leftOperand % 10) + (question.rightOperand % 10)) / 5
        return buildString {
            append("jm.template.jigsaw-add-v1.")
            append(difficulty.name.lowercase()).append('.')
            append(if (regrouping) "regroup" else "no-regroup").append('.')
            append(leftDigits).append('x').append(rightDigits).append('.')
            append("ones-").append(onesBand)
        }
    }

    private fun requiresRegrouping(left: Int, right: Int): Boolean {
        var remainingLeft = left
        var remainingRight = right
        var carry = 0
        while (remainingLeft > 0 || remainingRight > 0) {
            val sum = remainingLeft % 10 + remainingRight % 10 + carry
            if (sum >= 10) return true
            carry = sum / 10
            remainingLeft /= 10
            remainingRight /= 10
        }
        return false
    }
}

class JigsawLearningAttemptFactory(
    private val clock: LearningClock,
    private val idSource: LearningIdSource,
    private val applicationVersion: String,
    private val itemFactory: JigsawLearningItemFactory = JigsawLearningItemFactory(),
) {
    fun newSessionId(): SessionId = idSource.nextSessionId()

    fun create(
        sessionId: SessionId,
        question: MathQuestion,
        difficulty: Difficulty,
        selectedAnswer: Int,
        attemptOrdinal: Int,
        assistance: AssistanceSummary = AssistanceSummary(),
        elapsedMillis: Long? = null,
    ): LearningAttempt {
        val item = itemFactory.fromQuestion(question, difficulty)
        val selected = IntegerResponse(selectedAnswer)
        return LearningAttempt(
            attemptId = idSource.nextAttemptId(),
            sessionId = sessionId,
            activityId = item.ref.activityId,
            activityVersion = item.ref.activityVersion,
            item = item.ref,
            selectedResponse = selected,
            expectedResponse = item.expectedResponse,
            outcome = if (selected == item.expectedResponse) AttemptOutcome.CORRECT else AttemptOutcome.INCORRECT,
            assistance = assistance,
            attemptOrdinal = attemptOrdinal,
            occurredAtEpochMillis = clock.nowEpochMillis(),
            elapsedMillis = elapsedMillis,
            applicationProvenance = ApplicationProvenance(
                applicationVersion = applicationVersion,
                engineVersion = JigsawLearningContract.ENGINE_VERSION,
            ),
        )
    }
}
