package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.R

@Composable
fun StartScreen(onPlayClick: () -> Unit, onSettingsClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartbeat")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

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

            // Placeholder for the central image with puzzle pieces
            Image(
                painter = painterResource(id = R.drawable.puzzle), // Should ideally be a specialized graphic
                contentDescription = null,
                modifier = Modifier
                    .size(280.dp)
                    .padding(20.dp),
                contentScale = ContentScale.Fit
            )

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
