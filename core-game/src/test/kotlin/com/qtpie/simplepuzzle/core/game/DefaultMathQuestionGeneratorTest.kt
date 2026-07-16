package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.Difficulty
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultMathQuestionGeneratorTest {
    private val generator = DefaultMathQuestionGenerator()

    @Test
    fun sameSeedProducesSameQuestionSequence() {
        val first = SeededRandomSource(4821L)
        val second = SeededRandomSource(4821L)

        val firstSequence = List(30) { generator.generate(Difficulty.MEDIUM, first) }
        val secondSequence = List(30) { generator.generate(Difficulty.MEDIUM, second) }

        assertEquals(firstSequence, secondSequence)
    }

    @Test
    fun generatedQuestionsHaveValidUniqueChoicesForEveryDifficulty() {
        Difficulty.entries.forEach { difficulty ->
            val random = SeededRandomSource(9000L + difficulty.ordinal)
            val policy = DifficultyPolicies.forDifficulty(difficulty)
            repeat(500) {
                val question = generator.generate(difficulty, random)

                assertEquals(question.operation.evaluate(question.leftOperand, question.rightOperand), question.answer)
                assertEquals(4, question.options.size)
                assertEquals(4, question.options.distinct().size)
                assertTrue(question.answer in question.options)
                assertTrue(question.options.all { it >= 0 })
                assertTrue(question.leftOperand in policy.operandRange)
                assertTrue(question.rightOperand in policy.operandRange)
            }
        }
    }
}
