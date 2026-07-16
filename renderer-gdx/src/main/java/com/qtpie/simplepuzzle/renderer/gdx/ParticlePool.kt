package com.qtpie.simplepuzzle.renderer.gdx

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal class ParticlePool(val capacity: Int) {
    private val x = FloatArray(capacity)
    private val y = FloatArray(capacity)
    private val velocityX = FloatArray(capacity)
    private val velocityY = FloatArray(capacity)
    private val remainingLife = FloatArray(capacity)
    private val totalLife = FloatArray(capacity)
    private val sizes = FloatArray(capacity)
    private var randomState = 0x6D2B79F5

    init {
        require(capacity > 0) { "Particle pool capacity must be positive." }
    }

    fun spawnBurst(originX: Float, originY: Float, requestedCount: Int, speed: Float): Int {
        var spawned = 0
        var index = 0
        while (index < capacity && spawned < requestedCount.coerceAtLeast(0)) {
            if (remainingLife[index] <= 0f) {
                val angle = nextFloat() * (2f * PI.toFloat())
                val particleSpeed = speed * (0.55f + nextFloat() * 0.65f)
                x[index] = originX
                y[index] = originY
                velocityX[index] = cos(angle) * particleSpeed
                velocityY[index] = sin(angle) * particleSpeed
                remainingLife[index] = 0.32f + nextFloat() * 0.42f
                totalLife[index] = remainingLife[index]
                sizes[index] = 0.018f + nextFloat() * 0.025f
                spawned++
            }
            index++
        }
        return spawned
    }

    fun update(stepSeconds: Float) {
        var index = 0
        while (index < capacity) {
            if (remainingLife[index] > 0f) {
                remainingLife[index] = (remainingLife[index] - stepSeconds).coerceAtLeast(0f)
                x[index] += velocityX[index] * stepSeconds
                y[index] += velocityY[index] * stepSeconds
                velocityY[index] -= 0.18f * stepSeconds
            }
            index++
        }
    }

    fun isActive(index: Int): Boolean = remainingLife[index] > 0f
    fun x(index: Int): Float = x[index]
    fun y(index: Int): Float = y[index]
    fun size(index: Int): Float = sizes[index]
    fun alpha(index: Int): Float = remainingLife[index] / totalLife[index]

    fun activeCount(): Int {
        var count = 0
        var index = 0
        while (index < capacity) {
            if (remainingLife[index] > 0f) count++
            index++
        }
        return count
    }

    private fun nextFloat(): Float {
        randomState = randomState * 1_664_525 + 1_013_904_223
        return (randomState ushr 8) / 16_777_216f
    }
}
