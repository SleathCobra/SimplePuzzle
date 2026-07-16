package com.qtpie.simplepuzzle.core.model.assets

import kotlinx.serialization.Serializable

const val PUZZLE_FORMAT_VERSION = 2

@Serializable
data class PuzzleDefinition(
    val id: String,
    val title: String,
    val category: String,
    val sourceImage: String,
    val rows: Int,
    val columns: Int,
    val activeTextureMaxSize: Int = 1024,
    val thumbnailSize: Int = 320,
    val edgeSeed: Long,
)

@Serializable
data class PuzzleManifest(
    val formatVersion: Int,
    val puzzleId: String,
    val title: String,
    val category: String,
    val sourceHashSha256: String,
    val textureFile: String,
    val thumbnailFile: String,
    val textureWidth: Int,
    val textureHeight: Int,
    val rows: Int,
    val columns: Int,
    val edgeSeed: Long,
    val pieces: List<PieceManifest>,
)

@Serializable
data class PieceManifest(
    val index: Int,
    val vertices: List<Float>,
    val textureCoordinates: List<Float>,
    val triangleIndices: List<Int>,
    val finalX: Float,
    val finalY: Float,
    val width: Float,
    val height: Float,
    val bounds: PieceBounds,
    val revealOrder: Int,
)

@Serializable
data class PieceBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
