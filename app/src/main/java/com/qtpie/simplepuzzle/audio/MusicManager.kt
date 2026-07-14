package com.qtpie.simplepuzzle.audio

import android.content.Context
import android.media.MediaPlayer
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.model.BackgroundMusicMode
import com.qtpie.simplepuzzle.model.UserSettings
import kotlinx.coroutines.*

class MusicManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentSettings: UserSettings? = null
    private var currentMusicRes: Int = 0
    private var playCount = 0
    private val minPlaysBeforeSwitch = mapOf(
        R.raw.bg1 to 1,
        R.raw.bg2 to 1,
        R.raw.bg3 to 1,
        R.raw.bg4 to 5 // 11s loop, play at least 5 times (~55s)
    )

    private val musicMap = mapOf(
        BackgroundMusicMode.Music1 to R.raw.bg1,
        BackgroundMusicMode.Music2 to R.raw.bg2,
        BackgroundMusicMode.Music3 to R.raw.bg3,
        BackgroundMusicMode.Music4 to R.raw.bg4
    )

    fun updateSettings(settings: UserSettings) {
        val oldSettings = currentSettings
        currentSettings = settings

        if (!settings.backgroundMusicEnabled) {
            stopMusic()
            return
        }

        if (oldSettings?.backgroundMusicEnabled == false || mediaPlayer == null) {
            startNewMusic()
        } else if (oldSettings?.backgroundMusicMode != settings.backgroundMusicMode) {
            if (settings.backgroundMusicMode != BackgroundMusicMode.Random) {
                startNewMusic()
            }
        } else {
            mediaPlayer?.setVolume(settings.backgroundMusicVolume, settings.backgroundMusicVolume)
        }
    }

    private fun startNewMusic() {
        val settings = currentSettings ?: return
        if (!settings.backgroundMusicEnabled) return

        val nextRes = when (settings.backgroundMusicMode) {
            BackgroundMusicMode.Random -> {
                val pool = musicMap.values.toList()
                pool.random()
            }
            else -> musicMap[settings.backgroundMusicMode] ?: R.raw.bg1
        }

        playMusic(nextRes)
    }

    private fun playMusic(resId: Int) {
        if (currentMusicRes == resId && mediaPlayer?.isPlaying == true) return
        
        stopMusic()
        currentMusicRes = resId
        playCount = 0
        
        mediaPlayer = MediaPlayer.create(context, resId).apply {
            isLooping = false // We handle looping manually to count plays
            setVolume(currentSettings?.backgroundMusicVolume ?: 0.5f, currentSettings?.backgroundMusicVolume ?: 0.5f)
            setOnCompletionListener {
                playCount++
                val minPlays = minPlaysBeforeSwitch[currentMusicRes] ?: 1
                
                if (currentSettings?.backgroundMusicMode == BackgroundMusicMode.Random) {
                    if (playCount >= minPlays) {
                        startNewMusic()
                    } else {
                        it.start() // Loop same track
                    }
                } else {
                    it.start() // Constant loop for selected track
                }
            }
            start()
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusicRes = 0
    }

    fun release() {
        stopMusic()
    }
}
