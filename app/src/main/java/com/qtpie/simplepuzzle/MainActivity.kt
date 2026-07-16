package com.qtpie.simplepuzzle

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.qtpie.simplepuzzle.audio.MusicManager
import com.qtpie.simplepuzzle.audio.SoundManager
import com.qtpie.simplepuzzle.ui.components.JigsawMathBackground
import com.qtpie.simplepuzzle.ui.screens.*
import com.qtpie.simplepuzzle.ui.theme.SimplePuzzleTheme
import com.qtpie.simplepuzzle.viewmodel.GameViewModel
import com.qtpie.simplepuzzle.viewmodel.SoundEffect
import com.badlogic.gdx.backends.android.AndroidFragmentApplication
import com.qtpie.simplepuzzle.renderer.gdx.PuzzleRendererFragment

class MainActivity : FragmentActivity(), AndroidFragmentApplication.Callbacks {
    private val gameViewModel: GameViewModel by viewModels {
        val container = (application as JigsawMathApplication).dataContainer
        GameViewModel.Factory(
            preferencesRepository = container.preferencesRepository,
            progressRepository = container.progressRepository,
            learningRepository = container.learningRepository,
            applicationVersion = packageManager.getPackageInfo(packageName, 0).versionName ?: "unknown",
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimplePuzzleTheme {
                SimplePuzzleApp(viewModel = gameViewModel)
            }
        }
    }

    override fun onPause() {
        gameViewModel.pauseGame()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        gameViewModel.resumeGame()
    }

    override fun exit() {
        // libGDX is embedded as one renderer surface. It must never own or
        // finish the Compose application shell when its Fragment is removed.
    }
}

sealed class Screen(val route: String) {
    object Start : Screen("start")
    object Settings : Screen("settings")
    object Gallery : Screen("gallery")
    object Game : Screen("game")
    object Learning : Screen("learning")
}

@Composable
fun SimplePuzzleApp(viewModel: GameViewModel = viewModel()) {
    val context = LocalContext.current
    val soundManager = remember { SoundManager(context) }
    val musicManager = remember { MusicManager(context) }
    val hapticFeedback = LocalHapticFeedback.current
    
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val puzzles by viewModel.puzzles.collectAsStateWithLifecycle()
    val learningSummaries by viewModel.learningSummaries.collectAsStateWithLifecycle()
    val currentSettings by rememberUpdatedState(settings)
    var musicReady by remember { mutableStateOf(false) }

    fun playUiSound(effect: SoundEffect) {
        soundManager.playSound(effect, currentSettings.soundEffectsVolume)
    }

    fun leaveGameplay() {
        viewModel.pauseGame()
        val activity = context as? FragmentActivity
        val fragmentManager = activity?.supportFragmentManager
        val renderer = fragmentManager?.findFragmentByTag(PuzzleRendererFragment.TAG)
        if (fragmentManager != null && renderer != null && !fragmentManager.isStateSaved) {
            // Remove the libGDX Fragment before Compose detaches its
            // FragmentContainerView. Otherwise AndroidFragmentApplication can
            // lose the GL surface before its pause handshake and SIGKILL the
            // process after its four-second deadlock timeout.
            fragmentManager.commitNow { remove(renderer) }
        }
        playUiSound(SoundEffect.NAVIGATION)
        navController.popBackStack()
    }

    LaunchedEffect(Unit) {
        // Keep media preparation off the first-frame critical path without an
        // arbitrary delay: wait until two Compose frame clocks have elapsed.
        withFrameNanos { }
        withFrameNanos { }
        musicReady = true
    }

    LaunchedEffect(settings, musicReady) {
        if (musicReady) musicManager.updateSettings(settings)
    }

    LaunchedEffect(Unit) {
        viewModel.soundEvent.collect { effect ->
            val latestSettings = currentSettings
            soundManager.playSound(effect, latestSettings.soundEffectsVolume)
            if (latestSettings.hapticFeedbackEnabled) {
                when (effect) {
                    SoundEffect.ANSWER_CORRECT,
                    SoundEffect.PIECE_PLACED -> hapticFeedback.performHapticFeedback(HapticFeedbackType.Confirm)
                    SoundEffect.ANSWER_WRONG,
                    SoundEffect.MANY_FAILS -> hapticFeedback.performHapticFeedback(HapticFeedbackType.Reject)
                    else -> Unit
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            soundManager.release()
            musicManager.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        JigsawMathBackground(modifier = Modifier.fillMaxSize())
        
        NavHost(navController = navController, startDestination = Screen.Start.route) {
            composable(Screen.Start.route) {
                StartScreen(
                    onPlayClick = {
                        playUiSound(SoundEffect.NAVIGATION)
                        navController.navigate(Screen.Gallery.route)
                    },
                    onSettingsClick = {
                        playUiSound(SoundEffect.NAVIGATION)
                        navController.navigate(Screen.Settings.route)
                    },
                    onLearningClick = {
                        playUiSound(SoundEffect.NAVIGATION)
                        navController.navigate(Screen.Learning.route)
                    },
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
                            playUiSound(SoundEffect.NAVIGATION)
                            navController.navigate(Screen.Game.route)
                        }
                    },
                    onUnlock = { viewModel.unlockPuzzle(it) },
                    onBack = {
                        playUiSound(SoundEffect.NAVIGATION)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Game.route) {
                GameScreen(
                    state = uiState,
                    totalCoins = userProfile.totalCoins,
                    onAnswerSelected = { viewModel.onAnswerSelected(it) },
                    graphicsQuality = settings.graphicsQuality,
                    reducedMotion = settings.reducedMotion,
                    onRevealFinished = viewModel::onRevealAnimationFinished,
                    onReset = { uiState.currentPuzzle?.let { viewModel.selectPuzzle(it) } },
                    onBack = ::leaveGameplay,
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    userProfile = userProfile,
                    settings = settings,
                    onSettingsChange = { viewModel.updateSettings(it) },
                    onResetProgress = { viewModel.resetProgress() },
                    onBack = {
                        playUiSound(SoundEffect.NAVIGATION)
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Learning.route) {
                LearningSummaryScreen(
                    summaries = learningSummaries,
                    onBack = {
                        playUiSound(SoundEffect.NAVIGATION)
                        navController.popBackStack()
                    },
                )
            }
        }
    }
}
