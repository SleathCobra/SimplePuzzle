package com.qtpie.simplepuzzle.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.renderer.gdx.PuzzleRendererFragment
import com.qtpie.simplepuzzle.renderer.gdx.PuzzleRendererHostViewModel
import com.qtpie.simplepuzzle.renderer.gdx.RendererCommand

@Composable
fun GdxPuzzleBoard(
    visiblePieces: Set<Int>,
    revealingPiece: Int?,
    graphicsQuality: GraphicsQuality,
    reducedMotion: Boolean,
    incorrectFeedbackTrigger: Int,
    isCompleted: Boolean,
    onRevealFinished: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findFragmentActivity()
    val fragmentManager = activity.supportFragmentManager
    val hostViewModel = remember(activity) {
        ViewModelProvider(activity)[PuzzleRendererHostViewModel::class.java]
    }
    val controller = hostViewModel.controller
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentRevealCallback by rememberUpdatedState(onRevealFinished)

    DisposableEffect(controller) {
        val mainHandler = Handler(Looper.getMainLooper())
        controller.setRevealFinishedListener { pieceIndex ->
            mainHandler.post { currentRevealCallback(pieceIndex) }
        }
        onDispose { controller.setRevealFinishedListener(null) }
    }

    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.submit(RendererCommand.Resume)
                Lifecycle.Event.ON_PAUSE -> controller.submit(RendererCommand.Pause)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(visiblePieces) {
        controller.submit(RendererCommand.SetVisiblePieces(visiblePieces.sorted().toIntArray()))
    }
    LaunchedEffect(graphicsQuality) {
        controller.submit(RendererCommand.SetQuality(graphicsQuality))
    }
    LaunchedEffect(revealingPiece, reducedMotion) {
        revealingPiece?.let { pieceIndex ->
            controller.submit(RendererCommand.CorrectAnswerEffect)
            controller.submit(RendererCommand.RevealPiece(pieceIndex, reducedMotion))
        }
    }
    LaunchedEffect(incorrectFeedbackTrigger) {
        if (incorrectFeedbackTrigger > 0) controller.submit(RendererCommand.IncorrectAnswerEffect)
    }
    LaunchedEffect(isCompleted) {
        if (isCompleted) controller.submit(RendererCommand.CompletionEffect)
    }

    AndroidView(
        modifier = modifier,
        factory = { context ->
            FragmentContainerView(context).apply {
                id = R.id.puzzle_renderer_container
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                doOnAttach {
                    if (fragmentManager.findFragmentByTag(PuzzleRendererFragment.TAG) == null &&
                        !fragmentManager.isStateSaved
                    ) {
                        fragmentManager.commitNow {
                            replace(
                                id,
                                PuzzleRendererFragment.newInstance(),
                                PuzzleRendererFragment.TAG,
                            )
                        }
                    }
                }
            }
        },
        onRelease = {
            val fragment = fragmentManager.findFragmentByTag(PuzzleRendererFragment.TAG)
            if (fragment != null && !fragmentManager.isStateSaved) {
                fragmentManager.commitNow { remove(fragment) }
            }
        },
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("The puzzle renderer must be hosted by a FragmentActivity.")
}
