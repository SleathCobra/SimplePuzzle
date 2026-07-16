package com.qtpie.simplepuzzle.ui.screens

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.model.MathQuestion
import com.qtpie.simplepuzzle.model.PuzzleInfo
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.core.model.BoardMutationId
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import com.qtpie.simplepuzzle.core.model.ModeOutcome
import com.qtpie.simplepuzzle.ui.components.GdxPuzzleBoard
import com.qtpie.simplepuzzle.viewmodel.GameFeedbackEvent
import com.qtpie.simplepuzzle.viewmodel.GameFeedbackType
import com.qtpie.simplepuzzle.viewmodel.GameUiState
import com.qtpie.simplepuzzle.viewmodel.ModeTimerUiState
import com.qtpie.simplepuzzle.viewmodel.TimerUiAnchor
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    state: GameUiState,
    timerUiState: ModeTimerUiState,
    feedbackEvents: SharedFlow<GameFeedbackEvent>,
    totalCoins: Int,
    onAnswerSelected: (Int) -> Unit,
    graphicsQuality: GraphicsQuality,
    reducedMotion: Boolean,
    onRevealFinished: (BoardMutationId) -> Unit,
    onReset: () -> Unit,
    onChooseMode: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)

    val puzzle = state.currentPuzzle ?: return
    val feedbackAlpha = remember { Animatable(0f) }
    var feedback by remember { mutableStateOf<GameFeedbackEvent?>(null) }
    LaunchedEffect(feedbackEvents) {
        feedbackEvents.collectLatest { event ->
            feedback = event
            feedbackAlpha.snapTo(1f)
            feedbackAlpha.animateTo(0f, tween(durationMillis = if (reducedMotion) 700 else 1_300))
        }
    }

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
                ModeHud(
                    state = state,
                    timers = timerUiState,
                    compact = isSmallScreen,
                )

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
                            assetRoot = puzzle.assetRoot,
                            visiblePieces = state.unlockedPieces,
                            sessionGeneration = state.sessionGeneration,
                            pendingBoardMutation = state.pendingBoardMutation,
                            graphicsQuality = graphicsQuality,
                            reducedMotion = reducedMotion,
                            incorrectFeedbackTrigger = state.shakeTrigger,
                            isCompleted = state.isGameOver,
                            onMutationFinished = onRevealFinished,
                            modifier = Modifier.fillMaxSize(),
                        )
                        
                        if (state.isGameOver) {
                            ModeResultOverlay(
                                state = state,
                                onRetry = onReset,
                                onChooseMode = onChooseMode,
                                onBackToGallery = onBack,
                            )
                        }

                        feedback?.let { event ->
                            Text(
                                text = event.feedbackText(),
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 12.dp)
                                    .graphicsLayer(alpha = feedbackAlpha.value),
                                color = event.feedbackColor(),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                            )
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
                            .height(if (isSmallScreen) 50.dp else 64.dp)
                            .semantics {
                                contentDescription = "Question: ${state.currentQuestion.problem}"
                            },
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
                                enabled = !state.isGameOver && state.pendingBoardMutation == null,
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
private fun ModeHud(
    state: GameUiState,
    timers: ModeTimerUiState,
    compact: Boolean,
) {
    val valueSize = if (compact) 19.sp else 25.sp
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        when (state.mode.id) {
            GameModeId.CLASSIC -> {
                ScoreMetric(state, valueSize, Modifier.weight(1f))
                HudMetric("MODE", "RELAXED", Modifier.weight(1f), Color(0xFFBDEBFF), valueSize)
            }
            GameModeId.TIME_ATTACK -> {
                ScoreMetric(state, valueSize, Modifier.weight(1f))
                CountdownMetric(
                    label = "TIME LEFT",
                    timer = timers.timer(GameTimerKind.SESSION),
                    modifier = Modifier.weight(1f),
                    valueSize = valueSize,
                    announceTenSeconds = true,
                )
            }
            GameModeId.SURVIVAL -> {
                HudMetric(
                    label = "HEARTS",
                    value = "♥".repeat(state.heartsRemaining ?: 0),
                    modifier = Modifier.weight(0.9f),
                    color = Color(0xFFFF8FA8),
                    valueSize = valueSize,
                )
                ScoreMetric(state, valueSize, Modifier.weight(0.8f))
                CountdownMetric(
                    label = "QUESTION",
                    timer = timers.timer(GameTimerKind.QUESTION),
                    modifier = Modifier.weight(1f),
                    valueSize = valueSize,
                    announceTenSeconds = false,
                )
            }
            GameModeId.PUZZLE_DECAY -> {
                CountdownMetric(
                    label = "NEXT DECAY",
                    timer = timers.timer(GameTimerKind.DECAY),
                    modifier = Modifier.weight(1f),
                    valueSize = valueSize,
                    announceTenSeconds = false,
                )
                CountdownMetric(
                    label = "RUN LIMIT",
                    timer = timers.timer(GameTimerKind.MAXIMUM_SESSION),
                    modifier = Modifier.weight(1f),
                    valueSize = valueSize,
                    announceTenSeconds = true,
                )
                HudMetric(
                    "PIECES LOST",
                    state.statistics.piecesRemoved.toString(),
                    Modifier.weight(0.8f),
                    Color(0xFFFFB2D3),
                    valueSize,
                )
            }
            GameModeId.COMBO_RUSH -> {
                ScoreMetric(state, valueSize, Modifier.weight(0.8f), emphasizeCombo = true)
                CountdownMetric(
                    label = "RUSH TIME",
                    timer = timers.timer(GameTimerKind.SESSION),
                    modifier = Modifier.weight(1f),
                    valueSize = valueSize,
                    announceTenSeconds = true,
                )
            }
        }
    }
}

