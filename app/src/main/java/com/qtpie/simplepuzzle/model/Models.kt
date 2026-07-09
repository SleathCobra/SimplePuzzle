package com.qtpie.simplepuzzle.model

import kotlin.random.Random

data class MathQuestion(
    val problem: String,
    val answer: Int,
    val options: List<Int>
)

enum class Difficulty { Easy, Medium, Hard }

data class UserSettings(
    val soundEffectsEnabled: Boolean = true,
    val backgroundMusicEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val difficulty: Difficulty = Difficulty.Medium
)

data class PuzzleInfo(
    val id: Int,
    val name: String,
    val imageResId: Int,
    val totalPieces: Int = 16,
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false,
    val category: String = "All",
    val bestTime: Int? = null // in seconds
)

fun generateMathQuestion(difficulty: Difficulty = Difficulty.Medium): MathQuestion {
    val range = when (difficulty) {
        Difficulty.Easy -> 1..20
        Difficulty.Medium -> 10..50
        Difficulty.Hard -> 50..150
    }
    
    val a = Random.nextInt(range.first, range.last)
    val b = Random.nextInt(range.first, range.last)
    val op = if (Random.nextBoolean()) "+" else "-"
    val answer = if (op == "+") a + b else a - b
    
    val options = mutableSetOf(answer)
    while (options.size < 4) {
        val wrong = answer + Random.nextInt(-10, 11)
        if (wrong != answer) options.add(wrong)
    }
    return MathQuestion("$a $op $b", answer, options.toList().shuffled())
}
