package com.qtpie.simplepuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.ui.components.JigsawBoard
import com.qtpie.simplepuzzle.ui.theme.SimplePuzzleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimplePuzzleTheme {
                PuzzleGameScreen()
            }
        }
    }
}

@Composable
fun PuzzleGameScreen() {
    var pieceCount by remember { mutableStateOf(15) }
    val rows = 4
    val cols = 4
    val totalPieces = rows * cols
    
    val visiblePieces = remember(pieceCount) {
        (0 until pieceCount).toSet()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(title = { Text("Jigsaw Puzzle") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (pieceCount < totalPieces) pieceCount++ 
            }) {
                Icon(Icons.Default.Add, contentDescription = "Add Piece")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$pieceCount / $totalPieces Pieces",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))

            JigsawBoard(
                rows = rows,
                cols = cols,
                visiblePieces = visiblePieces,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(Color.Black),
                pieceBorderColor = Color.White.copy(alpha = 0.6f),
                slotBorderColor = Color.White.copy(alpha = 0.1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.sweepGradient(
                                listOf(Color(0xFF6C63FF), Color(0xFFFF6584), Color(0xFFFFD166), Color(0xFF6C63FF))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "PICTURE",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            "PUZZLE",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 24.sp,
                            letterSpacing = 8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text("Completion Progress", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = pieceCount.toFloat(),
                onValueChange = { pieceCount = it.toInt() },
                valueRange = 0f..totalPieces.toFloat(),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "Tip: Add an Image() inside the content block to use real photos!",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SimplePuzzleTheme {
        PuzzleGameScreen()
    }
}
