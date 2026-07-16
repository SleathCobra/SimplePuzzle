package com.qtpie.simplepuzzle.audio

import android.content.Context
import android.media.MediaPlayer
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.model.BackgroundMusicMode
import com.qtpie.simplepuzzle.model.UserSettings
import kotlinx.coroutines.*

class MusicManager(context: Context) {
    private val context = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var fadeJob: Job? = null
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
                val pool = musicMap.values.filter { it != currentMusicRes }
                if (pool.isEmpty()) musicMap.values.random() else pool.random()
            }
            else -> musicMap[settings.backgroundMusicMode] ?: R.raw.bg1
        }

        playMusic(nextRes)
    }

    private fun playMusic(resId: Int) {
        // Only return early if we are NOT in random mode. 
        // In random mode, we might have been called to loop or switch.
        if (currentSettings?.backgroundMusicMode != BackgroundMusicMode.Random && 
            currentMusicRes == resId && mediaPlayer?.isPlaying == true) return
        
        val fadeOutDuration = 1000L
        val fadeInDuration = 1500L
        
        fadeJob?.cancel()
        fadeJob = scope.launch {
            // Fade out current
            mediaPlayer?.let { player ->
                val startVol = currentSettings?.backgroundMusicVolume ?: 0.5f
                val steps = 20
                for (i in steps downTo 0) {
                    val vol = startVol * (i.toFloat() / steps)
                    try {
                        player.setVolume(vol, vol)
                    } catch (_: IllegalStateException) {
                        return@let
                    }
                    delay(fadeOutDuration / steps)
                }
            }

            stopMusic()
            currentMusicRes = resId
            playCount = 0

            val preparedPlayer = withContext(Dispatchers.IO) {
                MediaPlayer.create(context, resId)
            }
            if (!isActive) {
                preparedPlayer?.release()
                return@launch
            }
            mediaPlayer = preparedPlayer?.apply {
                val targetVol = currentSettings?.backgroundMusicVolume ?: 0.5f
                setVolume(0f, 0f)
                isLooping = false
                setOnCompletionListener {
                    playCount++
                    val minPlays = minPlaysBeforeSwitch[currentMusicRes] ?: 1
                    
                    if (currentSettings?.backgroundMusicMode == BackgroundMusicMode.Random) {
                        if (playCount >= minPlays) {
                            startNewMusic()
                        } else {
                            it.start()
                        }
                    } else {
                        it.start()
                    }
                }
                start()
                
                // Fade in
                val steps = 30
                for (i in 0..steps) {
                    val vol = targetVol * (i.toFloat() / steps)
                    try {
                        setVolume(vol, vol)
                    } catch (_: IllegalStateException) {
                        break
                    }
                    delay(fadeInDuration / steps)
                }
            }
        }
    }

    private fun stopMusic() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        currentMusicRes = 0
    }

    fun release() {
        fadeJob?.cancel()
        scope.cancel()
        stopMusic()
    }
}
