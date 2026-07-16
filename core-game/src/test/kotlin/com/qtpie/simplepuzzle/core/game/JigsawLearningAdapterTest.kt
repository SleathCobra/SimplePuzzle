package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.learning.AssistanceSummary
import com.qtpie.simplepuzzle.core.learning.AttemptId
import com.qtpie.simplepuzzle.core.learning.AttemptOutcome
import com.qtpie.simplepuzzle.core.learning.Grade2Quarter1Skills
import com.qtpie.simplepuzzle.core.learning.LearningClock
import com.qtpie.simplepuzzle.core.learning.LearningIdSource
import com.qtpie.simplepuzzle.core.learning.SessionId
import com.qtpie.simplepuzzle.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JigsawLearningAdapterTest {
    private val generator = DefaultMathQuestionGenerator()
    private val itemFactory = JigsawLearningItemFactory()

    @Test
    fun learningItemReproducesExactlyFromSeedAndVersions() {
        val question = generator.generateFromSeed(Difficulty.MEDIUM, 442L)
        val item = itemFactory.fromQuestion(question, Difficulty.MEDIUM)

        val reproduced = itemFactory.reproduce(item.ref)

        assertEquals(item, reproduced)
        assertEquals(442L, reproduced.ref.provenance.seed)
        assertTrue(reproduced.reviewedMisconceptionByResponse.isEmpty())
    }

    @Test
    fun itemIdentityIncludesGeneratorContentAndOptionConfiguration() {
        val fourOptions = itemFactory.fromQuestion(
            DefaultMathQuestionGenerator(optionCount = 4).generateFromSeed(Difficulty.EASY, 91L),
            Difficulty.EASY,
        )
        val fiveOptions = itemFactory.fromQuestion(
            DefaultMathQuestionGenerator(optionCount = 5).generateFromSeed(Difficulty.EASY, 91L),
            Difficulty.EASY,
        )

        assertTrue(fourOptions.ref.itemId != fiveOptions.ref.itemId)
        assertEquals(fourOptions, itemFactory.reproduce(fourOptions.ref))
        assertEquals(fiveOptions, itemFactory.reproduce(fiveOptions.ref))
    }

    @Test
    fun additionSkillDistinguishesRegroupingWithoutInferringMisconceptions() {
        val questions = (1L..500L).map { generator.generateFromSeed(Difficulty.MEDIUM, it) }
        val without = questions.first { (it.leftOperand % 10) + (it.rightOperand % 10) < 10 }
        val with = questions.first { (it.leftOperand % 10) + (it.rightOperand % 10) >= 10 }

        assertEquals(
            Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING,
            itemFactory.fromQuestion(without, Difficulty.MEDIUM).ref.primarySkillId,
        )
        assertEquals(
            Grade2Quarter1Skills.ADD_WITH_REGROUPING,
            itemFactory.fromQuestion(with, Difficulty.MEDIUM).ref.primarySkillId,
        )
        assertTrue(itemFactory.fromQuestion(with, Difficulty.MEDIUM).reviewedMisconceptionByResponse.isEmpty())
    }

    @Test
    fun attemptFactoryUsesInjectedClockIdsRetryAndAssistance() {
        val ids = SequenceIds()
        val factory = JigsawLearningAttemptFactory(
            clock = LearningClock { 12_345L },
            idSource = ids,
            applicationVersion = "test",
            itemFactory = itemFactory,
        )
        val question = generator.generateFromSeed(Difficulty.EASY, 10L)

        val attempt = factory.create(
            sessionId = factory.newSessionId(),
            question = question,
            difficulty = Difficulty.EASY,
            selectedAnswer = Int.MIN_VALUE,
            attemptOrdinal = 2,
            assistance = AssistanceSummary(hintsUsed = 1),
        )

        assertEquals(AttemptOutcome.INCORRECT, attempt.outcome)
        assertEquals(2, attempt.attemptOrdinal)
        assertEquals(12_345L, attempt.occurredAtEpochMillis)
        assertFalse(attempt.assistance.isUnassisted)
    }

    private class SequenceIds : LearningIdSource {
        private var attempt = 0
        private var session = 0

        override fun nextAttemptId() = AttemptId("attempt-test-${++attempt}")

        override fun nextSessionId() = SessionId("session-test-${++session}")
    }
}
