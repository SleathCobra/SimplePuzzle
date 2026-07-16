package com.qtpie.simplepuzzle.assets

import com.qtpie.simplepuzzle.core.model.assets.PUZZLE_FORMAT_VERSION
import com.qtpie.simplepuzzle.core.model.assets.PieceBounds
import com.qtpie.simplepuzzle.core.model.assets.PieceManifest
import com.qtpie.simplepuzzle.core.model.assets.PuzzleDefinition
import com.qtpie.simplepuzzle.core.model.assets.PuzzleManifest
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.math.sin
import kotlinx.serialization.encodeToString
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
class PuzzleAssetGenerator(
    private val json: Json = Json {
        prettyPrint = true
        prettyPrintIndent = "  "
        encodeDefaults = true
        explicitNulls = false
    },
) {
    fun generate(definitionFile: Path, outputDirectory: Path): PuzzleManifest {
        require(definitionFile.exists()) { "Puzzle definition does not exist: $definitionFile" }
        val definition = json.decodeFromString<PuzzleDefinition>(Files.readString(definitionFile))
        validate(definition)

        val sourceFile = definitionFile.parent.resolve(definition.sourceImage).normalize()
        require(sourceFile.exists()) { "Puzzle source image does not exist: $sourceFile" }
        val sourceBytes = Files.readAllBytes(sourceFile)
        val sourceImage = requireNotNull(ImageIO.read(sourceFile.toFile())) {
            "Puzzle source is not a supported image: $sourceFile"
        }

        outputDirectory.createDirectories()
        val activeTexture = scaleToFit(sourceImage, definition.activeTextureMaxSize)
        val thumbnail = scaleToFit(sourceImage, definition.thumbnailSize)
        val textureFile = outputDirectory.resolve(TEXTURE_FILE)
        val thumbnailFile = outputDirectory.resolve(THUMBNAIL_FILE)
        check(ImageIO.write(activeTexture, "png", textureFile.toFile())) { "No PNG writer is available." }
        check(ImageIO.write(thumbnail, "png", thumbnailFile.toFile())) { "No PNG writer is available." }

        val manifest = PuzzleManifest(
            formatVersion = PUZZLE_FORMAT_VERSION,
            puzzleId = definition.id,
            title = definition.title,
            category = definition.category,
            sourceHashSha256 = sha256(sourceBytes),
            textureFile = TEXTURE_FILE,
            thumbnailFile = THUMBNAIL_FILE,
            textureWidth = activeTexture.width,
            textureHeight = activeTexture.height,
            rows = definition.rows,
            columns = definition.columns,
            edgeSeed = definition.edgeSeed,
            pieces = generatePieces(definition.rows, definition.columns, definition.edgeSeed),
        )
        Files.writeString(outputDirectory.resolve(MANIFEST_FILE), json.encodeToString(manifest) + "\n")
        return manifest
    }

    private fun validate(definition: PuzzleDefinition) {
        require(definition.id.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*"))) {
            "Puzzle id must be lowercase kebab-case."
        }
        require(definition.title.isNotBlank()) { "Puzzle title cannot be blank." }
        require(definition.category.isNotBlank()) { "Puzzle category cannot be blank." }
        require(definition.rows > 0 && definition.columns > 0) { "Puzzle rows and columns must be positive." }
        require(definition.rows * definition.columns <= 512) { "Puzzle definitions are limited to 512 pieces." }
        require(definition.activeTextureMaxSize in 256..4096) { "Active texture size must be 256..4096." }
        require(definition.thumbnailSize in 64..1024) { "Thumbnail size must be 64..1024." }
    }

    private fun scaleToFit(source: BufferedImage, maximumSize: Int): BufferedImage {
        val scale = minOf(1.0, maximumSize.toDouble() / maxOf(source.width, source.height))
        val width = maxOf(1, (source.width * scale).toInt())
        val height = maxOf(1, (source.height * scale).toInt())
        val imageType = if (source.transparency == Transparency.OPAQUE) {
            BufferedImage.TYPE_INT_RGB
        } else {
            BufferedImage.TYPE_INT_ARGB
        }
        val target = BufferedImage(width, height, imageType)
        val graphics = target.createGraphics()
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            graphics.drawImage(source, 0, 0, width, height, null)
        } finally {
            graphics.dispose()
        }
        return target
    }

    private fun generatePieces(rows: Int, columns: Int, edgeSeed: Long): List<PieceManifest> {
        val pieceCount = rows * columns
        val revealRanks = IntArray(pieceCount)
        (0 until pieceCount)
            .sortedWith(compareBy<Int>({ mix(edgeSeed, REVEAL_SALT, it, 0) }, { it }))
            .forEachIndexed { rank, pieceIndex -> revealRanks[pieceIndex] = rank }
        val tabDepth = minOf(1f / columns, 1f / rows) * TAB_DEPTH_FRACTION

        return List(pieceCount) { index ->
            val row = index / columns
            val column = index % columns
            val polygon = buildPiecePolygon(row, column, rows, columns, edgeSeed, tabDepth)
            val triangleIndices = triangulate(polygon, "$row,$column")
            val left = polygon.minOf(Point::x)
            val top = polygon.minOf(Point::y)
            val right = polygon.maxOf(Point::x)
            val bottom = polygon.maxOf(Point::y)
            val coordinates = polygon.flatMap { point -> listOf(point.x, point.y) }
            PieceManifest(
                index = index,
                vertices = coordinates,
                textureCoordinates = coordinates,
                triangleIndices = triangleIndices,
                finalX = left,
                finalY = top,
                width = right - left,
                height = bottom - top,
                bounds = PieceBounds(left, top, right, bottom),
                revealOrder = revealRanks[index],
            )
        }
    }

    private fun buildPiecePolygon(
        row: Int,
        column: Int,
        rows: Int,
        columns: Int,
        edgeSeed: Long,
        tabDepth: Float,
    ): List<Point> {
        val points = mutableListOf<Point>()
        appendEdge(
            points,
            if (row == 0) {
                listOf(gridPoint(row, column, rows, columns), gridPoint(row, column + 1, rows, columns))
            } else {
                horizontalEdge(row, column, rows, columns, edgeSeed, tabDepth)
            },
        )
        appendEdge(
            points,
            if (column == columns - 1) {
                listOf(gridPoint(row, column + 1, rows, columns), gridPoint(row + 1, column + 1, rows, columns))
            } else {
                verticalEdge(row, column + 1, rows, columns, edgeSeed, tabDepth)
            },
        )
        appendEdge(
            points,
            if (row == rows - 1) {
                listOf(gridPoint(row + 1, column + 1, rows, columns), gridPoint(row + 1, column, rows, columns))
            } else {
                horizontalEdge(row + 1, column, rows, columns, edgeSeed, tabDepth).asReversed()
            },
        )
        appendEdge(
            points,
            if (column == 0) {
                listOf(gridPoint(row + 1, column, rows, columns), gridPoint(row, column, rows, columns))
            } else {
                verticalEdge(row, column, rows, columns, edgeSeed, tabDepth).asReversed()
            },
        )
        if (points.size > 1 && points.first() == points.last()) points.removeLast()
        require(points.size >= 3) { "Generated piece $row,$column has fewer than three vertices." }
        return points
    }

    private fun horizontalEdge(
        boundaryRow: Int,
        column: Int,
        rows: Int,
        columns: Int,
        edgeSeed: Long,
        tabDepth: Float,
    ): List<Point> {
        val start = column.toFloat() / columns
        val width = 1f / columns
        val baseline = boundaryRow.toFloat() / rows
        val direction = direction(edgeSeed, HORIZONTAL_SALT, boundaryRow, column)
        return edgeSamples.map { sample ->
            Point(
                x = start + width * sample.axis,
                y = baseline + direction * tabDepth * sample.normal,
            )
        }
    }

    private fun verticalEdge(
        row: Int,
        boundaryColumn: Int,
        rows: Int,
        columns: Int,
        edgeSeed: Long,
        tabDepth: Float,
    ): List<Point> {
        val baseline = boundaryColumn.toFloat() / columns
        val start = row.toFloat() / rows
        val height = 1f / rows
        val direction = direction(edgeSeed, VERTICAL_SALT, row, boundaryColumn)
        return edgeSamples.map { sample ->
            Point(
                x = baseline + direction * tabDepth * sample.normal,
                y = start + height * sample.axis,
            )
        }
    }

    private fun appendEdge(target: MutableList<Point>, edge: List<Point>) {
        edge.forEach { point ->
            if (target.lastOrNull() != point) target += point
        }
    }

    private fun gridPoint(row: Int, column: Int, rows: Int, columns: Int) = Point(
        x = column.toFloat() / columns,
        y = row.toFloat() / rows,
    )

    /** Ear-clips the precomputed concave piece polygon; Android never triangulates at runtime. */
    private fun triangulate(polygon: List<Point>, pieceLabel: String): List<Int> {
        val remaining = polygon.indices.toMutableList()
        val triangles = ArrayList<Int>((polygon.size - 2) * 3)
        val winding = if (signedArea(polygon) >= 0f) 1f else -1f

        while (remaining.size > 3) {
            var earFound = false
            for (position in remaining.indices) {
                val previous = remaining[(position - 1 + remaining.size) % remaining.size]
                val current = remaining[position]
                val next = remaining[(position + 1) % remaining.size]
                val a = polygon[previous]
                val b = polygon[current]
                val c = polygon[next]
                if (winding * cross(a, b, c) <= GEOMETRY_EPSILON) continue
                if (remaining.any { candidate ->
                        candidate != previous && candidate != current && candidate != next &&
                            pointInTriangle(polygon[candidate], a, b, c, winding)
                    }
                ) {
                    continue
                }

                triangles += previous
                triangles += current
                triangles += next
                remaining.removeAt(position)
                earFound = true
                break
            }
            if (!earFound) {
                val collinearPosition = remaining.indices.firstOrNull { position ->
                    val previous = remaining[(position - 1 + remaining.size) % remaining.size]
                    val current = remaining[position]
                    val next = remaining[(position + 1) % remaining.size]
                    absCross(polygon[previous], polygon[current], polygon[next]) <= GEOMETRY_EPSILON
                }
                if (collinearPosition != null) {
                    remaining.removeAt(collinearPosition)
                    earFound = true
                }
            }
            check(earFound) {
                "Generated a non-triangulatable jigsaw polygon for piece $pieceLabel " +
                    "with ${remaining.size} vertices remaining."
            }
        }
        triangles += remaining[0]
        triangles += remaining[1]
        triangles += remaining[2]
        return triangles
    }

    private fun signedArea(points: List<Point>): Float = points.indices.sumOf { index ->
        val current = points[index]
        val next = points[(index + 1) % points.size]
        (current.x * next.y - next.x * current.y).toDouble()
    }.toFloat() * 0.5f

    private fun cross(a: Point, b: Point, c: Point): Float =
        (b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x)

    private fun absCross(a: Point, b: Point, c: Point): Float = kotlin.math.abs(cross(a, b, c))

    private fun pointInTriangle(point: Point, a: Point, b: Point, c: Point, winding: Float): Boolean {
        val first = winding * cross(a, b, point)
        val second = winding * cross(b, c, point)
        val third = winding * cross(c, a, point)
        return first > GEOMETRY_EPSILON && second > GEOMETRY_EPSILON && third > GEOMETRY_EPSILON
    }

    private fun direction(seed: Long, salt: Long, primary: Int, secondary: Int): Float =
        if (mix(seed, salt, primary, secondary) and 1L == 0L) 1f else -1f

    private fun mix(seed: Long, salt: Long, primary: Int, secondary: Int): Long {
        var value = seed xor salt xor (primary.toLong() shl 32) xor secondary.toLong()
        value = (value xor (value ushr 33)) * -49064778989728563L
        value = (value xor (value ushr 33)) * -4265267296055464877L
        return value xor (value ushr 33)
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes)
        .joinToString(separator = "") { byte -> "%02x".format(byte) }

    private companion object {
        const val TAB_DEPTH_FRACTION = 0.20f
        const val GEOMETRY_EPSILON = 0.000001f
        const val HORIZONTAL_SALT = 0x13579BDFL
        const val VERTICAL_SALT = 0x2468ACE0L
        const val REVEAL_SALT = 0x5A17C0DEL
        const val TEXTURE_FILE = "texture.png"
        const val THUMBNAIL_FILE = "thumbnail.png"
        const val MANIFEST_FILE = "manifest.json"

        val edgeSamples = buildList {
            add(EdgeSample(0f, 0f))
            add(EdgeSample(0.34f, 0f))
            for (step in 1..5) {
                val phase = step.toFloat() / 6f
                add(EdgeSample(0.34f + 0.32f * phase, sin(Math.PI.toFloat() * phase)))
            }
            add(EdgeSample(0.66f, 0f))
            add(EdgeSample(1f, 0f))
        }
    }

    private data class Point(val x: Float, val y: Float)

    private data class EdgeSample(val axis: Float, val normal: Float)
}
