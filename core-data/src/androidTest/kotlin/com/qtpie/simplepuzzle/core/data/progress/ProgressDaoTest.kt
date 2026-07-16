package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qtpie.simplepuzzle.core.model.GameSessionSummary
import com.qtpie.simplepuzzle.core.model.PuzzleId
import com.qtpie.simplepuzzle.core.model.Score
import com.qtpie.simplepuzzle.core.data.learning.LearningAttemptEntity
import com.qtpie.simplepuzzle.core.learning.AttemptOutcome
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
        database.learningDao().recordAttempt(
            LearningAttemptEntity(
                attemptId = "attempt-reset-001",
                sessionId = "session-reset-001",
                activityId = "jm.activity.jigsaw-math",
                activityVersion = 1,
                skillId = "math.addition.to-1000-without-regrouping",
                templateId = "jm.template.reset",
                occurredAtEpochMillis = 10,
                supersedesAttemptId = null,
                payloadJson = "{}",
            ),
            AttemptOutcome.CORRECT,
        )

        dao.resetAll()

        assertEquals(null, dao.get("cosmic"))
        assertEquals(0, dao.sessionCount())
        assertEquals(0, database.learningDao().attemptCount())
        assertEquals(0, database.learningDao().sessionCount())
    }
}
