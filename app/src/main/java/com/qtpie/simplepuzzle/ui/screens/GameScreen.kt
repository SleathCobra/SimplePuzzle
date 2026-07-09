package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.model.MathQuestion
import com.qtpie.simplepuzzle.model.PuzzleInfo
import com.qtpie.simplepuzzle.ui.components.JigsawBoard
import com.qtpie.simplepuzzle.viewmodel.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    onAnswerSelected: (Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val puzzle = state.currentPuzzle ?: return

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(puzzle.name.uppercase(), fontWeight = FontWeight.Black, color = Color.White, letterSpacing = 2.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    IconButton(onClick = onReset) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header: Score (Left), Timer & Coins (Right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Left: Score
                Column {
                    Text("SCORE", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${state.score}", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Black)
                    if (state.combo > 1) {
                        Surface(
                            color = Color(0xFFFFD700),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text("COMBO X${state.combo}", color = Color.Black, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }

                // Right: Timer & Coins
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(formatTime(state.timeElapsed), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "BEST: ${state.bestTime?.let { formatTime(it) } ?: "--:--"}",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💰", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${state.coins}", color = Color(0xFFFFD700), fontWeight = FontWeight.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                JigsawBoard(
                    rows = if (puzzle.totalPieces == 16) 4 else 5,
                    cols = if (puzzle.totalPieces == 16) 4 else 6,
                    visiblePieces = state.unlockedPieces,
                    shakeTrigger = state.shakeTrigger,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Image(
                        painter = painterResource(id = puzzle.imageResId),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (state.isGameOver) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.7f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("PUZZLE COMPLETE!", color = Color(0xFFFFD700), fontSize = 32.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = onBack) {
                                Text("Back to Gallery")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "${state.currentQuestion.problem} = ?",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chunks = state.currentQuestion.options.chunked(2)
                chunks.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { option ->
                            MathOptionButton(
                                text = "$option",
                                modifier = Modifier.weight(1f),
                                onClick = { onAnswerSelected(option) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            FlowingProgressBar(
                current = state.unlockedPieces.size,
                total = puzzle.totalPieces
            )
        }
    }
}

@Composable
fun FlowingProgressBar(current: Int, total: Int) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    val infiniteTransition = rememberInfiniteTransition(label = "flow")
    
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "offset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
            .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Progress Fill
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF00F2FE)),
                        start = Offset(offset, 0f),
                        end = Offset(offset + 400f, 400f)
                    )
                )
        )
        
        // Text Overlay
        Text(
            text = "$current / $total PIECES COLLECTED",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            letterSpacing = 1.sp
        )
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}

@Composable
fun MathOptionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.15f),
        tonalElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
