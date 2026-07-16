package com.qtpie.simplepuzzle.model

import com.qtpie.simplepuzzle.core.model.GraphicsQuality

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
    val id: Int,
    val name: String,
    val imageResId: Int,
    val totalPieces: Int = 16,
    val isLocked: Boolean = false,
    val isCompleted: Boolean = false,
    val category: String = "All",
    val bestTime: Int? = null, // in seconds
    val maxScore: Int = 0,
    val unlockCost: Int = 50
)
