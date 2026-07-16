package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.ui.components.TitlePuzzlePreview

@Composable
fun StartScreen(
    previewAssetRoots: List<String>,
    previewSeed: Long,
    graphicsQuality: GraphicsQuality,
    reducedMotion: Boolean,
    onPlayClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "play emphasis")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (reducedMotion) 1f else 1.045f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "play scale",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val compactHeight = maxHeight < 700.dp
        val previewSize = if (compactHeight) 190.dp else 260.dp
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = if (compactHeight) 18.dp else 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (compactHeight) 8.dp else 14.dp),
        ) {
            Text(
                text = "Jigsaw Math",
                color = Color(0xFFFFE066),
                fontSize = if (compactHeight) 44.sp else 56.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Solve. Unlock. Assemble.",
                color = Color.White.copy(alpha = 0.84f),
                fontSize = 18.sp,
            )

            Box(
                modifier = Modifier
                    .sizeIn(maxWidth = 320.dp)
                    .size(previewSize)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(8.dp),
            ) {
                if (previewAssetRoots.isNotEmpty()) {
                    TitlePuzzlePreview(
                        assetRoots = previewAssetRoots,
                        seed = previewSeed,
                        graphicsQuality = graphicsQuality,
                        reducedMotion = reducedMotion,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Image(
                        painter = painterResource(R.drawable.cosmic_journey_thumbnail),
                        contentDescription = "Jigsaw puzzle preview",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            Spacer(modifier = Modifier.height(if (compactHeight) 4.dp else 14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .width(200.dp)
                        .height(72.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                        .padding(6.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E7FF)),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp),
                ) {
                    Text(
                        text = "PLAY",
                        color = Color(0xFF5A4FCF),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    onClick = onSettingsClick,
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 4.dp,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = Color(0xFF5A4FCF),
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
        }
    }
}
