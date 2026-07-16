package com.qtpie.simplepuzzle.assets

import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest
import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlinx.serialization.json.Json
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PuzzleAssetGeneratorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun identicalInputProducesIdenticalArtifacts() {
        val root = temporaryFolder.root.toPath()
        val source = root.resolve("source.png")
        val definition = root.resolve("puzzle.json")
        writeSource(source)
        Files.writeString(definition, definitionJson(rows = 2, columns = 3))

        val first = root.resolve("first")
        val second = root.resolve("second")
        val generator = PuzzleAssetGenerator()
        val firstManifest = generator.generate(definition, first)
        val secondManifest = generator.generate(definition, second)

        assertEquals(firstManifest, secondManifest)
        listOf("texture.png", "thumbnail.png", "manifest.json").forEach { name ->
            assertArrayEquals(Files.readAllBytes(first.resolve(name)), Files.readAllBytes(second.resolve(name)))
        }
        assertEquals(6, firstManifest.pieces.size)
    }

    @Test
    fun manifestRoundTripsThroughSerialization() {
        val root = temporaryFolder.root.toPath()
        val source = root.resolve("source.png")
        val definition = root.resolve("puzzle.json")
        val output = root.resolve("output")
        writeSource(source)
        Files.writeString(definition, definitionJson(rows = 1, columns = 1))

        val generated = PuzzleAssetGenerator().generate(definition, output)
        val decoded = Json { ignoreUnknownKeys = false }.decodeFromString<PuzzleManifest>(
            Files.readString(output.resolve("manifest.json")),
        )

        assertEquals(generated, decoded)
    }

    @Test
    fun generatedJigsawMeshesPartitionTheBoardAndArePreTriangulated() {
        val root = temporaryFolder.root.toPath()
        val source = root.resolve("source.png")
        val definition = root.resolve("puzzle.json")
        writeSource(source)
        Files.writeString(definition, definitionJson(rows = 3, columns = 4))

        val manifest = PuzzleAssetGenerator().generate(definition, root.resolve("output"))

        assertEquals((0 until 12).toList(), manifest.pieces.map { it.revealOrder }.sorted())
        assertTrue(manifest.pieces.all { it.vertices.size > 8 })
        assertTrue(manifest.pieces.all { piece ->
            piece.triangleIndices.size == (piece.vertices.size / 2 - 2) * 3 &&
                piece.triangleIndices.all { it in 0 until piece.vertices.size / 2 }
        })
        manifest.pieces.forEach { piece ->
            assertEquals(
                polygonArea(piece.vertices),
                triangleArea(piece.vertices, piece.triangleIndices),
                0.00001f,
            )
        }
        assertEquals(1f, manifest.pieces.sumOf { polygonArea(it.vertices).toDouble() }.toFloat(), 0.00001f)

        val first = coordinatePairs(manifest.pieces[0].vertices)
        val rightNeighbor = coordinatePairs(manifest.pieces[1].vertices)
        assertTrue("Adjacent pieces must share the same precomputed tab boundary.", (first intersect rightNeighbor).size >= 7)
    }

    @Test
    fun edgeSeedChangesTopologyWithoutChangingSourceTexture() {
        val root = temporaryFolder.root.toPath()
        val source = root.resolve("source.png")
        val firstDefinition = root.resolve("first.json")
        val secondDefinition = root.resolve("second.json")
        writeSource(source)
        Files.writeString(firstDefinition, definitionJson(rows = 2, columns = 3, edgeSeed = 42))
        Files.writeString(secondDefinition, definitionJson(rows = 2, columns = 3, edgeSeed = 43))

        val generator = PuzzleAssetGenerator()
        val firstOutput = root.resolve("first-output")
        val secondOutput = root.resolve("second-output")
        val first = generator.generate(firstDefinition, firstOutput)
        val second = generator.generate(secondDefinition, secondOutput)

        assertNotEquals(first.pieces.map { it.vertices }, second.pieces.map { it.vertices })
        assertArrayEquals(
            Files.readAllBytes(firstOutput.resolve("texture.png")),
            Files.readAllBytes(secondOutput.resolve("texture.png")),
        )
    }

    @Test
    fun invalidDefinitionFailsClearly() {
        val root = temporaryFolder.root.toPath()
        val definition = root.resolve("invalid.json")
        Files.writeString(definition, definitionJson(rows = 0, columns = 3))

        val error = assertThrows(IllegalArgumentException::class.java) {
            PuzzleAssetGenerator().generate(definition, root.resolve("output"))
        }

        assertEquals("Puzzle rows and columns must be positive.", error.message)
    }

    private fun writeSource(path: java.nio.file.Path) {
        val image = BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(30, 60, 180)
            graphics.fillRect(0, 0, 32, 24)
            graphics.color = Color(240, 100, 140)
            graphics.fillOval(8, 4, 16, 16)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", path.toFile())
    }

    private fun coordinatePairs(coordinates: List<Float>): Set<Pair<Float, Float>> = coordinates
        .chunked(2)
        .mapTo(mutableSetOf()) { (x, y) -> x to y }

    private fun polygonArea(coordinates: List<Float>): Float {
        val points = coordinates.chunked(2).map { (x, y) -> x to y }
        return points.indices.sumOf { index ->
            val current = points[index]
            val next = points[(index + 1) % points.size]
            (current.first * next.second - next.first * current.second).toDouble()
        }.toFloat() * 0.5f
    }

    private fun triangleArea(coordinates: List<Float>, indices: List<Int>): Float = indices
        .chunked(3)
        .sumOf { (a, b, c) ->
            val ax = coordinates[a * 2]
            val ay = coordinates[a * 2 + 1]
            val bx = coordinates[b * 2]
            val by = coordinates[b * 2 + 1]
            val cx = coordinates[c * 2]
            val cy = coordinates[c * 2 + 1]
            (((bx - ax) * (cy - ay) - (by - ay) * (cx - ax)) * 0.5f).toDouble()
        }
        .toFloat()

    private fun definitionJson(rows: Int, columns: Int, edgeSeed: Long = 42) = """
        {
          "id": "test-puzzle",
          "title": "Test Puzzle",
          "category": "Test",
          "sourceImage": "source.png",
          "rows": $rows,
          "columns": $columns,
          "activeTextureMaxSize": 256,
          "thumbnailSize": 64,
          "edgeSeed": $edgeSeed
        }
    """.trimIndent()
}
