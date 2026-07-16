package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion

fun interface QuestionGenerator {
    fun generate(difficulty: Difficulty, random: RandomSource): MathQuestion
}

data class DifficultyPolicy(
    val operandRange: IntRange,
    val distractorSpread: Int,
)

object DifficultyPolicies {
    fun forDifficulty(difficulty: Difficulty): DifficultyPolicy = when (difficulty) {
        Difficulty.EASY -> DifficultyPolicy(1..20, distractorSpread = 8)
        Difficulty.MEDIUM -> DifficultyPolicy(10..50, distractorSpread = 15)
        Difficulty.HARD -> DifficultyPolicy(50..150, distractorSpread = 30)
    }
}

class DefaultMathQuestionGenerator(
    private val optionCount: Int = 4,
) : QuestionGenerator {
    init {
        require(optionCount >= 2) { "At least two answer options are required." }
    }

    override fun generate(difficulty: Difficulty, random: RandomSource): MathQuestion {
        val policy = DifficultyPolicies.forDifficulty(difficulty)
        val first = random.nextInt(policy.operandRange.first, policy.operandRange.last + 1)
        val second = random.nextInt(policy.operandRange.first, policy.operandRange.last + 1)
        val operation = if (random.nextBoolean()) MathOperation.ADD else MathOperation.SUBTRACT
        val left = if (operation == MathOperation.SUBTRACT) maxOf(first, second) else first
        val right = if (operation == MathOperation.SUBTRACT) minOf(first, second) else second
        val answer = operation.evaluate(left, right)

        val options = linkedSetOf(answer)
        var attempts = 0
        val maximumAttempts = optionCount * 20
        while (options.size < optionCount && attempts < maximumAttempts) {
            val offset = random.nextInt(-policy.distractorSpread, policy.distractorSpread + 1)
            val candidate = answer + offset
            if (candidate >= 0 && candidate != answer) {
                options += candidate
            }
            attempts++
        }

        var distance = policy.distractorSpread + 1
        while (options.size < optionCount) {
            options += answer + distance
            if (options.size < optionCount && answer - distance >= 0) {
                options += answer - distance
            }
            distance++
        }

        return MathQuestion(
            leftOperand = left,
            operation = operation,
            rightOperand = right,
            answer = answer,
            options = random.shuffled(options.toList()),
        )
    }
}
