package com.qtpie.simplepuzzle.core.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.qtpie.simplepuzzle.core.data.migration.EmptyLegacyProgressSource
import com.qtpie.simplepuzzle.core.data.migration.LegacyProgressMigrator
import com.qtpie.simplepuzzle.core.data.preferences.DataStorePreferencesRepository
import com.qtpie.simplepuzzle.core.data.preferences.PreferencesRepository
import com.qtpie.simplepuzzle.core.data.progress.JigsawMathDatabase
import com.qtpie.simplepuzzle.core.data.progress.JigsawMathMigrations
import com.qtpie.simplepuzzle.core.data.progress.ProgressRepository
import com.qtpie.simplepuzzle.core.data.progress.RoomProgressRepository
import kotlinx.coroutines.CoroutineScope

class JigsawDataContainer(
    context: Context,
    applicationScope: CoroutineScope,
) {
    private val applicationContext = context.applicationContext

    private val database: JigsawMathDatabase = Room.databaseBuilder(
        applicationContext,
        JigsawMathDatabase::class.java,
        DATABASE_NAME,
    ).addMigrations(JigsawMathMigrations.Migration1To2).build()

    private val preferencesDataStore = PreferenceDataStoreFactory.create(
        scope = applicationScope,
        produceFile = { applicationContext.preferencesDataStoreFile(PREFERENCES_FILE) },
    )

    val progressRepository: ProgressRepository = RoomProgressRepository(database.progressDao())
    val preferencesRepository: PreferencesRepository =
        DataStorePreferencesRepository(preferencesDataStore)

    private val legacyProgressMigrator = LegacyProgressMigrator(
        source = EmptyLegacyProgressSource,
        progressRepository = progressRepository,
        preferencesRepository = preferencesRepository,
    )

    suspend fun initialize() {
        legacyProgressMigrator.migrateIfNeeded()
    }

    private companion object {
        const val DATABASE_NAME = "jigsaw-math.db"
        const val PREFERENCES_FILE = "jigsaw-math.preferences_pb"
    }
}
