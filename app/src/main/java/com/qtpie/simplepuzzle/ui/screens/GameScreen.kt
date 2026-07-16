package com.qtpie.simplepuzzle.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.model.MathQuestion
import com.qtpie.simplepuzzle.model.PuzzleInfo
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.ui.components.GdxPuzzleBoard
import com.qtpie.simplepuzzle.viewmodel.GameUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    totalCoins: Int,
    onAnswerSelected: (Int) -> Unit,
    graphicsQuality: GraphicsQuality,
    reducedMotion: Boolean,
    onRevealFinished: (Int) -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

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
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("💰", fontSize = 16.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("$totalCoins", color = Color(0xFFFFD700), fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(onClick = onReset) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color.White)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .navigationBarsPadding() // Protect against system navbar
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp) // Extra safety margin at the bottom
        ) {
            val availableHeight = maxHeight
            val isSmallScreen = availableHeight < 650.dp

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // 1. Header Area
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Score
                    Column {
                        Text("SCORE", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${state.score}", color = Color.White, fontSize = if (isSmallScreen) 24.sp else 32.sp, fontWeight = FontWeight.Black)
                        Box(modifier = Modifier.height(if (isSmallScreen) 24.dp else 32.dp)) {
                            if (state.combo > 1) {
                                Surface(
                                    color = Color(0xFFFFD700),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        "COMBO X${state.combo}", 
                                        color = Color.Black, 
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), 
                                        fontWeight = FontWeight.Bold, 
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }
                    // Timer
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(formatTime(state.timeElapsed), color = Color.White, fontSize = if (isSmallScreen) 18.sp else 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "BEST: ${state.bestTime?.let { formatTime(it) } ?: "--:--"}",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
                }

                // 2. Puzzle Area (Flexible)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        GdxPuzzleBoard(
                            visiblePieces = state.unlockedPieces,
                            revealingPiece = state.revealingPiece,
                            graphicsQuality = graphicsQuality,
                            reducedMotion = reducedMotion,
                            incorrectFeedbackTrigger = state.shakeTrigger,
                            isCompleted = state.isGameOver,
                            onRevealFinished = onRevealFinished,
                            modifier = Modifier.fillMaxSize(),
                        )
                        
                        if (state.isGameOver) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("PUZZLE COMPLETE!", color = Color(0xFFFFD700), fontSize = if (isSmallScreen) 20.sp else 28.sp, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(onClick = onBack) {
                                        Text("Back to Gallery")
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Bottom Area
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(if (isSmallScreen) 12.dp else 16.dp)
                ) {
                    FlowingProgressBar(
                        current = state.unlockedPieces.size,
                        total = puzzle.totalPieces
                    )

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSmallScreen) 50.dp else 64.dp),
                        shape = RoundedCornerShape(if (isSmallScreen) 12.dp else 16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "${state.currentQuestion.problem} = ?",
                                fontSize = if (isSmallScreen) 22.sp else 28.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Answer Options - Horizontal List of 4 Squares
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        state.currentQuestion.options.forEach { option ->
                            MathOptionButton(
                                text = "$option",
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f),
                                onClick = { onAnswerSelected(option) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlowingProgressBar(current: Int, total: Int) {
    val progress = if (total > 0) current.toFloat() / total else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 350),
        label = "puzzle-progress",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Progress Fill
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF00F2FE), Color(0xFF4FACFE), Color(0xFF7B61FF)),
                    )
                )
        )
        
        // Text Overlay
        Text(
            text = "$current / $total PIECES",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 11.sp,
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
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.15f),
        tonalElevation = 8.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
