package com.qtpie.simplepuzzle.viewmodel

import android.os.SystemClock
import com.qtpie.simplepuzzle.core.model.GameEvent
import com.qtpie.simplepuzzle.core.model.GameTimerId
import com.qtpie.simplepuzzle.core.model.GameTimerKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

fun interface GameClock {
    fun nowMillis(): Long
}

data object MonotonicGameClock : GameClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}

fun interface TimerCancellation {
    fun cancel()
}

fun interface TimerScheduler {
    fun schedule(delayMillis: Long, callback: () -> Unit): TimerCancellation
}

class CoroutineTimerScheduler(
    private val scope: CoroutineScope,
) : TimerScheduler {
    override fun schedule(delayMillis: Long, callback: () -> Unit): TimerCancellation {
        val job: Job = scope.launch {
            delay(delayMillis.coerceAtLeast(0))
            callback()
        }
        return TimerCancellation(job::cancel)
    }
}

data class TimerUiAnchor(
    val id: GameTimerId,
    val durationMillis: Long,
    val remainingAtAnchorMillis: Long,
    val anchorClockMillis: Long,
    val paused: Boolean,
) {
    fun remainingMillis(nowMillis: Long): Long = if (paused) {
        remainingAtAnchorMillis
    } else {
        (remainingAtAnchorMillis - (nowMillis - anchorClockMillis).coerceAtLeast(0)).coerceAtLeast(0)
    }
}

data class SessionElapsedUiAnchor(
    val sessionGeneration: Long,
    val elapsedAtAnchorMillis: Long,
    val anchorClockMillis: Long,
    val paused: Boolean,
) {
    fun elapsedMillis(nowMillis: Long): Long = if (paused) {
        elapsedAtAnchorMillis
    } else {
        elapsedAtAnchorMillis + (nowMillis - anchorClockMillis).coerceAtLeast(0)
    }
}

data class ModeTimerUiState(
    val timers: List<TimerUiAnchor> = emptyList(),
    val sessionElapsed: SessionElapsedUiAnchor? = null,
) {
    fun timer(kind: GameTimerKind): TimerUiAnchor? = timers.firstOrNull { it.id.kind == kind }
}

