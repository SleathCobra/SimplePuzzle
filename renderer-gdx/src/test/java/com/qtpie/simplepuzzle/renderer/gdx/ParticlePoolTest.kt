package com.qtpie.simplepuzzle.renderer.gdx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ParticlePoolTest {
    @Test
    fun poolCapsBurstsAndReusesExpiredSlots() {
        val pool = ParticlePool(capacity = 4)

        assertEquals(4, pool.spawnBurst(0.5f, 0.5f, requestedCount = 12, speed = 1f))
        assertEquals(4, pool.activeCount())
        assertEquals(0, pool.spawnBurst(0.5f, 0.5f, requestedCount = 1, speed = 1f))

        repeat(60) { pool.update(1f / 60f) }
        assertEquals(0, pool.activeCount())
        assertEquals(2, pool.spawnBurst(0.5f, 0.5f, requestedCount = 2, speed = 1f))
        assertTrue(pool.activeCount() <= pool.capacity)
    }
}
