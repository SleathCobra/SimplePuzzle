package com.qtpie.simplepuzzle.model

import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import kotlin.random.Random

data class MathQuestion(
    val problem: String,
    val answer: Int,
    val options: List<Int>
)

enum class Difficulty { Easy, Medium, Hard }

enum class BackgroundMusicMode {
    Random, Music1, Music2, Music3, Music4
}

data class UserSettings(
    val soundEffectsEnabled: Boolean = true,
    val soundEffectsVolume: Float = 0.8f,
    val backgroundMusicEnabled: Boolean = true,
    val backgroundMusicVolume: Float = 0.5f,
    val backgroundMusicMode: BackgroundMusicMode = BackgroundMusicMode.Random,
    val hapticFeedbackEnabled: Boolean = true,
    val showSpecialConfetti: Boolean = true,
    val difficulty: Difficulty = Difficulty.Medium,
    val graphicsQuality: GraphicsQuality = GraphicsQuality.AUTO,
    val reducedMotion: Boolean = false,
)

data class PuzzleInfo(
    val id: String,
    val name: String,
    val imageResId: Int,
    val assetRoot: String,
    val totalPieces: Int = 16,
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false,
    val category: String = "All",
    val bestTime: Int? = null, // in seconds
    val maxScore: Int = 0,
    val unlockCost: Int = 50
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
