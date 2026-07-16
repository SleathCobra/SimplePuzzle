package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.assets.PieceBounds
import com.qtpie.simplepuzzle.core.model.assets.PieceManifest
import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PuzzleMeshDataBuilderTest {
    @Test
    fun combinesPrecomputedPieceGeometryWithoutRuntimeTriangulation() {
        val data = PuzzleMeshDataBuilder.build(manifest(listOf(piece(index = 0))))

        assertArrayEquals(
            floatArrayOf(0f, 1f, 0f, 0f, 1f, 1f, 1f, 0f, 1f, 0f, 1f, 1f, 0f, 0f, 0f, 1f),
            data.vertices,
            0.0001f,
        )
        assertArrayEquals(shortArrayOf(0, 1, 2, 2, 3, 0), data.indices)
        assertArrayEquals(intArrayOf(0), data.indexOffsets)
        assertArrayEquals(intArrayOf(6), data.indexCounts)
        assertArrayEquals(floatArrayOf(0.5f), data.centerX, 0.0001f)
        assertArrayEquals(floatArrayOf(0.5f), data.centerY, 0.0001f)
    }

    @Test
    fun rejectsNonContiguousPieceIndices() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            PuzzleMeshDataBuilder.build(manifest(listOf(piece(index = 1))))
        }

        assertEquals("Puzzle piece indices must be contiguous and ordered from zero.", error.message)
    }

    private fun manifest(pieces: List<PieceManifest>) = PuzzleManifest(
        formatVersion = PUZZLE_FORMAT_VERSION,
        puzzleId = "test",
        title = "Test",
        category = "Test",
        sourceHashSha256 = "00",
        textureFile = "texture.png",
        thumbnailFile = "thumbnail.png",
        textureWidth = 32,
        textureHeight = 32,
        rows = 1,
        columns = pieces.size,
        edgeSeed = 1,
        pieces = pieces,
    )

    private fun piece(index: Int) = PieceManifest(
        index = index,
        vertices = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
        textureCoordinates = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
        triangleIndices = listOf(0, 1, 2, 2, 3, 0),
        finalX = 0f,
        finalY = 0f,
        width = 1f,
        height = 1f,
        bounds = PieceBounds(0f, 0f, 1f, 1f),
        revealOrder = index,
    )
}
