package com.qtpie.simplepuzzle.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PieceSetTest {
    @Test
    fun reveal_supportsPuzzleSizesBeyondLongBitMasks() {
        val completed = (0 until 70).fold(PieceSet.Empty) { pieces, index ->
            pieces.reveal(PieceId(index), pieceCount = 70)
        }

        assertEquals(70, completed.size)
        assertTrue(PieceId(69) in completed)
        assertTrue(completed.hiddenIndices(70).isEmpty())
    }

    @Test
    fun revealingAnExistingPieceReturnsSameValue() {
        val once = PieceSet.Empty.reveal(PieceId(3), pieceCount = 8)

        assertSame(once, once.reveal(PieceId(3), pieceCount = 8))
        assertFalse(PieceId(2) in once)
    }

    @Test(expected = IllegalArgumentException::class)
    fun revealRejectsOutOfBoundsPiece() {
        PieceSet.Empty.reveal(PieceId(4), pieceCount = 4)
    }
}
