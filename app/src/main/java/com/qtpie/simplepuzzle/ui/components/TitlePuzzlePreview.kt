package com.qtpie.simplepuzzle.ui.components

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.ViewModelProvider
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.renderer.gdx.PuzzlePreviewRendererFragment
import com.qtpie.simplepuzzle.renderer.gdx.PuzzlePreviewRendererHostViewModel
import com.qtpie.simplepuzzle.renderer.gdx.PreviewPackageSequence

@Composable
fun TitlePuzzlePreview(
    assetRoots: List<String>,
    seed: Long,
    graphicsQuality: GraphicsQuality,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val activity = LocalContext.current.findPreviewHostActivity()
    val fragmentManager = activity.supportFragmentManager
    val hostViewModel = remember(activity) {
        ViewModelProvider(activity)[PuzzlePreviewRendererHostViewModel::class.java]
    }
    val packageSequence = remember(assetRoots, seed) {
        PreviewPackageSequence(assetRoots.size, seed)
    }
    var packageIndex by remember(assetRoots, seed) { mutableIntStateOf(packageSequence.currentIndex) }

    LaunchedEffect(hostViewModel, packageSequence, reducedMotion) {
        if (assetRoots.size > 1 && !reducedMotion) {
            hostViewModel.packageSwitchSignal.events.collect {
                val nextIndex = packageSequence.nextIndex()
                val nextAssetRoot = assetRoots[nextIndex]
                if (!fragmentManager.isStateSaved &&
                    fragmentManager.findFragmentByTag(PuzzlePreviewRendererFragment.TAG) != null
                ) {
                    val fragment = PuzzlePreviewRendererFragment.newInstance(
                        assetRoot = nextAssetRoot,
                        seed = seed xor nextIndex.toLong(),
                        quality = graphicsQuality,
                        reducedMotion = false,
                        packageSwitchEnabled = true,
                    )
                    fragmentManager.commitNow {
                        replace(
                            R.id.puzzle_preview_renderer_container,
                            fragment,
                            PuzzlePreviewRendererFragment.TAG,
                        )
                    }
                    activity.findViewById<android.view.View>(R.id.puzzle_preview_renderer_container)
                        ?.setTag(R.id.puzzle_preview_renderer_fragment_owner, fragment)
                    packageIndex = nextIndex
                }
            }
        }
    }

    val assetRoot = assetRoots.getOrNull(packageIndex) ?: return

    AndroidView(
            modifier = modifier.clearAndSetSemantics {
                contentDescription = "Animated jigsaw puzzle preview"
            },
            factory = { context ->
                FragmentContainerView(context).apply {
                    id = R.id.puzzle_preview_renderer_container
                    isClickable = false
                    isFocusable = false
                    importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    doOnAttach {
                        if (fragmentManager.findFragmentByTag(PuzzlePreviewRendererFragment.TAG) == null &&
                            !fragmentManager.isStateSaved
                        ) {
                            val fragment = PuzzlePreviewRendererFragment.newInstance(
                                assetRoot = assetRoot,
                                seed = seed xor packageIndex.toLong(),
                                quality = graphicsQuality,
                                reducedMotion = reducedMotion,
                                packageSwitchEnabled = assetRoots.size > 1,
                            )
                            fragmentManager.commitNow {
                                replace(
                                    id,
                                    fragment,
                                    PuzzlePreviewRendererFragment.TAG,
                                )
                            }
                            setTag(R.id.puzzle_preview_renderer_fragment_owner, fragment)
                        }
                    }
                }
            },
            onRelease = { container ->
                val fragment = container.getTag(R.id.puzzle_preview_renderer_fragment_owner)
                    as? PuzzlePreviewRendererFragment
                if (fragment != null && fragment in fragmentManager.fragments && !fragmentManager.isStateSaved) {
                    fragmentManager.commitNow { remove(fragment) }
                }
                container.setTag(R.id.puzzle_preview_renderer_fragment_owner, null)
            },
    )
}

private tailrec fun Context.findPreviewHostActivity(): FragmentActivity = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findPreviewHostActivity()
    else -> error("The puzzle preview must be hosted by a FragmentActivity.")
}
