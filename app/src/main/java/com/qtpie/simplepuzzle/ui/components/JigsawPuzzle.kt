package com.qtpie.simplepuzzle.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.min
import kotlin.random.Random

/**
 * Defines the type of connection for a side of a jigsaw piece.
 */
enum class SideType { 
    /** A straight edge, used for the outer boundaries of the board. */
    Flat, 
    /** An outward protruding tab. */
    Tab, 
    /** An inward indentation (blank) that receives a tab. */
    Blank 
}

/**
 * Configuration for the four sides of a single jigsaw piece.
 * 
 * @property top Connection type for the top edge.
 * @property bottom Connection type for the bottom edge.
 * @property left Connection type for the left edge.
 * @property right Connection type for the right edge.
 */
data class PieceConfig(
    val top: SideType,
    val bottom: SideType,
    val left: SideType,
    val right: SideType
)

/**
 * A custom [Shape] that draws a jigsaw puzzle piece based on a [PieceConfig].
 * 
 * This shape handles the drawing of cubic bezier curves for tabs and blanks,
 * ensuring they are centered on each side.
 *
 * @param config The connection types for each side of the piece.
 * @param tabSize The radius/size of the jigsaw tab in pixels.
 * @param padding The internal padding in pixels allowed for the tabs to protrude.
 */
class JigsawShape(
    private val config: PieceConfig,
    private val tabSize: Float,
    private val padding: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val w = size.width - 2 * padding
            val h = size.height - 2 * padding
            
            val leftX = padding
            val rightX = padding + w
            val topY = padding
            val bottomY = padding + h

            moveTo(leftX, topY)
            
            // Top side
            drawSide(this, Offset(leftX, topY), Offset(rightX, topY), config.top, tabSize, horizontal = true)
            
            // Right side
            drawSide(this, Offset(rightX, topY), Offset(rightX, bottomY), config.right, tabSize, horizontal = false)
            
            // Bottom side (backwards)
            drawSide(this, Offset(rightX, bottomY), Offset(leftX, bottomY), config.bottom, tabSize, horizontal = true)
            
            // Left side (backwards)
            drawSide(this, Offset(leftX, bottomY), Offset(leftX, topY), config.left, tabSize, horizontal = false)
            
            close()
        }
        return Outline.Generic(path)
    }

    private fun drawSide(path: Path, start: Offset, end: Offset, type: SideType, tabSize: Float, horizontal: Boolean) {
        if (type == SideType.Flat) {
            path.lineTo(end.x, end.y)
            return
        }

        val midX = (start.x + end.x) / 2
        val midY = (start.y + end.y) / 2
        
        val direction = if (type == SideType.Tab) 1f else -1f
        
        if (horizontal) {
            val normal = if (end.x > start.x) -1f else 1f
            val actualDirection = direction * normal
            
            path.lineTo(midX - tabSize, start.y)
            path.cubicTo(
                midX - tabSize, start.y + tabSize * actualDirection,
                midX + tabSize, start.y + tabSize * actualDirection,
                midX + tabSize, start.y
            )
            path.lineTo(end.x, end.y)
        } else {
            val normal = if (end.y > start.y) 1f else -1f
            val actualDirection = direction * normal
            
            path.lineTo(start.x, midY - tabSize)
            path.cubicTo(
                start.x + tabSize * actualDirection, midY - tabSize,
                start.x + tabSize * actualDirection, midY + tabSize,
                start.x, midY + tabSize
            )
            path.lineTo(end.x, end.y)
        }
    }
}

/**
 * A Jigsaw Puzzle Board composable that displays content clipped into jigsaw pieces.
 *
 * This component handles the complex task of splitting a single piece of content (like an image)
 * into a grid of interlocking jigsaw pieces. It supports partial completion by allowing you
 * to specify which pieces are currently visible.
 *
 * ### Example Usage:
 * ```kotlin
 * JigsawBoard(
 *     rows = 4,
 *     cols = 4,
 *     visiblePieces = setOf(0, 1, 5, 10), // Indices of pieces to show
 *     modifier = Modifier.fillMaxWidth().aspectRatio(1f)
 * ) {
 *     // Any content that should be "puzzled"
 *     Image(
 *         painter = painterResource(id = R.drawable.my_puzzle_image),
 *         contentDescription = null,
 *         modifier = Modifier.fillMaxSize(),
 *         contentScale = ContentScale.Crop
 *     )
 * }
 * ```
 *
 * @param rows Number of rows in the puzzle grid.
 * @param cols Number of columns in the puzzle grid.
 * @param visiblePieces A [Set] of integers representing the indices (0..rows*cols-1) 
 *                      of pieces that should be rendered.
 * @param modifier The modifier to be applied to the board container.
 * @param emptyColor The background color of the board where pieces are missing.
 * @param pieceBorderColor The color of the stroke around visible jigsaw pieces.
 * @param slotBorderColor The color of the faint outline shown for missing pieces.
 * @param shakeTrigger A key to trigger a shake animation on the board.
 * @param content The composable content to be split into puzzle pieces.
 */
