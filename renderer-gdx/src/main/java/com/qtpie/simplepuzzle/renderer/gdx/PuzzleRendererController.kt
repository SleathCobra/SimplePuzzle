package com.qtpie.simplepuzzle.renderer.gdx

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class PuzzleRendererController {
    private val commands = ConcurrentLinkedQueue<RendererCommand>()
    private val revealListener = AtomicReference<((Int) -> Unit)?>(null)

    fun submit(command: RendererCommand) {
        commands.offer(command)
    }

    fun setRevealFinishedListener(listener: ((Int) -> Unit)?) {
        revealListener.set(listener)
    }

    internal fun poll(): RendererCommand? = commands.poll()

    internal fun notifyRevealFinished(pieceIndex: Int) {
        revealListener.get()?.invoke(pieceIndex)
    }

    internal fun clear() {
        commands.clear()
        revealListener.set(null)
    }
}
