package com.qtpie.simplepuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qtpie.simplepuzzle.audio.MusicManager
import com.qtpie.simplepuzzle.audio.SoundManager
import com.qtpie.simplepuzzle.ui.screens.*
import com.qtpie.simplepuzzle.ui.theme.SimplePuzzleTheme
import com.qtpie.simplepuzzle.viewmodel.GameViewModel
import com.qtpie.simplepuzzle.viewmodel.SoundEffect

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimplePuzzleTheme {
                SimplePuzzleApp()
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Start : Screen("start")
    object Settings : Screen("settings")
    object Gallery : Screen("gallery")
    object Game : Screen("game")
}

@Composable
fun SimplePuzzleApp(viewModel: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val musicManager = remember { MusicManager(context) }
    
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val puzzles by viewModel.puzzles.collectAsState()

    LaunchedEffect(settings) {
        musicManager.updateSettings(settings)
    }

    LaunchedEffect(Unit) {
        viewModel.soundEvent.collect { effect ->
            soundManager.playSound(effect, settings.soundEffectsVolume)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
            musicManager.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.bg),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )
        
        NavHost(navController = navController, startDestination = Screen.Start.route) {
            composable(Screen.Start.route) {
                StartScreen(
                    onPlayClick = {
                        soundManager.playSound(SoundEffect.NAVIGATION)
                        navController.navigate(Screen.Gallery.route)
                    },
                    onSettingsClick = {
                        soundManager.playSound(SoundEffect.NAVIGATION)
                        navController.navigate(Screen.Settings.route)
                    },
                    onSound = { soundManager.playSound(it) }
                )
            }
            
            composable(Screen.Gallery.route) {
                GalleryScreen(
                    puzzles = puzzles,
                    completedCount = userProfile.puzzlesCompleted,
                    totalCount = userProfile.totalPuzzles,
                    totalCoins = userProfile.totalCoins,
                    onPuzzleSelect = { puzzle ->
                        viewModel.selectPuzzle(puzzle)
                        if (!puzzle.isLocked) {
                            soundManager.playSound(SoundEffect.NAVIGATION)
                            navController.navigate(Screen.Game.route)
                        }
                    },
                    onUnlock = { viewModel.unlockPuzzle(it) },
                    onBack = {
                        soundManager.playSound(SoundEffect.NAVIGATION)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Game.route) {
                GameScreen(
                    state = uiState,
                    totalCoins = userProfile.totalCoins,
                    onAnswerSelected = { viewModel.onAnswerSelected(it) },
                    onReset = { uiState.currentPuzzle?.let { viewModel.selectPuzzle(it) } },
                    onBack = {
                        soundManager.playSound(SoundEffect.NAVIGATION)
                        navController.popBackStack()
                    },
                    onSound = { soundManager.playSound(it) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    userProfile = userProfile,
                    settings = settings,
                    onSettingsChange = { viewModel.updateSettings(it) },
                    onResetProgress = { viewModel.resetProgress() },
                    onBack = {
                        soundManager.playSound(SoundEffect.NAVIGATION)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
