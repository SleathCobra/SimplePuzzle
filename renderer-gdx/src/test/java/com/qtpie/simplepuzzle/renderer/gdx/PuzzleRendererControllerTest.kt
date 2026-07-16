package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PuzzleRendererControllerTest {
    @Test
    fun commandsAreConsumedOnceInSubmissionOrder() {
        val controller = PuzzleRendererController()
        val first = RendererCommand.SetQuality(GraphicsQuality.LOW)
        val second = RendererCommand.RevealPiece(pieceIndex = 4, reducedMotion = false)

        controller.submit(first)
        controller.submit(second)

        assertEquals(first, controller.poll())
        assertEquals(second, controller.poll())
        assertNull(controller.poll())
    }

    @Test
    fun revealCompletionIsDeliveredOnlyToCurrentListener() {
        val controller = PuzzleRendererController()
        val completed = mutableListOf<Int>()
        controller.setRevealFinishedListener(completed::add)

        controller.notifyRevealFinished(7)
        controller.setRevealFinishedListener(null)
        controller.notifyRevealFinished(8)

        assertEquals(listOf(7), completed)
    }
}
