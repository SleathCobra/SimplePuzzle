package com.qtpie.simplepuzzle.renderer.gdx

import androidx.lifecycle.ViewModel

class PuzzleRendererHostViewModel : ViewModel() {
    val controller = PuzzleRendererController()

    override fun onCleared() {
        controller.clear()
    }
}
