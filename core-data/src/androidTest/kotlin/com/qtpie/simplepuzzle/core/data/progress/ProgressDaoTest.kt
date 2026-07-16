package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.GameModeCatalog
import com.qtpie.simplepuzzle.core.model.GameModeId
import com.qtpie.simplepuzzle.core.model.ModeOutcome
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.Score
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressDaoTest {
    private lateinit var database: JigsawMathDatabase
    private lateinit var dao: ProgressDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            JigsawMathDatabase::class.java,
        ).build()
        dao = database.progressDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun completionUpdatesProgressAndSessionAtomically() = runTest {
        dao.startAttempt("cosmic", totalPieces = 30, now = 10L)
        dao.saveProgress("cosmic", revealedPieces = 12, totalPieces = 30, score = 100, now = 20L)

        dao.completePuzzle(
            GameSessionSummary(
                puzzleId = PuzzleId("cosmic"),
                score = Score(300),
                correctAnswers = 30,
                incorrectAnswers = 2,
                durationMillis = 90_000,
                completed = true,
                endedAtEpochMillis = 100L,
            ),
        )

        val progress = requireNotNull(dao.get("cosmic"))
        assertTrue(progress.completed)
        assertEquals(30, progress.revealedPieces)
        assertEquals(300, progress.bestScore)
        assertEquals(1, dao.sessionCount())
    }

    @Test
    fun resetRemovesProgressAndSessions() = runTest {
        dao.startAttempt("cosmic", totalPieces = 1, now = 10L)
        dao.completePuzzle(
            GameSessionSummary(
                puzzleId = PuzzleId("cosmic"),
                score = Score(10),
                correctAnswers = 1,
                incorrectAnswers = 0,
                durationMillis = 1_000,
                completed = true,
                endedAtEpochMillis = 20L,
            ),
        )

        dao.resetAll()

        assertEquals(null, dao.get("cosmic"))
        assertEquals(0, dao.sessionCount())
        assertTrue(dao.getModeBests().isEmpty())
    }

    @Test
    fun challengeSessionAndIndependentBestMetricsArePersisted() = runTest {
        dao.startAttempt("cosmic", totalPieces = 2, now = 10L)
        val first = GameSessionSummary(
            puzzleId = PuzzleId("cosmic"),
            score = Score(200),
            correctAnswers = 3,
            incorrectAnswers = 4,
            durationMillis = 80_000,
            completed = true,
            endedAtEpochMillis = 100L,
            modeId = GameModeId.PUZZLE_DECAY,
            outcome = ModeOutcome.COMPLETED,
            timeoutCount = 1,
            maximumCombo = 4,
            piecesRevealed = 3,
            piecesRemoved = 1,
        )
        dao.completePuzzle(first)
        dao.completePuzzle(
            first.copy(
                score = Score(180),
                incorrectAnswers = 1,
                timeoutCount = 0,
                maximumCombo = 7,
                durationMillis = 60_000,
                endedAtEpochMillis = 200L,
            ),
        )

        val sessions = dao.getSessions()
        val best = requireNotNull(dao.getModeBest("cosmic", "puzzle-decay"))
        assertEquals(2, sessions.size)
        assertEquals("puzzle-decay", sessions.first().modeId)
        assertEquals(1, sessions.first().piecesRemoved)
        assertEquals(200, best.bestScore)
        assertEquals(60_000L, best.fastestCompletionMillis)
        assertEquals(7, best.highestCombo)
        assertEquals(1, best.fewestMistakes)
        assertEquals(200L, best.mostRecentCompletionEpochMillis)
        assertEquals(0, requireNotNull(dao.get("cosmic")).bestScore)
    }

    @Test
    fun failedChallengeDoesNotOverwriteProgressAndUnknownModeValueIsRetained() = runTest {
        dao.startAttempt("cosmic", totalPieces = 30, now = 10L)
        dao.saveProgress("cosmic", revealedPieces = 12, totalPieces = 30, score = 150, now = 20L)
        dao.recordSession(
            GameSessionSummary(
                puzzleId = PuzzleId("cosmic"),
                score = Score(90),
                correctAnswers = 8,
                incorrectAnswers = 2,
                durationMillis = 45_000,
                completed = false,
                endedAtEpochMillis = 30L,
                modeId = GameModeId.COMBO_RUSH,
                outcome = ModeOutcome.TIME_EXPIRED,
                maximumCombo = 3,
                piecesRevealed = 8,
            ),
        )
        dao.insertSession(
            GameSessionEntity(
                puzzleId = "cosmic",
                score = 1,
                correctAnswers = 0,
                incorrectAnswers = 0,
                durationMillis = 1,
                completed = false,
                endedAtEpochMillis = 31,
                modeId = "future-mode",
                outcome = "future-outcome",
            ),
        )

        val progress = requireNotNull(dao.get("cosmic"))
        val unknown = dao.getSessions().last()
        assertEquals(12, progress.revealedPieces)
        assertEquals(150, progress.bestScore)
        assertEquals("future-mode", unknown.modeId)
        assertEquals(GameModeId.CLASSIC, GameModeCatalog.idOrClassic(unknown.modeId))
    }
}
