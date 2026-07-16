package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest

internal data class PuzzleMeshData(
    val vertices: FloatArray,
    val indices: ShortArray,
    val indexOffsets: IntArray,
    val indexCounts: IntArray,
    val centerX: FloatArray,
    val centerY: FloatArray,
)

internal object PuzzleMeshDataBuilder {
    private const val COMPONENTS_PER_VERTEX = 4

    fun build(manifest: PuzzleManifest): PuzzleMeshData {
        require(manifest.pieces.isNotEmpty()) { "Puzzle manifest must contain pieces." }
        require(manifest.pieces.map { it.index } == manifest.pieces.indices.toList()) {
            "Puzzle piece indices must be contiguous and ordered from zero."
        }

        val vertexCount = manifest.pieces.sumOf { piece ->
            require(piece.vertices.size == piece.textureCoordinates.size) {
                "Piece ${piece.index} vertex and texture-coordinate counts differ."
            }
            require(piece.vertices.size % 2 == 0) {
                "Piece ${piece.index} vertex coordinates must be x/y pairs."
            }
            piece.vertices.size / 2
        }
        require(vertexCount <= Short.MAX_VALUE.toInt()) {
            "Puzzle mesh exceeds the 16-bit index limit."
        }

        val indexCount = manifest.pieces.sumOf { it.triangleIndices.size }
        val vertices = FloatArray(vertexCount * COMPONENTS_PER_VERTEX)
        val indices = ShortArray(indexCount)
        val indexOffsets = IntArray(manifest.pieces.size)
        val indexCounts = IntArray(manifest.pieces.size)
        val centerX = FloatArray(manifest.pieces.size)
        val centerY = FloatArray(manifest.pieces.size)
        var vertexBase = 0
        var vertexWrite = 0
        var indexWrite = 0

        manifest.pieces.forEach { piece ->
            indexOffsets[piece.index] = indexWrite
            indexCounts[piece.index] = piece.triangleIndices.size
            centerX[piece.index] = (piece.bounds.left + piece.bounds.right) * 0.5f
            centerY[piece.index] = 1f - (piece.bounds.top + piece.bounds.bottom) * 0.5f
            var pair = 0
            while (pair < piece.vertices.size) {
                vertices[vertexWrite++] = piece.vertices[pair]
                vertices[vertexWrite++] = 1f - piece.vertices[pair + 1]
                vertices[vertexWrite++] = piece.textureCoordinates[pair]
                vertices[vertexWrite++] = piece.textureCoordinates[pair + 1]
                pair += 2
            }
            piece.triangleIndices.forEach { localIndex ->
                require(localIndex in 0 until piece.vertices.size / 2) {
                    "Piece ${piece.index} has an out-of-range triangle index."
                }
                indices[indexWrite++] = (vertexBase + localIndex).toShort()
            }
            vertexBase += piece.vertices.size / 2
        }

        return PuzzleMeshData(vertices, indices, indexOffsets, indexCounts, centerX, centerY)
    }
}
