package com.qtpie.simplepuzzle.renderer.gdx

internal class FixedStepClock(
    private val stepSeconds: Float = 1f / 60f,
    private val maximumDeltaSeconds: Float = 0.1f,
) {
    private var accumulatorSeconds = 0f

    var interpolation: Float = 0f
        private set

    fun consume(deltaSeconds: Float): Int {
        accumulatorSeconds += deltaSeconds.coerceIn(0f, maximumDeltaSeconds)
        val steps = (accumulatorSeconds / stepSeconds).toInt()
        accumulatorSeconds -= steps * stepSeconds
        interpolation = accumulatorSeconds / stepSeconds
        return steps
    }

    fun reset() {
        accumulatorSeconds = 0f
        interpolation = 0f
    }
}