@Composable
fun JigsawBoard(
    rows: Int,
    cols: Int,
    visiblePieces: Set<Int>,
    modifier: Modifier = Modifier,
    emptyColor: Color = Color(0xFF1A1A2E),
    pieceBorderColor: Color = Color.White.copy(alpha = 0.4f),
    slotBorderColor: Color = Color.White.copy(alpha = 0.15f),
    shakeTrigger: Any? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val configs = remember(rows, cols) {
        generateConfigs(rows, cols)
    }

    // Shake Animation Logic
    var shakeValue by remember { mutableStateOf(0f) }
    val shakeOffset by animateFloatAsState(
        targetValue = shakeValue,
        animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium)
    )

    LaunchedEffect(shakeTrigger) {
        if (shakeTrigger != null) {
            shakeValue = 10f
            kotlinx.coroutines.delay(100)
            shakeValue = -10f
            kotlinx.coroutines.delay(100)
            shakeValue = 0f
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer { translationX = shakeOffset }
    ) {
        val boardWidth = maxWidth
        val boardHeight = maxHeight
        
        val cellWidth = boardWidth / cols
        val cellHeight = boardHeight / rows
        
        val tabSizeDp = min(cellWidth.value, cellHeight.value).dp * 0.18f
        val paddingDp = tabSizeDp
        
        val pieceWidthDp = cellWidth + paddingDp * 2
        val pieceHeightDp = cellHeight + paddingDp * 2

        // Base background
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(emptyColor)
        )

        val density = LocalDensity.current
        val tabSizePx = with(density) { tabSizeDp.toPx() }
        val paddingPx = with(density) { paddingDp.toPx() }

        // Draw Slots (Empty outlines)
        configs.forEachIndexed { index, config ->
            val row = index / cols
            val col = index % cols
            val shape = remember(config, tabSizePx, paddingPx) { JigsawShape(config, tabSizePx, paddingPx) }

            if (!visiblePieces.contains(index)) {
                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * col - paddingDp, y = cellHeight * row - paddingDp)
                        .size(width = pieceWidthDp, height = pieceHeightDp)
                        .border(1.dp, slotBorderColor, shape)
                )
            }
        }

        // Draw Visible Pieces
        configs.forEachIndexed { index, config ->
            val row = index / cols
            val col = index % cols
            val shape = remember(config, tabSizePx, paddingPx) { JigsawShape(config, tabSizePx, paddingPx) }

            AnimatedVisibility(
                visible = visiblePieces.contains(index),
                enter = fadeIn() + scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
                )
            ) {
                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * col - paddingDp, y = cellHeight * row - paddingDp)
                        .size(width = pieceWidthDp, height = pieceHeightDp)
                        .clip(shape)
                        .border(1.dp, pieceBorderColor, shape)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layout { measurable, constraints ->
                                // Force measurement at the full board size to ensure content isn't compressed
                                val boardWidthPx = boardWidth.roundToPx()
                                val boardHeightPx = boardHeight.roundToPx()
                                val placeable = measurable.measure(
                                    Constraints.fixed(boardWidthPx, boardHeightPx)
                                )
                                
                                // Position the content within the piece by applying a negative offset
                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    val x = -cellWidth.roundToPx() * col + paddingDp.roundToPx()
                                    val y = -cellHeight.roundToPx() * row + paddingDp.roundToPx()
                                    placeable.placeRelative(x, y)
                                }
                            }
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

/**
 * Generates an interlocking grid of [PieceConfig]s.
 * 
 * Ensures that if piece (r, c) has a [SideType.Tab] on its right, 
 * piece (r, c+1) will have a [SideType.Blank] on its left, and so on.
 */
private fun generateConfigs(rows: Int, cols: Int): List<PieceConfig> {
    val grid = Array(rows) { arrayOfNulls<PieceConfig>(cols) }
    val random = Random(42) // Fixed seed for consistent board generation

    for (r in 0 until rows) {
        for (c in 0 until cols) {
            val top = if (r == 0) SideType.Flat else {
                if (grid[r - 1][c]!!.bottom == SideType.Tab) SideType.Blank else SideType.Tab
            }
            val left = if (c == 0) SideType.Flat else {
                if (grid[r][c - 1]!!.right == SideType.Tab) SideType.Blank else SideType.Tab
            }
            val bottom = if (r == rows - 1) SideType.Flat else {
                if (random.nextBoolean()) SideType.Tab else SideType.Blank
            }
            val right = if (c == cols - 1) SideType.Flat else {
                if (random.nextBoolean()) SideType.Tab else SideType.Blank
            }
            grid[r][c] = PieceConfig(top, bottom, left, right)
        }
    }
    return grid.flatMap { it.toList() }.filterNotNull()
}
