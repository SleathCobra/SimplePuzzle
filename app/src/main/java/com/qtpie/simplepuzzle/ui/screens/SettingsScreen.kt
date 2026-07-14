package com.qtpie.simplepuzzle.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qtpie.simplepuzzle.model.BackgroundMusicMode
import com.qtpie.simplepuzzle.model.Difficulty
import com.qtpie.simplepuzzle.model.UserSettings
import com.qtpie.simplepuzzle.viewmodel.UserProfileState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userProfile: UserProfileState,
    settings: UserSettings,
    onSettingsChange: (UserSettings) -> Unit,
    onResetProgress: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("SETTINGS", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // User Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0E7FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(40.dp), tint = Color(0xFF5A4FCF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(userProfile.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Total Score: ${userProfile.totalScore}", color = Color.Gray)
                        Text("Puzzles Completed: ${userProfile.puzzlesCompleted}/${userProfile.totalPuzzles}", color = Color.Gray)
                    }
                }
            }

            SettingsSection(title = "AUDIO") {
                SettingsToggle("Sound Effects", settings.soundEffectsEnabled) {
                    onSettingsChange(settings.copy(soundEffectsEnabled = it))
                }
                if (settings.soundEffectsEnabled) {
                    VolumeSlider("SFX Volume", settings.soundEffectsVolume) {
                        onSettingsChange(settings.copy(soundEffectsVolume = it))
                    }
                }
                
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                
                SettingsToggle("Background Music", settings.backgroundMusicEnabled) {
                    onSettingsChange(settings.copy(backgroundMusicEnabled = it))
                }
                if (settings.backgroundMusicEnabled) {
                    VolumeSlider("Music Volume", settings.backgroundMusicVolume) {
                        onSettingsChange(settings.copy(backgroundMusicVolume = it))
                    }
                    MusicModeSelector(settings.backgroundMusicMode) {
                        onSettingsChange(settings.copy(backgroundMusicMode = it))
                    }
                }
            }

            SettingsSection(title = "GAMEPLAY") {
                SettingsToggle("Haptic Feedback", settings.hapticFeedbackEnabled) {
                    onSettingsChange(settings.copy(hapticFeedbackEnabled = it))
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                SettingsToggle("Stars & Hearts Confetti", settings.showSpecialConfetti) {
                    onSettingsChange(settings.copy(showSpecialConfetti = it))
                }
                Divider(color = Color.LightGray.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Difficulty", fontWeight = FontWeight.Medium)
                    DifficultySegmentedButton(settings.difficulty) {
                        onSettingsChange(settings.copy(difficulty = it))
                    }
                }
            }

            SettingsSection(title = "ACCOUNT") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Reset All Progress", fontWeight = FontWeight.Bold)
                        Text("WARNING: This cannot be undone", fontSize = 12.sp, color = Color.Gray)
                    }
                    IconButton(
                        onClick = onResetProgress,
                        modifier = Modifier.background(Color(0xFFFF4B5C).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Default.Error, contentDescription = "Reset", tint = Color(0xFFFF4B5C))
                    }
                }
            }

            SettingsSection(title = "ABOUT") {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("About Jigsaw Math", fontWeight = FontWeight.Bold)
                    Text("Version 1.0.0", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Privacy Policy", color = Color(0xFF5A4FCF), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column {
        Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            content = content
        )
    }
}

@Composable
fun VolumeSlider(label: String, value: Float, onValueChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, color = Color.Gray)
            Text("${(value * 100).roundToInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFF5A4FCF),
                activeTrackColor = Color(0xFF5A4FCF),
                inactiveTrackColor = Color(0xFFE0E7FF)
            )
        )
    }
}

@Composable
fun MusicModeSelector(current: BackgroundMusicMode, onSelect: (BackgroundMusicMode) -> Unit) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Music Track", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackgroundMusicMode.values().forEach { mode ->
                val isSelected = current == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Color(0xFF5A4FCF) else Color(0xFFF1F5F9))
                        .clickable { onSelect(mode) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        mode.name.replace("Music", "#"),
                        color = if (isSelected) Color.White else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DifficultySegmentedButton(current: Difficulty, onSelect: (Difficulty) -> Unit) {
    Row(
        modifier = Modifier
            .background(Color(0xFFF1F5F9), RoundedCornerShape(20.dp))
            .padding(4.dp)
    ) {
        Difficulty.values().forEach { difficulty ->
            val isSelected = current == difficulty
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) Color(0xFF5A4FCF) else Color.Transparent)
                    .clickable { onSelect(difficulty) }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    difficulty.name,
                    color = if (isSelected) Color.White else Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
