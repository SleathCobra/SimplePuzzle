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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
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

data class PieceState(
    val index: Int,
    val row: Int,
    val col: Int,
    val config: PieceConfig,
    var currentOffset: Offset = Offset.Zero,
    val targetPosition: Offset,
    var isPlaced: Boolean = false
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
    pieceBorderColor: Color = Color.White.copy(alpha = 0.4f),
    slotBorderColor: Color = Color.White.copy(alpha = 0.15f),
    shakeTrigger: Any? = null,
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
    LaunchedEffect(visiblePieces) {
        if (visiblePieces.size == totalPieces && totalPieces > 0) {
            onPuzzleComplete()
            // Haptic feedback on completion
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
                        .border(1.dp, slotBorderColor, shape)
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
                    .clip(shape)
                    .border(1.dp, pieceBorderColor, shape)

                // Add drag functionality if enabled
                val finalModifier = if (enableDrag) {
                    pieceModifier.pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragOffset += dragAmount
                            },
                            onDragEnd = {
                                // Snap check when drag ends
                                val targetX = (cellWidth * col).toPx()
                                val targetY = (cellHeight * row).toPx()
                                val currentX = dragOffset.x
                                val currentY = dragOffset.y

                                // Simple proximity check
                                if (kotlin.math.abs(currentX) < 20f &&
                                    kotlin.math.abs(currentY) < 20f) {
                                    // Snap to position!
                                    dragOffset = Offset.Zero
                                    vibrate(context, 30)
                                    onPiecePlaced(index)
                                }
                            }
                        )
                    }
                } else pieceModifier

                Box(modifier = finalModifier) {
                    // Content offset to show correct portion of the full image
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .layout { measurable, constraints ->
                                val boardWidthPx = boardWidth.roundToPx()
                                val boardHeightPx = boardHeight.roundToPx()
                                val placeable = measurable.measure(
                                    Constraints.fixed(boardWidthPx, boardHeightPx)
                                )

                                // Offset content so the correct section shows in this piece
                                // Plus drag offset compensation
                                val dragX = dragOffset.x.roundToInt()
                                val dragY = dragOffset.y.roundToInt()
                                val offsetX = -(cellWidth.roundToPx() * col) + paddingDp.roundToPx() + dragX
                                val offsetY = -(cellHeight.roundToPx() * row) + paddingDp.roundToPx() + dragY

                                layout(constraints.maxWidth, constraints.maxHeight) {
                                    placeable.placeRelative(offsetX, offsetY)
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

// ─── Extension: Convert Dp to Px for drag offset ─────────────

private fun Float.dpToPx(): Float {
    return this * android.content.res.Resources.getSystem().displayMetrics.density
}

private fun Dp.toPx(): Float {
    return this.value * android.content.res.Resources.getSystem().displayMetrics.density
}