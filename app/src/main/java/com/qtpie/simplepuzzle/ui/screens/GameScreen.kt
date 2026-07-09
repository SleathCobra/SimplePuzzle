package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
                title = { Text(puzzle.name, fontWeight = FontWeight.Bold, color = Color.White) },
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Score: ${state.score}", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    if (state.combo > 1) {
                        Text("Combo x${state.combo}", color = Color(0xFFFFD700), fontWeight = FontWeight.Bold)
                    }
                }
                Text("${state.unlockedPieces.size} / ${puzzle.totalPieces} Pieces", color = Color.White)
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
                    modifier = Modifier.fillMaxSize(),
                    pieceBorderColor = Color.White.copy(alpha = 0.6f),
                    slotBorderColor = Color.White.copy(alpha = 0.1f)
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
            
            LinearProgressIndicator(
                progress = { state.unlockedPieces.size.toFloat() / puzzle.totalPieces },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF06D6A0),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
        }
    }
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
