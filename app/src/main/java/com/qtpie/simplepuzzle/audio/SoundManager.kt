package com.qtpie.simplepuzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.viewmodel.SoundEffect

class SoundManager(context: Context) {
    private val context = context.applicationContext
    private val soundPool: SoundPool
    private val lock = Any()
    private val loadedSoundIds = mutableMapOf<SoundEffect, MutableList<Int>>()
    private val effectBySampleId = mutableMapOf<Int, SoundEffect>()
    private val requestedEffects = mutableSetOf<SoundEffect>()
    private val pendingVolume = mutableMapOf<SoundEffect, Float>()
    private var released = false

    private val resourcesByEffect = mapOf(
        SoundEffect.MENU_CLICK to intArrayOf(R.raw.menuclick1, R.raw.menuclick2),
        SoundEffect.PUZZLE_LOCKED to intArrayOf(R.raw.locked_puzzle),
        SoundEffect.PUZZLE_UNLOCKED to intArrayOf(R.raw.unlock_puzzle),
        SoundEffect.ANSWER_CORRECT to intArrayOf(R.raw.right_answer),
        SoundEffect.ANSWER_WRONG to intArrayOf(R.raw.wrong_answer1, R.raw.wrong_answer2),
        SoundEffect.PIECE_PLACED to intArrayOf(
            R.raw.puzzle_piece_added1,
            R.raw.puzzle_piece_added2,
            R.raw.puzzle_piece_added3,
            R.raw.puzzle_piece_added4,
            R.raw.puzzle_piece_added5,
        ),
        SoundEffect.PIECE_REMOVED to intArrayOf(
            R.raw.puzzle_piece_removed1,
            R.raw.puzzle_piece_removed2,
            R.raw.puzzle_piece_removed3,
        ),
        SoundEffect.COINS_ADDED to intArrayOf(R.raw.money_added),
        SoundEffect.GAME_COMPLETE to intArrayOf(R.raw.money_added),
        SoundEffect.NAVIGATION to intArrayOf(R.raw.navigationchange),
        SoundEffect.SWITCH_ON to intArrayOf(R.raw.switch_on),
        SoundEffect.SWITCH_OFF to intArrayOf(R.raw.switch_off),
        SoundEffect.SWITCH_TOGGLE to intArrayOf(R.raw.switch_toggle),
        SoundEffect.MANY_FAILS to intArrayOf(R.raw.many_fails),
        SoundEffect.HEART_LOSS to intArrayOf(R.raw.wrong_answer2),
        SoundEffect.TIME_BONUS to intArrayOf(R.raw.money_added),
        SoundEffect.MODE_FAILED to intArrayOf(R.raw.many_fails),
    )

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            val playback = synchronized(lock) {
                if (released) return@setOnLoadCompleteListener
                val effect = effectBySampleId.remove(sampleId)
                    ?: return@setOnLoadCompleteListener
                if (status != 0) return@setOnLoadCompleteListener

                loadedSoundIds.getOrPut(effect, ::mutableListOf).add(sampleId)
                pendingVolume.remove(effect)?.let { volume -> sampleId to volume }
            }
            playback?.let { (soundId, volume) -> playLoaded(soundId, volume) }
        }
    }

    fun playSound(effect: SoundEffect, settingsVolume: Float = 1.0f) {
        val baseVolume = when (effect) {
            SoundEffect.PIECE_PLACED, SoundEffect.PIECE_REMOVED -> 0.2f
            else -> 1.0f
        }
        val volume = (baseVolume * settingsVolume).coerceIn(0f, 1f)
        if (volume == 0f) return

        val loadedSound = synchronized(lock) {
            if (released) return

            loadedSoundIds[effect]?.takeIf { it.isNotEmpty() }?.random()
                ?: run {
                    pendingVolume[effect] = volume
                    if (requestedEffects.add(effect)) {
                        var requestedSampleCount = 0
                        (resourcesByEffect[effect] ?: intArrayOf()).forEach { resourceId ->
                            val sampleId = soundPool.load(context, resourceId, 1)
                            if (sampleId != 0) {
                                effectBySampleId[sampleId] = effect
                                requestedSampleCount++
                            }
                        }
                        if (requestedSampleCount == 0) {
                            requestedEffects.remove(effect)
                            pendingVolume.remove(effect)
                        }
                    }
                    null
                }
        }
        loadedSound?.let { playLoaded(it, volume) }
    }

    private fun playLoaded(soundId: Int, volume: Float) {
        synchronized(lock) {
            if (released) return
            soundPool.play(soundId, volume, volume, 0, 0, 1f)
        }
    }

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            pendingVolume.clear()
            effectBySampleId.clear()
            loadedSoundIds.clear()
            requestedEffects.clear()
            soundPool.release()
        }
    }
}
