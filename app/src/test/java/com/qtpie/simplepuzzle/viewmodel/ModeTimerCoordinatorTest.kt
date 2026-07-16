package com.qtpie.simplepuzzle.viewmodel

import com.qtpie.simplepuzzle.core.model.ActiveGameTimer
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GameTimerId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeTimerCoordinatorTest {
    @Test
    fun pauseAndResumePreserveExactMonotonicRemainingDuration() {
        val clock = FakeGameClock()
        val scheduler = ManualTimerScheduler(clock)
        val expired = mutableListOf<GameTimerId>()
        val coordinator = ModeTimerCoordinator(clock, scheduler, expired::add)
        val timer = timer(durationMillis = 10_000)
        coordinator.startSession(1)
        coordinator.apply(listOf(GameEvent.TimerScheduled(timer)))

        scheduler.advanceBy(3_250)
        coordinator.pause()
        assertEquals(6_750L, coordinator.remainingMillis(GameTimerKind.SESSION))

        scheduler.advanceBy(20_000)
        assertTrue(expired.isEmpty())
        coordinator.resume()
        scheduler.advanceBy(6_749)
        assertTrue(expired.isEmpty())
        scheduler.advanceBy(1)

        assertEquals(listOf(timer.id), expired)
    }

    @Test
    fun cancelledCallbackFromEarlierSessionIsRejectedAfterRestart() {
        val clock = FakeGameClock()
        val scheduler = ManualTimerScheduler(clock)
        val expired = mutableListOf<GameTimerId>()
        val coordinator = ModeTimerCoordinator(clock, scheduler, expired::add)
        val oldTimer = timer(session = 1, generation = 1, durationMillis = 1_000)
        coordinator.startSession(1)
        coordinator.apply(listOf(GameEvent.TimerScheduled(oldTimer)))
        val staleCallback = scheduler.latestCallback()

        coordinator.startSession(2)
        val newTimer = timer(session = 2, generation = 1, durationMillis = 2_000)
        coordinator.apply(listOf(GameEvent.TimerScheduled(newTimer)))
        staleCallback()

        assertTrue(expired.isEmpty())
        assertEquals(newTimer.id, coordinator.uiState.value.timers.single().id)
    }

    @Test
    fun adjustmentsUseCurrentRemainingAndRespectCapWithoutTickWrites() {
        val clock = FakeGameClock()
        val scheduler = ManualTimerScheduler(clock)
        val coordinator = ModeTimerCoordinator(clock, scheduler) { }
        val timer = timer(durationMillis = 45_000)
        coordinator.startSession(1)
        coordinator.apply(listOf(GameEvent.TimerScheduled(timer)))
        scheduler.advanceBy(1_000)

        coordinator.apply(
            listOf(
                GameEvent.TimerAdjustmentRequested(
                    timerId = timer.id,
                    deltaMillis = 20_000,
                    maximumRemainingMillis = 60_000,
                ),
            ),
        )

        assertEquals(60_000L, coordinator.remainingMillis(GameTimerKind.SESSION))
        assertFalse(coordinator.uiState.value.timers.single().paused)
    }

    private fun timer(
        session: Long = 1,
        generation: Long = 1,
        durationMillis: Long,
    ) = ActiveGameTimer(
        id = GameTimerId(session, GameTimerKind.SESSION, generation),
        durationMillis = durationMillis,
    )

    private class FakeGameClock : GameClock {
        var now = 0L
        override fun nowMillis(): Long = now
    }

    private class ManualTimerScheduler(
        private val clock: FakeGameClock,
    ) : TimerScheduler {
        private data class Task(
            val dueMillis: Long,
            val callback: () -> Unit,
            var cancelled: Boolean = false,
        )

        private val tasks = mutableListOf<Task>()

        override fun schedule(delayMillis: Long, callback: () -> Unit): TimerCancellation {
            val task = Task(clock.now + delayMillis, callback)
            tasks += task
            return TimerCancellation { task.cancelled = true }
        }

        fun latestCallback(): () -> Unit = tasks.last().callback

        fun advanceBy(millis: Long) {
            clock.now += millis
            val due = tasks.filter { !it.cancelled && it.dueMillis <= clock.now }
            tasks.removeAll(due.toSet())
            due.forEach { it.callback() }
        }
    }
}
