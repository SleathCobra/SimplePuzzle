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
import com.qtpie.simplepuzzle.core.model.BoardMutationId
import com.qtpie.simplepuzzle.core.model.BoardMutationType
import com.qtpie.simplepuzzle.core.model.PendingBoardMutation
import com.qtpie.simplepuzzle.renderer.gdx.PuzzleRendererFragment
import com.qtpie.simplepuzzle.renderer.gdx.PuzzleRendererHostViewModel
import com.qtpie.simplepuzzle.renderer.gdx.RendererCommand

@Composable
fun GdxPuzzleBoard(
    assetRoot: String,
    visiblePieces: Set<Int>,
    sessionGeneration: Long,
    pendingBoardMutation: PendingBoardMutation?,
    graphicsQuality: GraphicsQuality,
    reducedMotion: Boolean,
    incorrectFeedbackTrigger: Int,
    isCompleted: Boolean,
    onMutationFinished: (BoardMutationId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findFragmentActivity()
    val fragmentManager = activity.supportFragmentManager
    val hostViewModel = remember(activity) {
        ViewModelProvider(activity)[PuzzleRendererHostViewModel::class.java]
    }
    val controller = hostViewModel.controller
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val currentMutationCallback by rememberUpdatedState(onMutationFinished)

    DisposableEffect(controller) {
        val mainHandler = Handler(Looper.getMainLooper())
        controller.setMutationFinishedListener { mutationId ->
            mainHandler.post { currentMutationCallback(mutationId) }
        }
        onDispose { controller.setMutationFinishedListener(null) }
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

    LaunchedEffect(visiblePieces, sessionGeneration, pendingBoardMutation, reducedMotion) {
        controller.submit(
            RendererCommand.SetVisiblePieces(
                pieceIndices = visiblePieces.sorted().toIntArray(),
                sessionGeneration = sessionGeneration,
            ),
        )
        pendingBoardMutation?.let { mutation ->
            when (mutation.type) {
                BoardMutationType.REVEAL -> {
                    controller.submit(RendererCommand.CorrectAnswerEffect)
                    controller.submit(
                        RendererCommand.RevealPiece(
                            mutationId = mutation.id,
                            pieceIndex = mutation.pieceId.value,
                            reducedMotion = reducedMotion,
                        ),
                    )
                }
                BoardMutationType.REMOVE -> controller.submit(
                    RendererCommand.RemovePiece(
                        mutationId = mutation.id,
                        pieceIndex = mutation.pieceId.value,
                        reducedMotion = reducedMotion,
                    ),
                )
            }
        }
    }
    LaunchedEffect(graphicsQuality) {
        controller.submit(RendererCommand.SetQuality(graphicsQuality))
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
                        val fragment = PuzzleRendererFragment.newInstance(assetRoot)
                        fragmentManager.commitNow {
                            replace(
                                id,
                                fragment,
                                PuzzleRendererFragment.TAG,
                            )
                        }
                        setTag(R.id.puzzle_renderer_fragment_owner, fragment)
                    }
                }
            }
        },
        onRelease = { container ->
            val fragment = container.getTag(R.id.puzzle_renderer_fragment_owner) as? PuzzleRendererFragment
            if (fragment != null && fragment in fragmentManager.fragments && !fragmentManager.isStateSaved) {
                fragmentManager.commitNow { remove(fragment) }
            }
            container.setTag(R.id.puzzle_renderer_fragment_owner, null)
        },
    )
}

private tailrec fun Context.findFragmentActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> error("The puzzle renderer must be hosted by a FragmentActivity.")
}
