package com.qtpie.simplepuzzle.assets

import java.awt.Color
import java.awt.image.BufferedImage
import java.nio.file.Files
import javax.imageio.ImageIO
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class PuzzleCatalogGeneratorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `catalog output is deterministic and derived from generated packages`() {
        val root = temporaryFolder.root.toPath()
        val puzzlesRoot = root.resolve("puzzles")
        val definitionRoot = puzzlesRoot.resolve("test-puzzle")
        Files.createDirectories(definitionRoot)
        writeSource(definitionRoot.resolve("source.png"))
        Files.writeString(definitionRoot.resolve("puzzle.json"), puzzleDefinition())
        PuzzleAssetGenerator().generate(
            definitionRoot.resolve("puzzle.json"),
            root.resolve("app/src/main/assets/puzzles/test-puzzle"),
        )
        val catalogDefinition = puzzlesRoot.resolve("catalog.json")
        Files.writeString(catalogDefinition, catalogDefinition())
        val first = root.resolve("first/catalog.json")
        val second = root.resolve("second/catalog.json")

        val firstCatalog = PuzzleCatalogGenerator().generate(catalogDefinition, root, first)
        val secondCatalog = PuzzleCatalogGenerator().generate(catalogDefinition, root, second)

        assertEquals(firstCatalog, secondCatalog)
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second))
        assertEquals("test-puzzle", firstCatalog.puzzles.single().puzzleId)
        assertEquals(6, firstCatalog.puzzles.single().pieceCount)
        assertEquals("puzzles/test-puzzle", firstCatalog.puzzles.single().generatedAssetRoot)
    }

    @Test
    fun `missing generated package fails with an actionable message`() {
        val root = temporaryFolder.root.toPath()
        val definitionRoot = root.resolve("puzzles/test-puzzle")
        Files.createDirectories(definitionRoot)
        writeSource(definitionRoot.resolve("source.png"))
        Files.writeString(definitionRoot.resolve("puzzle.json"), puzzleDefinition())
        val catalogDefinition = root.resolve("puzzles/catalog.json")
        Files.writeString(catalogDefinition, catalogDefinition())

        val error = assertThrows(IllegalArgumentException::class.java) {
            PuzzleCatalogGenerator().generate(catalogDefinition, root, root.resolve("output/catalog.json"))
        }

        assertEquals(true, error.message?.startsWith("Generated puzzle manifest does not exist:"))
    }

    private fun writeSource(path: java.nio.file.Path) {
        val image = BufferedImage(32, 24, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try {
            graphics.color = Color(35, 55, 180)
            graphics.fillRect(0, 0, image.width, image.height)
        } finally {
            graphics.dispose()
        }
        ImageIO.write(image, "png", path.toFile())
    }

    private fun puzzleDefinition() = """
        {
          "id": "test-puzzle",
          "title": "Test Puzzle",
          "category": "Test",
          "sourceImage": "source.png",
          "rows": 2,
          "columns": 3,
          "activeTextureMaxSize": 256,
          "thumbnailSize": 64,
          "edgeSeed": 42
        }
    """.trimIndent()

    private fun catalogDefinition() = """
        {
          "formatVersion": 1,
          "puzzles": [
            {
              "definitionFile": "test-puzzle/puzzle.json",
              "generatedAssetRoot": "puzzles/test-puzzle",
              "thumbnailResourceName": "test_puzzle_thumbnail",
              "available": true,
              "initiallyLocked": false
            }
          ]
        }
    """.trimIndent()
}
