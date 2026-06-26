package com.qtpie.simplepuzzle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qtpie.simplepuzzle.ui.screens.*
import com.qtpie.simplepuzzle.ui.theme.SimplePuzzleTheme
import com.qtpie.simplepuzzle.viewmodel.GameViewModel

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
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val puzzles by viewModel.puzzles.collectAsState()

    NavHost(navController = navController, startDestination = Screen.Start.route) {
        composable(Screen.Start.route) {
            StartScreen(
                onPlayClick = { navController.navigate(Screen.Gallery.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }
        
        composable(Screen.Gallery.route) {
            GalleryScreen(
                puzzles = puzzles,
                completedCount = userProfile.puzzlesCompleted,
                totalCount = userProfile.totalPuzzles,
                onPuzzleSelect = { puzzle ->
                    viewModel.selectPuzzle(puzzle)
                    navController.navigate(Screen.Game.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Game.route) {
            GameScreen(
                state = uiState,
                onAnswerSelected = { viewModel.onAnswerSelected(it) },
                onReset = { uiState.currentPuzzle?.let { viewModel.selectPuzzle(it) } },
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                userProfile = userProfile,
                settings = settings,
                onSettingsChange = { viewModel.updateSettings(it) },
                onResetProgress = { viewModel.resetProgress() },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
