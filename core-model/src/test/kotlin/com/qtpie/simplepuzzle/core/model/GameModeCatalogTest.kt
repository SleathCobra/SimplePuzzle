package com.qtpie.simplepuzzle.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GameModeCatalogTest {
    @Test
    fun catalogContainsEveryUniqueStableModeIdWithClassicFirst() {
        val definitions = GameModeCatalog.all

        assertEquals(GameModeId.entries.size, definitions.size)
        assertEquals(definitions.size, definitions.map { it.id }.distinct().size)
        assertEquals(GameModeId.CLASSIC, definitions.first().id)
        assertTrue(definitions.first().recommended)
        assertTrue(definitions.first().clock.isUntimed)
    }

    @Test
    fun unknownPersistedModeFallsBackToClassic() {
        assertEquals(GameModeId.CLASSIC, GameModeCatalog.idOrClassic("future-mode"))
        assertEquals(GameModeCatalog.Classic, GameModeCatalog.definitionOrClassic("future-mode"))
        assertEquals(GameModeCatalog.Classic, GameModeCatalog.definitionOrClassic(null))
    }

    @Test
    fun requiredModeTuningIsCentralAndExact() {
        assertEquals(90_000L, GameModeCatalog.TimeAttack.clock.initialSessionMillis)
        assertEquals(3, GameModeCatalog.Survival.mistakes.startingHearts)
        assertEquals(10_000L, GameModeCatalog.Survival.clock.questionDeadlineMillis?.easyMillis)
        assertEquals(8_000L, GameModeCatalog.Survival.clock.questionDeadlineMillis?.mediumMillis)
        assertEquals(6_000L, GameModeCatalog.Survival.clock.questionDeadlineMillis?.hardMillis)
        assertEquals(9_000L, GameModeCatalog.PuzzleDecay.clock.decayIntervalMillis)
        assertEquals(180_000L, GameModeCatalog.PuzzleDecay.clock.maximumSessionMillis)
        assertEquals(45_000L, GameModeCatalog.ComboRush.clock.initialSessionMillis)
        assertEquals(2_500L, GameModeCatalog.ComboRush.clock.correctAnswerBonusMillis)
        assertEquals(4_000L, GameModeCatalog.ComboRush.clock.wrongAnswerPenaltyMillis)
        assertEquals(60_000L, GameModeCatalog.ComboRush.clock.maximumRemainingMillis)
    }

    @Test
    fun configurationRejectsMismatchedResolvedRules() {
        assertThrows(IllegalArgumentException::class.java) {
            GameConfiguration(
                puzzleId = PuzzleId("test"),
                pieceCount = 1,
                difficulty = Difficulty.EASY,
                modeId = GameModeId.TIME_ATTACK,
                modeRules = GameModeCatalog.Classic,
            )
        }
    }
}