/** Converts semantic reducer timer directives into monotonic, lifecycle-aware callbacks. */
class ModeTimerCoordinator(
    private val clock: GameClock,
    private val scheduler: TimerScheduler,
    private val onTimerExpired: (GameTimerId) -> Unit,
) {
    private data class RuntimeTimer(
        val id: GameTimerId,
        val durationMillis: Long,
        var remainingMillis: Long,
        var anchorMillis: Long,
        var paused: Boolean,
        var callbackGeneration: Long = 0,
        var cancellation: TimerCancellation? = null,
        var callbackDelivered: Boolean = false,
    )

    private val timers = linkedMapOf<GameTimerKind, RuntimeTimer>()
    private val _uiState = MutableStateFlow(ModeTimerUiState())
    val uiState: StateFlow<ModeTimerUiState> = _uiState.asStateFlow()

    private var sessionGeneration: Long? = null
    private var sessionElapsedMillis = 0L
    private var sessionAnchorMillis = 0L
    private var sessionPaused = true

    fun startSession(generation: Long) {
        require(generation > 0) { "Session generation must be positive." }
        cancelAll()
        sessionGeneration = generation
        sessionElapsedMillis = 0
        sessionAnchorMillis = clock.nowMillis()
        sessionPaused = false
        publish()
    }

    fun apply(events: List<GameEvent>) {
        events.forEach { event ->
            when (event) {
                is GameEvent.TimerScheduled -> schedule(
                    id = event.timer.id,
                    durationMillis = event.timer.durationMillis,
                )
                is GameEvent.TimerCancelled -> cancel(event.timerId)
                is GameEvent.TimerAdjustmentRequested -> adjust(
                    timerId = event.timerId,
                    deltaMillis = event.deltaMillis,
                    maximumRemainingMillis = event.maximumRemainingMillis,
                )
                else -> Unit
            }
        }
        publish()
    }

    fun pause() {
        val now = clock.nowMillis()
        timers.values.forEach { timer ->
            if (!timer.paused) {
                timer.remainingMillis = remaining(timer, now)
                timer.anchorMillis = now
                timer.paused = true
                invalidateCallback(timer)
            }
        }
        if (!sessionPaused && sessionGeneration != null) {
            sessionElapsedMillis += (now - sessionAnchorMillis).coerceAtLeast(0)
            sessionAnchorMillis = now
            sessionPaused = true
        }
        publish()
    }

    fun resume() {
        val now = clock.nowMillis()
        timers.values.forEach { timer ->
            if (timer.paused && !timer.callbackDelivered) {
                timer.anchorMillis = now
                timer.paused = false
                arm(timer)
            }
        }
        if (sessionPaused && sessionGeneration != null) {
            sessionAnchorMillis = now
            sessionPaused = false
        }
        publish()
    }

    fun remainingMillis(kind: GameTimerKind): Long? = timers[kind]?.let { remaining(it, clock.nowMillis()) }

    fun elapsedMillis(): Long {
        val now = clock.nowMillis()
        return if (sessionGeneration == null || sessionPaused) {
            sessionElapsedMillis
        } else {
            sessionElapsedMillis + (now - sessionAnchorMillis).coerceAtLeast(0)
        }
    }

    fun cancelAll() {
        timers.values.forEach(::invalidateCallback)
        timers.clear()
        sessionGeneration = null
        sessionElapsedMillis = 0
        sessionAnchorMillis = 0
        sessionPaused = true
        publish()
    }

    private fun schedule(id: GameTimerId, durationMillis: Long) {
        timers.remove(id.kind)?.let(::invalidateCallback)
        val now = clock.nowMillis()
        val timer = RuntimeTimer(
            id = id,
            durationMillis = durationMillis,
            remainingMillis = durationMillis,
            anchorMillis = now,
            paused = sessionPaused,
        )
        timers[id.kind] = timer
        if (!timer.paused) arm(timer)
    }

    private fun cancel(id: GameTimerId) {
        val timer = timers[id.kind] ?: return
        if (timer.id != id) return
        invalidateCallback(timer)
        timers.remove(id.kind)
    }

    private fun adjust(
        timerId: GameTimerId,
        deltaMillis: Long,
        maximumRemainingMillis: Long?,
    ) {
        val timer = timers[timerId.kind] ?: return
        if (timer.id != timerId || timer.callbackDelivered) return
        val now = clock.nowMillis()
        val adjusted = (remaining(timer, now) + deltaMillis)
            .let { remaining -> maximumRemainingMillis?.let(remaining::coerceAtMost) ?: remaining }
            .coerceAtLeast(0)
        timer.remainingMillis = adjusted
        timer.anchorMillis = now
        invalidateCallback(timer)
        timer.callbackDelivered = false
        if (!timer.paused) arm(timer)
    }

    private fun arm(timer: RuntimeTimer) {
        val callbackGeneration = ++timer.callbackGeneration
        timer.cancellation = scheduler.schedule(timer.remainingMillis) {
            val current = timers[timer.id.kind]
            if (current !== timer || current.paused || current.callbackDelivered ||
                current.callbackGeneration != callbackGeneration
            ) {
                return@schedule
            }
            current.remainingMillis = 0
            current.anchorMillis = clock.nowMillis()
            current.callbackDelivered = true
            current.cancellation = null
            publish()
            onTimerExpired(current.id)
        }
    }

    private fun invalidateCallback(timer: RuntimeTimer) {
        timer.callbackGeneration++
        timer.cancellation?.cancel()
        timer.cancellation = null
    }

    private fun remaining(timer: RuntimeTimer, now: Long): Long = if (timer.paused) {
        timer.remainingMillis
    } else {
        (timer.remainingMillis - (now - timer.anchorMillis).coerceAtLeast(0)).coerceAtLeast(0)
    }

    private fun publish() {
        val now = clock.nowMillis()
        _uiState.value = ModeTimerUiState(
            timers = timers.values.map { timer ->
                TimerUiAnchor(
                    id = timer.id,
                    durationMillis = timer.durationMillis,
                    remainingAtAnchorMillis = remaining(timer, now),
                    anchorClockMillis = now,
                    paused = timer.paused,
                )
            },
            sessionElapsed = sessionGeneration?.let { generation ->
                SessionElapsedUiAnchor(
                    sessionGeneration = generation,
                    elapsedAtAnchorMillis = elapsedMillis(),
                    anchorClockMillis = now,
                    paused = sessionPaused,
                )
            },
        )
    }
}
