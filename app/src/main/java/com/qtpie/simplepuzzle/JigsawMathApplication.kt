package com.qtpie.simplepuzzle

import android.app.Application
import com.qtpie.simplepuzzle.core.data.JigsawDataContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class JigsawMathApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var dataContainer: JigsawDataContainer
        private set

    override fun onCreate() {
        super.onCreate()
        dataContainer = JigsawDataContainer(this, applicationScope)
        applicationScope.launch {
            dataContainer.initialize()
        }
    }
}
