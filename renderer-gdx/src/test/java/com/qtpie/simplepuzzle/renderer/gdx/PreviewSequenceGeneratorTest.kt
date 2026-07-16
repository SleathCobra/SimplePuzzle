package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.BoardMutationType
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PreviewSequenceGeneratorTest {
    @Test
    fun `same seed produces the same initial board and sequence`() {
        val first = PreviewSequenceGenerator(30, seed = 728L, quality = GraphicsQuality.HIGH)
        val second = PreviewSequenceGenerator(30, seed = 728L, quality = GraphicsQuality.HIGH)

        assertArrayEquals(first.initialVisiblePieces(), second.initialVisiblePieces())
        repeat(40) {
            assertEquals(first.nextStep(), second.nextStep())
        }
    }

    @Test
    fun `occupancy remains bounded and a step never conflicts on a piece`() {
        val sequence = PreviewSequenceGenerator(30, seed = 90210L, quality = GraphicsQuality.HIGH)
        val visible = sequence.initialVisiblePieces().toMutableSet()
        val bounds = sequence.occupancyBounds()

        repeat(500) {
            val step = sequence.nextStep()
            assertTrue(step.delayMillis in 800L..1_200L)
            assertTrue(step.operations.size in 1..3)
            assertEquals(step.operations.size, step.operations.map { operation -> operation.pieceIndex }.distinct().size)
            step.operations.forEach { operation ->
                when (operation.type) {
                    BoardMutationType.REVEAL -> assertTrue(visible.add(operation.pieceIndex))
                    BoardMutationType.REMOVE -> assertTrue(visible.remove(operation.pieceIndex))
                }
            }
            assertTrue(visible.size in bounds)
            assertEquals(visible.size, sequence.visibleCount())
        }
    }

    @Test
    fun `low quality uses one transition and no immediate retoggle`() {
        val sequence = PreviewSequenceGenerator(30, seed = 17L, quality = GraphicsQuality.LOW)
        var previousPiece: Int? = null

        repeat(100) {
            val step = sequence.nextStep()
            assertEquals(1, step.operations.size)
            assertTrue(step.delayMillis in 1_300L..1_800L)
            val currentPiece = step.operations.single().pieceIndex
            assertFalse(currentPiece == previousPiece)
            previousPiece = currentPiece
        }
    }

    @Test
    fun `package order is deterministic non-repeating and switch duration is bounded`() {
        val first = PreviewPackageSequence(packageCount = 4, seed = 88L)
        val second = PreviewPackageSequence(packageCount = 4, seed = 88L)
        assertEquals(first.currentIndex, second.currentIndex)
        repeat(30) {
            val previous = first.currentIndex
            val firstNext = first.nextIndex()
            assertEquals(firstNext, second.nextIndex())
            assertFalse(previous == firstNext)
        }

        val sequence = PreviewSequenceGenerator(30, seed = 88L, quality = GraphicsQuality.MEDIUM)
        assertTrue(sequence.packageDurationMillis() in 15_000L..25_000L)
    }
}
