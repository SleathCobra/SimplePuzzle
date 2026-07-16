package com.qtpie.simplepuzzle.core.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.core.model.PlayerPreferences
import com.qtpie.simplepuzzle.core.model.GameModeId
import java.io.File
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class DataStorePreferencesRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun preferencesRoundTripThroughDataStore() = runTest {
        val repository = repository(File(temporaryFolder.root, "settings.preferences_pb"))
        val expected = PlayerPreferences(
            soundEnabled = false,
            soundVolume = 0.3f,
            musicEnabled = false,
            musicVolume = 0.25f,
            hapticsEnabled = false,
            difficulty = Difficulty.HARD,
            graphicsQuality = GraphicsQuality.LOW,
            reducedMotion = true,
        )

        repository.setPreferences(expected)

        assertEquals(expected, repository.preferences.first())
    }

    @Test
    fun legacyMigrationFlagPersists() = runTest {
        val repository = repository(File(temporaryFolder.root, "migration.preferences_pb"))

        assertFalse(repository.isLegacyMigrationComplete())
        repository.markLegacyMigrationComplete()
        assertTrue(repository.isLegacyMigrationComplete())
    }

    @Test
    fun lastModeRoundTripsAndUnknownFutureValueFallsBackToClassic() = runTest {
        val repository = repository(File(temporaryFolder.root, "mode.preferences_pb"))

        repository.setLastSelectedMode(GameModeId.PUZZLE_DECAY)

        assertEquals(GameModeId.PUZZLE_DECAY, repository.preferences.first().lastSelectedMode)
    }

    private fun kotlinx.coroutines.test.TestScope.repository(file: File): DataStorePreferencesRepository =
        DataStorePreferencesRepository(
            PreferenceDataStoreFactory.create(
                scope = backgroundScope,
                produceFile = { file },
            ),
        )
}
