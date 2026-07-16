package com.qtpie.simplepuzzle.core.game

import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.MathOperation
import com.qtpie.simplepuzzle.core.model.MathQuestion
import com.qtpie.simplepuzzle.core.model.MathQuestionProvenance

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
        val seed = nextItemSeed(random)
        return generateFromSeed(difficulty, seed)
    }

    fun generateFromSeed(difficulty: Difficulty, seed: Long): MathQuestion {
        val policy = DifficultyPolicies.forDifficulty(difficulty)
        val itemRandom = SeededRandomSource(seed)
        val left = itemRandom.nextInt(policy.operandRange.first, policy.operandRange.last + 1)
        val right = itemRandom.nextInt(policy.operandRange.first, policy.operandRange.last + 1)
        // Academy Phase 1 deliberately limits the default pilot generator to
        // owner-approved Grade 2 Quarter 1 addition. SUBTRACT remains in the
        // engine model for existing authored/test questions but is not emitted
        // as unreviewed learning evidence.
        val operation = MathOperation.ADD
        val answer = operation.evaluate(left, right)

        val options = linkedSetOf(answer)
        var attempts = 0
        val maximumAttempts = optionCount * 20
        while (options.size < optionCount && attempts < maximumAttempts) {
            val offset = itemRandom.nextInt(-policy.distractorSpread, policy.distractorSpread + 1)
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
            options = itemRandom.shuffled(options.toList()),
            provenance = MathQuestionProvenance(
                generatorId = GENERATOR_ID,
                generatorVersion = GENERATOR_VERSION,
                contentVersion = CONTENT_VERSION,
                seed = seed,
                configuration = sortedMapOf(
                    "difficulty" to difficulty.name,
                    "optionCount" to optionCount.toString(),
                    "operation" to operation.name,
                ),
            ),
        )
    }

    private fun nextItemSeed(random: RandomSource): Long {
        val high = random.nextInt(0, 1 shl 30).toLong()
        val low = random.nextInt(0, 1 shl 30).toLong()
        return (high shl 30) or low
    }

    companion object {
        const val GENERATOR_ID = "jm.jigsaw.addition"
        const val GENERATOR_VERSION = 1
        const val CONTENT_VERSION = 1
    }
}
