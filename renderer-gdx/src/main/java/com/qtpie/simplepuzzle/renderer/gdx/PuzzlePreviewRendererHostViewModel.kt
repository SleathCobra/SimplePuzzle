package com.qtpie.simplepuzzle.renderer.gdx

import androidx.lifecycle.ViewModel
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PuzzlePreviewRendererHostViewModel : ViewModel() {
    val controller = PuzzleRendererController()
    val packageSwitchSignal = PreviewPackageSwitchSignal()

    override fun onCleared() {
        controller.clear()
    }
}

class PreviewPackageSwitchSignal internal constructor() {
    private val nextId = AtomicLong(1)
    private val _events = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val events: SharedFlow<Long> = _events.asSharedFlow()

    internal fun markIdleBoundary() {
        _events.tryEmit(nextId.getAndIncrement())
    }
}
