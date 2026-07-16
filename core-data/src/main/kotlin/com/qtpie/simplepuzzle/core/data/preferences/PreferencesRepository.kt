package com.qtpie.simplepuzzle.core.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.qtpie.simplepuzzle.core.model.Difficulty
import com.qtpie.simplepuzzle.core.model.GraphicsQuality
import com.qtpie.simplepuzzle.core.model.PlayerPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

interface PreferencesRepository {
    val preferences: Flow<PlayerPreferences>

    suspend fun setPreferences(preferences: PlayerPreferences)

    suspend fun isLegacyMigrationComplete(): Boolean

    suspend fun markLegacyMigrationComplete()
}

class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {
    override val preferences: Flow<PlayerPreferences> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map(::toPlayerPreferences)

    override suspend fun setPreferences(preferences: PlayerPreferences) {
        dataStore.edit { values ->
            values[Keys.SoundEnabled] = preferences.soundEnabled
            values[Keys.SoundVolume] = preferences.soundVolume
            values[Keys.MusicEnabled] = preferences.musicEnabled
            values[Keys.MusicVolume] = preferences.musicVolume
            values[Keys.HapticsEnabled] = preferences.hapticsEnabled
            values[Keys.Difficulty] = preferences.difficulty.name
            values[Keys.GraphicsQuality] = preferences.graphicsQuality.name
            values[Keys.ReducedMotion] = preferences.reducedMotion
        }
    }

    override suspend fun isLegacyMigrationComplete(): Boolean =
        dataStore.data.map { it[Keys.LegacyMigrationComplete] ?: false }.first()

    override suspend fun markLegacyMigrationComplete() {
        dataStore.edit { it[Keys.LegacyMigrationComplete] = true }
    }

    private fun toPlayerPreferences(values: Preferences) = PlayerPreferences(
        soundEnabled = values[Keys.SoundEnabled] ?: true,
        soundVolume = values[Keys.SoundVolume] ?: 0.8f,
        musicEnabled = values[Keys.MusicEnabled] ?: true,
        musicVolume = values[Keys.MusicVolume] ?: 0.5f,
        hapticsEnabled = values[Keys.HapticsEnabled] ?: true,
        difficulty = values[Keys.Difficulty].enumOrDefault(Difficulty.MEDIUM),
        graphicsQuality = values[Keys.GraphicsQuality].enumOrDefault(GraphicsQuality.AUTO),
        reducedMotion = values[Keys.ReducedMotion] ?: false,
    )

    private object Keys {
        val SoundEnabled = booleanPreferencesKey("sound_enabled")
        val SoundVolume = floatPreferencesKey("sound_volume")
        val MusicEnabled = booleanPreferencesKey("music_enabled")
        val MusicVolume = floatPreferencesKey("music_volume")
        val HapticsEnabled = booleanPreferencesKey("haptics_enabled")
        val Difficulty = stringPreferencesKey("difficulty")
        val GraphicsQuality = stringPreferencesKey("graphics_quality")
        val ReducedMotion = booleanPreferencesKey("reduced_motion")
        val LegacyMigrationComplete = booleanPreferencesKey("legacy_migration_complete")
    }
}

private inline fun <reified T : Enum<T>> String?.enumOrDefault(default: T): T =
    enumValues<T>().firstOrNull { it.name == this } ?: default