@Composable
private fun ScoreMetric(
    state: GameUiState,
    valueSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier,
    emphasizeCombo: Boolean = false,
) {
    Column(modifier = modifier) {
        Text("SCORE", color = Color.White.copy(alpha = 0.62f), fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(state.score.toString(), color = Color.White, fontSize = valueSize, fontWeight = FontWeight.Black)
        if (state.combo > 1 || emphasizeCombo) {
            Text(
                "COMBO ×${state.combo}",
                color = Color(0xFFFFE066),
                fontWeight = FontWeight.Black,
                fontSize = if (emphasizeCombo) 13.sp else 10.sp,
            )
        }
    }
}

@Composable
private fun HudMetric(
    label: String,
    value: String,
    modifier: Modifier,
    color: Color,
    valueSize: androidx.compose.ui.unit.TextUnit,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Text(value.ifEmpty { "♡" }, color = color, fontSize = valueSize, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun CountdownMetric(
    label: String,
    timer: TimerUiAnchor?,
    modifier: Modifier,
    valueSize: androidx.compose.ui.unit.TextUnit,
    announceTenSeconds: Boolean,
) {
    if (timer == null) {
        HudMetric(label, "—", modifier, Color.White, valueSize)
        return
    }
    var remaining by remember(timer.id, timer.anchorClockMillis, timer.paused) {
        mutableLongStateOf(timer.remainingMillis(SystemClock.elapsedRealtime()))
    }
    var warning by remember(timer.id) { mutableStateOf("") }
    LaunchedEffect(timer) {
        remaining = timer.remainingMillis(SystemClock.elapsedRealtime())
        while (!timer.paused && remaining > 0) {
            withFrameNanos { }
            remaining = timer.remainingMillis(SystemClock.elapsedRealtime())
            if (announceTenSeconds && warning.isEmpty() && timer.durationMillis > 10_000 && remaining in 1L..10_000L) {
                warning = "Ten seconds left"
            }
        }
    }
    val fraction = (remaining.toFloat() / timer.durationMillis).coerceIn(0f, 1f)
    Column(
        modifier = modifier.semantics { contentDescription = "$label timer" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, color = Color.White.copy(alpha = 0.62f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Timer, contentDescription = null, tint = Color(0xFFBDEBFF), modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(formatTimeMillis(remaining), color = Color.White, fontSize = valueSize, fontWeight = FontWeight.Black)
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
            color = if (remaining <= 10_000) Color(0xFFFF8FA8) else Color(0xFF69D5FF),
            trackColor = Color.White.copy(alpha = 0.16f),
        )
        if (warning.isNotEmpty()) {
            Text(
                warning,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                color = Color(0xFFFFE066),
                fontSize = 9.sp,
            )
        }
    }
}

@Composable
private fun ModeResultOverlay(
    state: GameUiState,
    onRetry: () -> Unit,
    onChooseMode: () -> Unit,
    onBackToGallery: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xEE171044)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.outcome.resultTitle(),
                color = Color(0xFFFFE066),
                fontSize = 23.sp,
                fontWeight = FontWeight.Black,
            )
            Text(state.mode.visibleName, color = Color(0xFFBDEBFF), fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(10.dp))
            ResultLine("Final score", state.score.toString())
            ResultLine("Elapsed", formatTime(state.timeElapsed))
            ResultLine("Correct answers", state.statistics.correctAnswers.toString())
            ResultLine("Wrong answers", state.statistics.incorrectAnswers.toString())
            ResultLine("Maximum combo", "×${state.statistics.maximumCombo}")
            ResultLine("Pieces revealed", state.statistics.piecesRevealed.toString())
            ResultLine("Pieces removed", state.statistics.piecesRemoved.toString())
            state.heartsRemaining?.let { ResultLine("Hearts remaining", it.toString()) }
            state.bestTime?.let { ResultLine("Personal best", formatTime(it)) }
            Spacer(Modifier.height(12.dp))
            Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) { Text("Retry") }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick = onChooseMode, modifier = Modifier.fillMaxWidth()) {
                Text("Choose Another Mode", color = Color.White)
            }
            TextButton(onClick = onBackToGallery) { Text("Back to Gallery", color = Color.White) }
        }
    }
}

