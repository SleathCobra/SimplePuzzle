package com.qtpie.simplepuzzle

import android.os.Bundle
import android.view.View
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import com.qtpie.simplepuzzle.renderer.gdx.PuzzlePreviewRendererFragment
import com.qtpie.simplepuzzle.renderer.gdx.PuzzleRendererFragment
import com.qtpie.simplepuzzle.viewmodel.GameViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class JigsawMathNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun titlePreviewGalleryAndTitleUseOneControlledSurface() {
        composeRule.onNodeWithContentDescription("Animated jigsaw puzzle preview").assertExists()
        repeat(2) {
            composeRule.onNodeWithText("PLAY").performClick()
            composeRule.onNodeWithText("PUZZLE GALLERY").assertExists()
            composeRule.runOnUiThread {
                assertNull(
                    composeRule.activity.supportFragmentManager
                        .findFragmentByTag(PuzzlePreviewRendererFragment.TAG),
                )
            }
            composeRule.onNodeWithContentDescription("Back").performClick()
            composeRule.onNodeWithContentDescription("Animated jigsaw puzzle preview").assertExists()
            composeRule.runOnUiThread {
                val previews = composeRule.activity.supportFragmentManager.fragments
                    .count { it.tag == PuzzlePreviewRendererFragment.TAG }
                assertEquals(1, previews)
            }
        }
    }

    @Test
    fun galleryModeSelectionStartsClassicAndTimedMode() {
        openModeSelection()
        composeRule.onNodeWithText("Classic").performClick()
        composeRule.onNodeWithText("RELAXED").assertExists()
        composeRule.onNodeWithContentDescription("Back").performClick()
        composeRule.onNodeWithContentDescription("Cosmic Journey").performClick()
        val rendererCreated = CountDownLatch(1)
        val fragmentCallbacks = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fragmentManager: FragmentManager,
                fragment: Fragment,
                view: View,
                savedInstanceState: Bundle?,
            ) {
                if (fragment is PuzzleRendererFragment) rendererCreated.countDown()
            }
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            composeRule.activity.supportFragmentManager.registerFragmentLifecycleCallbacks(
                fragmentCallbacks,
                false,
            )
        }
        // Invoke the semantic action directly. performClick() waits for Compose
        // idleness after the action, but the timed HUD deliberately owns a
        // local frame-driven countdown and therefore never becomes globally
        // idle while the session is running.
        val timeAttackClick = composeRule.onNodeWithText("Time Attack")
            .fetchSemanticsNode()
            .config[SemanticsActions.OnClick]
            .action
        composeRule.mainClock.autoAdvance = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertEquals(true, timeAttackClick?.invoke())
        }
        composeRule.mainClock.advanceTimeBy(1_000L)
        val rendererAttached = rendererCreated.await(10, TimeUnit.SECONDS)
        var rendererDetails = ""
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val fragmentManager = composeRule.activity.supportFragmentManager
            val container = composeRule.activity.findViewById<View>(R.id.puzzle_renderer_container)
            rendererDetails = buildString {
                val viewModel = ViewModelProvider(composeRule.activity)[GameViewModel::class.java]
                append("mode=")
                append(viewModel.uiState.value.mode.id)
                append(", puzzle=")
                append(viewModel.uiState.value.currentPuzzle?.id)
                append(", ")
                append("fragments=")
                append(fragmentManager.fragments.joinToString { "${it::class.simpleName}:${it.tag}" })
                append(", stateSaved=")
                append(fragmentManager.isStateSaved)
                append(", container=")
                append(container != null)
                append(", attached=")
                append(container?.isAttachedToWindow)
                append(", size=")
                append(container?.width)
                append('x')
                append(container?.height)
            }
        }
        assertTrue(rendererDetails, rendererAttached)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            composeRule.activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(fragmentCallbacks)
            val viewModel = ViewModelProvider(composeRule.activity)[GameViewModel::class.java]
            assertEquals(GameModeId.TIME_ATTACK, viewModel.uiState.value.mode.id)
            assertEquals(
                90_000L,
                viewModel.timerUiState.value.timer(GameTimerKind.SESSION)?.durationMillis,
            )
            assertNull(
                composeRule.activity.supportFragmentManager
                    .findFragmentByTag(PuzzlePreviewRendererFragment.TAG),
            )
            assertEquals(
                1,
                composeRule.activity.supportFragmentManager.fragments
                    .count { it.tag == PuzzleRendererFragment.TAG },
            )
        }
    }

    private fun openModeSelection() {
        composeRule.onNodeWithText("PLAY").performClick()
        composeRule.onNodeWithContentDescription("Cosmic Journey").performClick()
        composeRule.onNodeWithText("CHOOSE A MODE").assertExists()
    }
}
