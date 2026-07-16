package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.GraphicsQuality

sealed interface RendererCommand {
    data class SetVisiblePieces(val pieceIndices: IntArray) : RendererCommand
    data class RevealPiece(val pieceIndex: Int, val reducedMotion: Boolean) : RendererCommand
    data class SetQuality(val quality: GraphicsQuality) : RendererCommand
    data object CorrectAnswerEffect : RendererCommand
    data object IncorrectAnswerEffect : RendererCommand
    data object CompletionEffect : RendererCommand
    data object Pause : RendererCommand
    data object Resume : RendererCommand
}

enum class RendererQualityProfile(
    val revealDurationSeconds: Float,
    val optionalParticleLimit: Int,
) {
    LOW(revealDurationSeconds = 0.18f, optionalParticleLimit = 12),
    MEDIUM(revealDurationSeconds = 0.28f, optionalParticleLimit = 32),
    HIGH(revealDurationSeconds = 0.34f, optionalParticleLimit = 64),
    ;

    companion object {
        fun from(quality: GraphicsQuality): RendererQualityProfile = when (quality) {
            GraphicsQuality.AUTO,
            GraphicsQuality.MEDIUM -> MEDIUM
            GraphicsQuality.LOW -> LOW
            GraphicsQuality.HIGH -> HIGH
        }
    }
}
