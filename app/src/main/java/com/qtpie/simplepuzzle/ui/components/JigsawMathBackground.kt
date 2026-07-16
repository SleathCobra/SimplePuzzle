package com.qtpie.simplepuzzle.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path

/**
 * Lightweight application backdrop drawn without decoding a full-screen bitmap.
 * Brushes and paths are rebuilt only when the available size changes.
 */
@Composable
fun JigsawMathBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawWithCache {
            val mainGradient = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF273EC7),
                    Color(0xFF5145D7),
                    Color(0xFF8A4BCB),
                ),
                start = Offset.Zero,
                end = Offset(size.width, size.height * 0.82f),
            )
            val lowerGradient = Brush.linearGradient(
                colors = listOf(Color(0xFFF73570), Color(0xFFFF6477)),
                start = Offset(0f, size.height * 0.62f),
                end = Offset(size.width, size.height),
            )
            val lowerWave = Path().apply {
                moveTo(0f, size.height * 0.66f)
                quadraticTo(
                    size.width * 0.30f,
                    size.height * 0.73f,
                    size.width * 0.58f,
                    size.height * 0.67f,
                )
                quadraticTo(
                    size.width * 0.83f,
                    size.height * 0.61f,
                    size.width,
                    size.height * 0.64f,
                )
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            val triangle = Path().apply {
                moveTo(size.width * 0.13f, size.height * 0.53f)
                lineTo(size.width * 0.16f, size.height * 0.49f)
                lineTo(size.width * 0.19f, size.height * 0.54f)
                close()
            }

            onDrawBehind {
                drawRect(brush = mainGradient)
                drawRoundRect(
                    color = Color(0xFFB7C1FF).copy(alpha = 0.16f),
                    topLeft = Offset(size.width * 0.80f, -size.height * 0.04f),
                    size = Size(size.width * 0.30f, size.height * 0.16f),
                    cornerRadius = CornerRadius(size.minDimension * 0.08f),
                )
                drawCircle(
                    color = Color(0xFF9EA9FF).copy(alpha = 0.24f),
                    radius = size.minDimension * 0.075f,
                    center = Offset(0f, size.height * 0.49f),
                )
                drawCircle(
                    color = Color(0xFFBA74E7).copy(alpha = 0.55f),
                    radius = size.minDimension * 0.025f,
                    center = Offset(size.width * 0.13f, size.height * 0.39f),
                )
                drawCircle(
                    color = Color(0xFFFF79B5).copy(alpha = 0.58f),
                    radius = size.minDimension * 0.022f,
                    center = Offset(size.width * 0.17f, size.height * 0.40f),
                )
                drawCircle(
                    color = Color(0xFFD6A5FF).copy(alpha = 0.45f),
                    radius = size.minDimension * 0.013f,
                    center = Offset(size.width * 0.58f, size.height * 0.37f),
                )
                drawPath(
                    path = triangle,
                    color = Color(0xFFA9B7FF).copy(alpha = 0.45f),
                )
                drawPath(path = lowerWave, brush = lowerGradient)
                drawCircle(
                    color = Color(0xFFD9B8FF).copy(alpha = 0.35f),
                    radius = size.minDimension * 0.025f,
                    center = Offset(size.width * 0.13f, size.height * 0.68f),
                )
            }
        },
    )
}
