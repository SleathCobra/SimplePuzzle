package com.qtpie.simplepuzzle.core.game

import kotlin.random.Random

interface RandomSource {
    fun nextInt(fromInclusive: Int, untilExclusive: Int): Int

    fun nextBoolean(): Boolean = nextInt(0, 2) == 0

    fun <T> shuffled(values: List<T>): List<T> {
        val result = values.toMutableList()
        for (index in result.lastIndex downTo 1) {
            val swapIndex = nextInt(0, index + 1)
            val value = result[index]
            result[index] = result[swapIndex]
            result[swapIndex] = value
        }
        return result
    }
}

class SeededRandomSource(seed: Long) : RandomSource {
    private val random = Random(seed)

    override fun nextInt(fromInclusive: Int, untilExclusive: Int): Int =
        random.nextInt(fromInclusive, untilExclusive)
}
