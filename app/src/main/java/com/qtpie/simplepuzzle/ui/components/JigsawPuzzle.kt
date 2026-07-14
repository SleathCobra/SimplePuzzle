package com.qtpie.simplepuzzle.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.qtpie.simplepuzzle.viewmodel.SoundEffect
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

// ─── Enums and Data Classes ──────────────────────────────────

enum class SideType { Flat, Tab, Blank }

data class PieceConfig(
    val top: SideType,
    val bottom: SideType,
    val left: SideType,
    val right: SideType
)

// ─── Shape ────────────────────────────────────────────────────

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
            drawSide(this, Offset(leftX, topY), Offset(rightX, topY), config.top, tabSize, horizontal = true)
            drawSide(this, Offset(rightX, topY), Offset(rightX, bottomY), config.right, tabSize, horizontal = false)
            drawSide(this, Offset(rightX, bottomY), Offset(leftX, bottomY), config.bottom, tabSize, horizontal = true)
            drawSide(this, Offset(leftX, bottomY), Offset(leftX, topY), config.left, tabSize, horizontal = false)
            close()
        }
        return Outline.Generic(path)
    }

    private fun drawSide(
        path: Path,
        start: Offset,
        end: Offset,
        type: SideType,
        tabSize: Float,
        horizontal: Boolean
    ) {
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

// ─── Main Composable ──────────────────────────────────────────

@Composable
fun JigsawBoard(
    rows: Int,
    cols: Int,
    visiblePieces: Set<Int>,
    modifier: Modifier = Modifier,
    emptyColor: Color = Color(0xFF1A1A2E),
    shakeTrigger: Any? = null,
    onSound: (SoundEffect) -> Unit = {},
    onPiecePlaced: (Int) -> Unit = {},
    onPuzzleComplete: () -> Unit = {},
    enableDrag: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val configs = remember(rows, cols) { generateConfigs(rows, cols) }
    val totalPieces = rows * cols

    // ── Shake Animation ──
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

    // ── Completion Detection ──
    var previousVisibleCount by remember { mutableStateOf(visiblePieces.size) }
    
    LaunchedEffect(visiblePieces) {
        val currentCount = visiblePieces.size
        if (currentCount > previousVisibleCount) {
            onSound(SoundEffect.PIECE_PLACED)
        } else if (currentCount < previousVisibleCount) {
            onSound(SoundEffect.PIECE_REMOVED)
        }
        previousVisibleCount = currentCount

        if (visiblePieces.size == totalPieces && totalPieces > 0) {
            onPuzzleComplete()
            vibrate(context, 100)
        }
    }

    // ── Board Layout ──
    BoxWithConstraints(
        modifier = modifier
            .graphicsLayer { translationX = shakeOffset }
    ) {
        val boardWidth = maxWidth
        val boardHeight = maxHeight

        val cellWidth = boardWidth / cols
        val cellHeight = boardHeight / rows

        val tabSizeDp = min(cellWidth.value, cellHeight.value).dp * 0.18f
        val paddingDp = tabSizeDp * 1.2f

        val pieceWidthDp = cellWidth + paddingDp * 2
        val pieceHeightDp = cellHeight + paddingDp * 2

        // ── Background ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(emptyColor)
        )

        val density = LocalDensity.current
        val tabSizePx = with(density) { tabSizeDp.toPx() }
        val paddingPx = with(density) { paddingDp.toPx() }

        // ── Draw Empty Slots ──
        configs.forEachIndexed { index, config ->
            val row = index / cols
            val col = index % cols
            val shape = remember(config, tabSizePx, paddingPx) {
                JigsawShape(config, tabSizePx, paddingPx)
            }

            if (!visiblePieces.contains(index)) {
                Box(
                    modifier = Modifier
                        .offset(x = cellWidth * col - paddingDp, y = cellHeight * row - paddingDp)
                        .size(width = pieceWidthDp, height = pieceHeightDp)
                        .background(Color.Black.copy(alpha = 0.25f), shape)
                        .border(
                            width = 4.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Black.copy(alpha = 0.8f),
                                    Color.Black.copy(alpha = 0.4f),
                                    Color.White.copy(alpha = 0.1f),
                                    Color.White.copy(alpha = 0.3f)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            ),
                            shape = shape
                        )
                )
            }
        }

        // ── Draw Visible Pieces ──
        configs.forEachIndexed { index, config ->
            val row = index / cols
            val col = index % cols
            val shape = remember(config, tabSizePx, paddingPx) {
                JigsawShape(config, tabSizePx, paddingPx)
            }

            // Track drag state for this piece
            var dragOffset by remember { mutableStateOf(Offset.Zero) }

            AnimatedVisibility(
                visible = visiblePieces.contains(index),
                enter = fadeIn() + scaleIn(
                    initialScale = 0.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            ) {
                // For each piece, we need to position it with offset
                val pieceModifier = Modifier
                    .offset(
                        x = cellWidth * col - paddingDp + dragOffset.x.dp,
                        y = cellHeight * row - paddingDp + dragOffset.y.dp
                    )
                    .size(width = pieceWidthDp, height = pieceHeightDp)
                    .shadow(
                        elevation = 10.dp,
                        shape = shape,
                        ambientColor = Color.Black.copy(alpha = 0.6f),
                        spotColor = Color.Black
                    )
                    .clip(shape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.95f),
                                Color.White.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            start = Offset.Zero,
                            end = Offset.Infinite
                        ),
                        shape = shape
                    )

                // Add drag functionality if enabled
                val finalModifier = if (enableDrag) {
                    pieceModifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                val currentX = dragOffset.x
                                val currentY = dragOffset.y

                                if (kotlin.math.abs(currentX) < 20f &&
                                    kotlin.math.abs(currentY) < 20f) {
                                    dragOffset = Offset.Zero
                                    vibrate(context, 30)
                                    onPiecePlaced(index)
                                }
                            }
                        )
                    }
                } else pieceModifier

                Box(modifier = finalModifier) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layout { measurable, constraints ->
                                val boardWidthPx = boardWidth.roundToPx()
                                val boardHeightPx = boardHeight.roundToPx()
                                val placeable = measurable.measure(
                                    Constraints.fixed(boardWidthPx, boardHeightPx)
                                )

                                val dragX = dragOffset.x.roundToInt()
                                val dragY = dragOffset.y.roundToInt()
                                val offsetX = -(cellWidth.roundToPx() * col) + paddingDp.roundToPx() + dragX
                                val offsetY = -(cellHeight.roundToPx() * row) + paddingDp.roundToPx() + dragY

                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.placeRelative(offsetX, offsetY)
                                }
                            }
                            .drawWithContent {
                                drawContent()
                                val path = (shape.createOutline(this.size, this.layoutDirection, this) as Outline.Generic).path
                                
                                drawPath(
                                    path = path,
                                    brush = Brush.linearGradient(
                                        0.0f to Color.White.copy(alpha = 0.6f),
                                        0.4f to Color.Transparent,
                                        start = Offset.Zero,
                                        end = Offset(this.size.width, this.size.height)
                                    ),
                                    style = Stroke(width = 5.dp.toPx())
                                )

                                drawPath(
                                    path = path,
                                    brush = Brush.linearGradient(
                                        0.6f to Color.Transparent,
                                        1.0f to Color.Black.copy(alpha = 0.6f),
                                        start = Offset.Zero,
                                        end = Offset(this.size.width, this.size.height)
                                    ),
                                    style = Stroke(width = 5.dp.toPx())
                                )
                                
                                drawPath(
                                    path = path,
                                    color = Color.White.copy(alpha = 0.3f),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                    ) {
                        content()
                    }
                }
            }
        }
    }
}

// ─── Helpers ────────────────────────────────────────────────────

private fun generateConfigs(rows: Int, cols: Int): List<PieceConfig> {
    val grid = Array(rows) { arrayOfNulls<PieceConfig>(cols) }
    val random = Random(42)

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

private fun vibrate(context: Context, duration: Long) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    vibrator?.let {
        if (it.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
    }
}
