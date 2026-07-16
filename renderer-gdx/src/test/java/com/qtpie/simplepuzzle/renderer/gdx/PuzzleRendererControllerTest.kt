package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.core.model.BoardMutationId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PuzzleRendererControllerTest {
    @Test
    fun commandsAreConsumedOnceInSubmissionOrder() {
        val controller = PuzzleRendererController()
        val first = RendererCommand.SetQuality(GraphicsQuality.LOW)
        val revealId = BoardMutationId(sessionGeneration = 1, sequence = 1)
        val removeId = BoardMutationId(sessionGeneration = 1, sequence = 2)
        val second = RendererCommand.RevealPiece(
            mutationId = revealId,
            pieceIndex = 4,
            reducedMotion = false,
        )
        val third = RendererCommand.RemovePiece(
            mutationId = removeId,
            pieceIndex = 2,
            reducedMotion = true,
        )

        controller.submit(first)
        controller.submit(second)
        controller.submit(third)

        assertEquals(first, controller.poll())
        assertEquals(second, controller.poll())
        assertEquals(third, controller.poll())
        assertNull(controller.poll())
    }

    @Test
    fun mutationCompletionIsDeliveredOnlyToCurrentListener() {
        val controller = PuzzleRendererController()
        val completed = mutableListOf<BoardMutationId>()
        val first = BoardMutationId(sessionGeneration = 3, sequence = 7)
        val second = BoardMutationId(sessionGeneration = 3, sequence = 8)
        controller.setMutationFinishedListener(completed::add)

        controller.notifyMutationFinished(first)
        controller.setMutationFinishedListener(null)
        controller.notifyMutationFinished(second)

        assertEquals(listOf(first), completed)
    }

    @Test
    fun inFlightRemovalReplaysUntilCommittedSnapshotArrives() {
        val controller = PuzzleRendererController()
        val mutation = RendererCommand.RemovePiece(
            mutationId = BoardMutationId(sessionGeneration = 4, sequence = 9),
            pieceIndex = 2,
            reducedMotion = false,
        )
        controller.submit(RendererCommand.SetVisiblePieces(intArrayOf(1, 2), sessionGeneration = 4))
        controller.submit(RendererCommand.SetQuality(GraphicsQuality.LOW))
        controller.submit(mutation)
        while (controller.poll() != null) Unit

        controller.notifyMutationFinished(mutation.mutationId)
        controller.replayState()

        val snapshot = controller.poll() as RendererCommand.SetVisiblePieces
        assertArrayEquals(intArrayOf(1, 2), snapshot.pieceIndices)
        assertEquals(RendererCommand.SetQuality(GraphicsQuality.LOW), controller.poll())
        assertEquals(mutation, controller.poll())
        assertNull(controller.poll())

        controller.submit(RendererCommand.SetVisiblePieces(intArrayOf(1), sessionGeneration = 4))
        controller.poll()
        controller.replayState()
        assertTrue(controller.poll() is RendererCommand.SetVisiblePieces)
        assertEquals(RendererCommand.SetQuality(GraphicsQuality.LOW), controller.poll())
        assertNull(controller.poll())
    }
}