@Composable
private fun ResultLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.White.copy(alpha = 0.72f), fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

private fun ModeOutcome?.resultTitle(): String = when (this) {
    ModeOutcome.COMPLETED -> "Puzzle Complete!"
    ModeOutcome.TIME_EXPIRED -> "Time's Up — Great Effort!"
    ModeOutcome.OUT_OF_HEARTS -> "Out of Hearts — Try Again"
    ModeOutcome.DECAY_LIMIT_REACHED -> "Run Over — Great Effort!"
    ModeOutcome.ABANDONED -> "Run Paused"
    null -> "Run Over"
}

private fun GameFeedbackEvent.feedbackText(): String = when (type) {
    GameFeedbackType.TIME_BONUS -> "+${"%.1f".format(amount / 1_000f)}s"
    GameFeedbackType.TIME_PENALTY -> "−${"%.1f".format(-amount / 1_000f)}s"
    GameFeedbackType.HEART_LOST -> if (amount == 1L) "One heart remaining" else "Heart used — keep going!"
    GameFeedbackType.PIECE_REMOVED -> "A piece faded — solve it again!"
}

private fun GameFeedbackEvent.feedbackColor(): Color = when (type) {
    GameFeedbackType.TIME_BONUS -> Color(0xFF7FFFD4)
    GameFeedbackType.TIME_PENALTY,
    GameFeedbackType.HEART_LOST,
    GameFeedbackType.PIECE_REMOVED -> Color(0xFFFFB2D3)
}

private fun formatTimeMillis(millis: Long): String = formatTime(((millis + 999) / 1_000L).toInt())

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
fun MathOptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.semantics { contentDescription = "Answer $text" },
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
