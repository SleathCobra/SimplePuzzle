package com.qtpie.simplepuzzle

import android.app.Application
import com.qtpie.simplepuzzle.core.data.JigsawDataContainer
import com.qtpie.simplepuzzle.model.AndroidPuzzleCatalog
import com.qtpie.simplepuzzle.model.PuzzleInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JigsawMathApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var dataContainer: JigsawDataContainer
        private set

    lateinit var puzzleCatalog: List<PuzzleInfo>
        private set

    lateinit var previewPuzzleCatalog: List<PuzzleInfo>
        private set

    val titlePreviewSeed: Long = System.nanoTime() xor System.currentTimeMillis()

    override fun onCreate() {
        super.onCreate()
        val catalog = AndroidPuzzleCatalog.load(this)
        puzzleCatalog = catalog.puzzles
        previewPuzzleCatalog = catalog.previewCandidates
        dataContainer = JigsawDataContainer(this, applicationScope)
        applicationScope.launch {
            dataContainer.initialize()
        }
    }
}
