package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.qtpie.simplepuzzle.ui.components.JigsawBoard
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun StartScreen(onPlayClick: () -> Unit, onSettingsClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    // Animated puzzle preview
    val previewImageRes = remember { R.drawable.puzzle }
    var unlockedPieces by remember { mutableStateOf((0 until 16).filter { Random.nextBoolean() }.toSet()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1500)
            val total = 16
            val current = unlockedPieces.toMutableSet()
            if (current.size > 12 || (current.size > 4 && Random.nextBoolean())) {
                // Remove a random piece
                if (current.isNotEmpty()) {
                    current.remove(current.random())
                }
            } else {
                // Add a random piece
                val remaining = (0 until total).toSet() - current
                if (remaining.isNotEmpty()) {
                    current.add(remaining.random())
                }
            }
            unlockedPieces = current
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) {
            androidx.compose.material3.Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Jigsaw",
                color = Color(0xFFFF858D),
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = "Math",
                color = Color(0xFFFFE066),
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Text(
                text = "Solve. Unlock. Assemble.",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 3D Puzzle Preview instead of static image
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .padding(8.dp)
            ) {
                JigsawBoard(
                    rows = 4,
                    cols = 4,
                    visiblePieces = unlockedPieces,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = previewImageRes),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(60.dp))

            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .width(200.dp)
                    .height(80.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E7FF)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Text(
                    "PLAY",
                    color = Color(0xFF5A4FCF),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
