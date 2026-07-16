package com.qtpie.simplepuzzle.core.data.migration

import com.qtpie.simplepuzzle.core.data.preferences.PreferencesRepository
import com.qtpie.simplepuzzle.core.data.progress.ProgressRepository
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.ModeBest
import com.qtpie.simplepuzzle.core.model.PlayerPreferences
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.PuzzleProgress
import com.qtpie.simplepuzzle.core.model.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyProgressMigratorTest {
    @Test
    fun importsLegacyRowsOnlyOnce() = runTest {
        val legacy = PuzzleProgress(
            puzzleId = PuzzleId("legacy"),
            revealedPieces = 30,
            totalPieces = 30,
            completed = true,
            bestScore = Score(450),
            attempts = 3,
            completedAtEpochMillis = 100L,
            lastPlayedAtEpochMillis = 100L,
        )
        val progressRepository = RecordingProgressRepository()
        val preferencesRepository = FakePreferencesRepository()
        val migrator = LegacyProgressMigrator(
            source = LegacyProgressSource { listOf(legacy) },
            progressRepository = progressRepository,
            preferencesRepository = preferencesRepository,
        )

        migrator.migrateIfNeeded()
        migrator.migrateIfNeeded()

        assertEquals(listOf(legacy), progressRepository.restored)
        assertTrue(preferencesRepository.migrationComplete)
    }

    private class RecordingProgressRepository : ProgressRepository {
        override val progress: Flow<List<PuzzleProgress>> = MutableStateFlow(emptyList())
        val restored = mutableListOf<PuzzleProgress>()

        override suspend fun startAttempt(puzzleId: PuzzleId, totalPieces: Int) = Unit
        override suspend fun saveProgress(puzzleId: PuzzleId, revealedPieces: Int, totalPieces: Int, score: Score) = Unit
        override suspend fun completePuzzle(summary: GameSessionSummary) = Unit
        override suspend fun recordSession(summary: GameSessionSummary) = Unit
        override suspend fun getModeBests(): List<ModeBest> = emptyList()
        override suspend fun restore(progress: PuzzleProgress) {
            restored += progress
        }
        override suspend fun resetAll() = Unit
    }

    private class FakePreferencesRepository : PreferencesRepository {
        override val preferences: Flow<PlayerPreferences> = MutableStateFlow(PlayerPreferences())
        var migrationComplete = false

        override suspend fun setPreferences(preferences: PlayerPreferences) = Unit
        override suspend fun setLastSelectedMode(modeId: GameModeId) = Unit
        override suspend fun isLegacyMigrationComplete(): Boolean = migrationComplete
        override suspend fun markLegacyMigrationComplete() {
            migrationComplete = true
        }
    }
}
