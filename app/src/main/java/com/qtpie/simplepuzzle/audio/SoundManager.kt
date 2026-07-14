package com.qtpie.simplepuzzle.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.qtpie.simplepuzzle.R
import com.qtpie.simplepuzzle.viewmodel.SoundEffect

class SoundManager(private val context: Context) {
    private val soundPool: SoundPool
    private val soundMap = mutableMapOf<SoundEffect, List<Int>>()

    init {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds()
    }

    private fun loadSounds() {
        soundMap[SoundEffect.MENU_CLICK] = listOf(
            soundPool.load(context, R.raw.menuclick1, 1),
            soundPool.load(context, R.raw.menuclick2, 1)
        )
        soundMap[SoundEffect.PUZZLE_LOCKED] = listOf(
            soundPool.load(context, R.raw.locked_puzzle, 1)
        )
        soundMap[SoundEffect.PUZZLE_UNLOCKED] = listOf(
            soundPool.load(context, R.raw.unlock_puzzle, 1)
        )
        soundMap[SoundEffect.ANSWER_CORRECT] = listOf(
            soundPool.load(context, R.raw.right_answer, 1)
        )
        soundMap[SoundEffect.ANSWER_WRONG] = listOf(
            soundPool.load(context, R.raw.wrong_answer1, 1),
            soundPool.load(context, R.raw.wrong_answer2, 1)
        )
        soundMap[SoundEffect.PIECE_PLACED] = listOf(
            soundPool.load(context, R.raw.puzzle_piece_added1, 1),
            soundPool.load(context, R.raw.puzzle_piece_added2, 1),
            soundPool.load(context, R.raw.puzzle_piece_added3, 1),
            soundPool.load(context, R.raw.puzzle_piece_added4, 1),
            soundPool.load(context, R.raw.puzzle_piece_added5, 1)
        )
        soundMap[SoundEffect.PIECE_REMOVED] = listOf(
            soundPool.load(context, R.raw.puzzle_piece_removed1, 1),
            soundPool.load(context, R.raw.puzzle_piece_removed2, 1),
            soundPool.load(context, R.raw.puzzle_piece_removed3, 1)
        )
        soundMap[SoundEffect.COINS_ADDED] = listOf(
            soundPool.load(context, R.raw.money_added, 1)
        )
        soundMap[SoundEffect.GAME_COMPLETE] = listOf(
            soundPool.load(context, R.raw.money_added, 1) // Using money_added as a placeholder if no specific victory sound
        )
        soundMap[SoundEffect.NAVIGATION] = listOf(
            soundPool.load(context, R.raw.navigationchange, 1)
        )
        soundMap[SoundEffect.SWITCH_ON] = listOf(
            soundPool.load(context, R.raw.switch_on, 1)
        )
        soundMap[SoundEffect.SWITCH_OFF] = listOf(
            soundPool.load(context, R.raw.switch_off, 1)
        )
        soundMap[SoundEffect.SWITCH_TOGGLE] = listOf(
            soundPool.load(context, R.raw.switch_toggle, 1)
        )
        soundMap[SoundEffect.MANY_FAILS] = listOf(
            soundPool.load(context, R.raw.many_fails, 1)
        )
    }

    fun playSound(effect: SoundEffect, settingsVolume: Float = 1.0f) {
        val sounds = soundMap[effect]
        if (!sounds.isNullOrEmpty()) {
            val soundId = sounds.random()
            val baseVolume = when (effect) {
                SoundEffect.PIECE_PLACED, SoundEffect.PIECE_REMOVED -> 0.2f
                else -> 1.0f
            }
            val volume = baseVolume * settingsVolume
            soundPool.play(soundId, volume, volume, 0, 0, 1f)
        }
    }

    fun release() {
        soundPool.release()
    }
}
