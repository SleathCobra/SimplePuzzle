package com.qtpie.simplepuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.ui.components.JigsawBoard
import com.qtpie.simplepuzzle.ui.theme.SimplePuzzleTheme
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimplePuzzleTheme {
                MathPuzzleGame()
            }
        }
    }
}

data class MathQuestion(
    val problem: String,
    val answer: Int,
    val options: List<Int>
)

fun generateMathQuestion(): MathQuestion {
    val a = Random.nextInt(10, 50)
    val b = Random.nextInt(10, 50)
    val op = if (Random.nextBoolean()) "+" else "-"
    val answer = if (op == "+") a + b else a - b
    
    val options = mutableSetOf(answer)
    while (options.size < 4) {
        val wrong = answer + Random.nextInt(-10, 11)
        if (wrong != answer) options.add(wrong)
    }
    return MathQuestion("$a $op $b", answer, options.toList().shuffled())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathPuzzleGame() {
    val rows = 4
    val cols = 4
    val totalPieces = rows * cols
    
    var unlockedPieces by remember { mutableStateOf(setOf<Int>()) }
    var currentQuestion by remember { mutableStateOf(generateMathQuestion()) }
    var score by remember { mutableStateOf(0) }
    var shakeTrigger by remember { mutableStateOf(0) }

    fun unlockRandomPiece() {
        val remaining = (0 until totalPieces).toSet() - unlockedPieces
        if (remaining.isNotEmpty()) {
            val randomPiece = remaining.random()
            unlockedPieces = unlockedPieces + randomPiece
            shakeTrigger++
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Math Jigsaw", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        unlockedPieces = emptySet()
                        score = 0
                        currentQuestion = generateMathQuestion()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E))))
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Score: $score", color = Color.White, style = MaterialTheme.typography.titleLarge)
                Text("${unlockedPieces.size} / $totalPieces Pieces", color = Color.White)
            }

            Spacer(modifier = Modifier.height(20.dp))

            JigsawBoard(
                rows = rows,
                cols = cols,
                visiblePieces = unlockedPieces,
                shakeTrigger = shakeTrigger,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f)),
                pieceBorderColor = Color.White.copy(alpha = 0.6f),
                slotBorderColor = Color.White.copy(alpha = 0.1f)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.puzzle),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
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
                        text = "${currentQuestion.problem} = ?",
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val chunks = currentQuestion.options.chunked(2)
                chunks.forEach { rowOptions ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowOptions.forEach { option ->
                            MathOptionButton(
                                text = "[$option]",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    if (option == currentQuestion.answer) {
                                        score += 10
                                        unlockRandomPiece()
                                        currentQuestion = generateMathQuestion()
                                    } else {
                                        shakeTrigger++
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            
            LinearProgressIndicator(
                progress = { unlockedPieces.size.toFloat() / totalPieces },
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
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MathGamePreview() {
    SimplePuzzleTheme {
        MathPuzzleGame()
    }
}
