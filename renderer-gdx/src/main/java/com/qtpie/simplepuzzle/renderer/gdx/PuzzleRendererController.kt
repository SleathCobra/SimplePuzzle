package com.qtpie.simplepuzzle.renderer.gdx

import com.qtpie.simplepuzzle.core.model.BoardMutationId
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

class PuzzleRendererController {
    private val commands = ConcurrentLinkedQueue<RendererCommand>()
    private val mutationListener = AtomicReference<((BoardMutationId) -> Unit)?>(null)
    private val visibleSnapshot = AtomicReference<RendererCommand.SetVisiblePieces?>(null)
    private val pendingMutation = AtomicReference<RendererCommand?>(null)
    private val selectedQuality = AtomicReference<RendererCommand.SetQuality?>(null)
    private val lifecycleCommand = AtomicReference<RendererCommand?>(null)

    fun submit(command: RendererCommand) {
        when (command) {
            is RendererCommand.SetVisiblePieces -> {
                visibleSnapshot.set(command)
                pendingMutation.updateAndGet { pending ->
                    if (pending == null || pending.sessionGeneration() != command.sessionGeneration) {
                        null
                    } else if (pending.isCommittedBy(command.pieceIndices)) {
                        null
                    } else {
                        pending
                    }
                }
            }
            is RendererCommand.RevealPiece,
            is RendererCommand.RemovePiece -> pendingMutation.set(command)
            is RendererCommand.SetQuality -> selectedQuality.set(command)
            RendererCommand.Pause,
            RendererCommand.Resume -> lifecycleCommand.set(command)
            RendererCommand.CompletionEffect,
            RendererCommand.CorrectAnswerEffect,
            RendererCommand.IncorrectAnswerEffect -> Unit
        }
        commands.offer(command)
    }

    fun setMutationFinishedListener(listener: ((BoardMutationId) -> Unit)?) {
        mutationListener.set(listener)
    }

    internal fun poll(): RendererCommand? = commands.poll()

    internal fun notifyMutationFinished(mutationId: BoardMutationId) {
        mutationListener.get()?.invoke(mutationId)
    }

    /** Replays coarse domain state before a recreated GL surface starts polling. */
    internal fun replayState() {
        commands.clear()
        visibleSnapshot.get()?.let(commands::offer)
        selectedQuality.get()?.let(commands::offer)
        pendingMutation.get()?.let(commands::offer)
        lifecycleCommand.get()?.let(commands::offer)
    }

    internal fun clear() {
        commands.clear()
        mutationListener.set(null)
        visibleSnapshot.set(null)
        pendingMutation.set(null)
        selectedQuality.set(null)
        lifecycleCommand.set(null)
    }

    private fun RendererCommand.sessionGeneration(): Long = when (this) {
        is RendererCommand.RevealPiece -> mutationId.sessionGeneration
        is RendererCommand.RemovePiece -> mutationId.sessionGeneration
        else -> Long.MIN_VALUE
    }

    private fun RendererCommand.isCommittedBy(visiblePieces: IntArray): Boolean = when (this) {
        is RendererCommand.RevealPiece -> visiblePieces.contains(pieceIndex)
        is RendererCommand.RemovePiece -> !visiblePieces.contains(pieceIndex)
        else -> true
    }
}
