package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.core.model.GameModeCatalog
import com.qtpie.simplepuzzle.core.model.GameModeDefinition
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.model.PuzzleInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionScreen(
    puzzle: PuzzleInfo,
    onModeSelected: (GameModeId) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("CHOOSE A MODE", color = Color.White, fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to gallery",
                            tint = Color.White,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Image(
                    painter = painterResource(puzzle.imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop,
                )
                Column {
                    Text(
                        puzzle.name,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Play the same puzzle in a new way.",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            LazyVerticalGrid(
                columns = GridCells.Adaptive(240.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(
                    items = GameModeCatalog.all,
                    key = { it.id.persistedValue },
                    contentType = { "game-mode" },
                ) { mode ->
                    GameModeCard(mode = mode, onClick = { onModeSelected(mode.id) })
                }
            }
        }
    }
}

@Composable
private fun GameModeCard(
    mode: GameModeDefinition,
    onClick: () -> Unit,
) {
    val timerText = mode.timerDescription()
    val penaltyText = mode.penaltyDescription()
    val accessibilityLabel = buildString {
        append(mode.visibleName)
        if (mode.recommended) append(", recommended relaxed mode")
        append(". ${mode.summary} Timer: $timerText. $penaltyText")
    }
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = accessibilityLabel }
            .border(
                width = if (mode.recommended) 2.dp else 1.dp,
                color = if (mode.recommended) Color(0xFFFFE066) else Color.White.copy(alpha = 0.25f),
                shape = RoundedCornerShape(20.dp),
            ),
        shape = RoundedCornerShape(20.dp),
        color = if (mode.recommended) Color(0xFF6558D3).copy(alpha = 0.9f) else Color(0xFF352C80).copy(alpha = 0.76f),
        tonalElevation = 3.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = mode.icon(),
                        contentDescription = null,
                        tint = if (mode.recommended) Color(0xFFFFE066) else Color(0xFFFF9BC2),
                        modifier = Modifier.size(32.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        mode.visibleName,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 21.sp,
                    )
                    if (mode.recommended) {
                        Text(
                            "RECOMMENDED • RELAXED",
                            color = Color(0xFFFFE066),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
            Text(mode.summary, color = Color.White.copy(alpha = 0.9f))
            Text("Timer: $timerText", color = Color(0xFFBDEBFF), fontWeight = FontWeight.SemiBold)
            Text(penaltyText, color = Color.White.copy(alpha = 0.72f), style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun GameModeDefinition.icon(): ImageVector = when (id) {
    GameModeId.CLASSIC -> Icons.Default.SentimentSatisfied
    GameModeId.TIME_ATTACK -> Icons.Default.Timer
    GameModeId.SURVIVAL -> Icons.Default.Favorite
    GameModeId.PUZZLE_DECAY -> Icons.Default.AutoAwesome
    GameModeId.COMBO_RUSH -> Icons.Default.Bolt
}

private fun GameModeDefinition.timerDescription(): String = when (id) {
    GameModeId.CLASSIC -> "None"
    GameModeId.TIME_ATTACK -> "90 second session"
    GameModeId.SURVIVAL -> "10 / 8 / 6 seconds per question by difficulty"
    GameModeId.PUZZLE_DECAY -> "9 second decay, 3 minute session limit"
    GameModeId.COMBO_RUSH -> "45 seconds; correct answers add 2.5 seconds"
}

private fun GameModeDefinition.penaltyDescription(): String = when (id) {
    GameModeId.CLASSIC -> "No hearts or piece loss."
    GameModeId.TIME_ATTACK -> "Wrong answers reset your combo."
    GameModeId.SURVIVAL -> "Three hearts. Wrong or late answers cost one."
    GameModeId.PUZZLE_DECAY -> "Wrong answers and decay can remove a visible piece."
    GameModeId.COMBO_RUSH -> "Wrong answers reset combo and remove 4 seconds."
}
