package com.qtpie.simplepuzzle.renderer.gdx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FixedStepClockTest {
    @Test
    fun accumulatesSubFrameDeltas() {
        val clock = FixedStepClock(stepSeconds = 0.02f, maximumDeltaSeconds = 0.1f)

        assertEquals(0, clock.consume(0.01f))
        assertEquals(1, clock.consume(0.01f))
        assertEquals(0f, clock.interpolation, 0.0001f)
    }

    @Test
    fun clampsExtremeDeltaAfterResume() {
        val clock = FixedStepClock(stepSeconds = 0.02f, maximumDeltaSeconds = 0.1f)

        val steps = clock.consume(10f)

        assertEquals(5, steps)
        assertTrue(clock.interpolation in 0f..1f)
    }
}
