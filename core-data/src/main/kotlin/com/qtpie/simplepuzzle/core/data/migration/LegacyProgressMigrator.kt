package com.qtpie.simplepuzzle.core.data.migration

import com.qtpie.simplepuzzle.core.data.preferences.PreferencesRepository
import com.qtpie.simplepuzzle.core.data.progress.ProgressRepository
import com.qtpie.simplepuzzle.core.model.PuzzleProgress

fun interface LegacyProgressSource {
    suspend fun readProgress(): List<PuzzleProgress>
}

object EmptyLegacyProgressSource : LegacyProgressSource {
    override suspend fun readProgress(): List<PuzzleProgress> = emptyList()
}

class LegacyProgressMigrator(
    private val source: LegacyProgressSource,
    private val progressRepository: ProgressRepository,
    private val preferencesRepository: PreferencesRepository,
) {
    suspend fun migrateIfNeeded() {
        if (preferencesRepository.isLegacyMigrationComplete()) return

        source.readProgress().forEach { progressRepository.restore(it) }
        preferencesRepository.markLegacyMigrationComplete()
    }
}
