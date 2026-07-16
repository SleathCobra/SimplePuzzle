package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.core.model.BoardMutationId

sealed interface RendererCommand {
    data class SetVisiblePieces(
        val pieceIndices: IntArray,
        val sessionGeneration: Long = 1,
    ) : RendererCommand
    data class RevealPiece(
        val mutationId: BoardMutationId,
        val pieceIndex: Int,
        val reducedMotion: Boolean,
    ) : RendererCommand
    data class RemovePiece(
        val mutationId: BoardMutationId,
        val pieceIndex: Int,
        val reducedMotion: Boolean,
    ) : RendererCommand
    data class SetQuality(val quality: GraphicsQuality) : RendererCommand
    data object CorrectAnswerEffect : RendererCommand
    data object IncorrectAnswerEffect : RendererCommand
    data object CompletionEffect : RendererCommand
    data object Pause : RendererCommand
    data object Resume : RendererCommand
}

enum class RendererQualityProfile(
    val revealDurationSeconds: Float,
    val removalDurationSeconds: Float,
    val optionalParticleLimit: Int,
    val removalParticleLimit: Int,
) {
    LOW(
        revealDurationSeconds = 0.18f,
        removalDurationSeconds = 0.12f,
        optionalParticleLimit = 12,
        removalParticleLimit = 0,
    ),
    MEDIUM(
        revealDurationSeconds = 0.28f,
        removalDurationSeconds = 0.34f,
        optionalParticleLimit = 32,
        removalParticleLimit = 8,
    ),
    HIGH(
        revealDurationSeconds = 0.34f,
        removalDurationSeconds = 0.42f,
        optionalParticleLimit = 64,
        removalParticleLimit = 16,
    ),
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
