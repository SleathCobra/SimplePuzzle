package com.qtpie.simplepuzzle.core.data.learning

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.qtpie.simplepuzzle.core.data.progress.JigsawMathDatabase
import com.qtpie.simplepuzzle.core.learning.ActivityId
import com.qtpie.simplepuzzle.core.learning.ActivityVersion
import com.qtpie.simplepuzzle.core.learning.ApplicationProvenance
import com.qtpie.simplepuzzle.core.learning.AssistanceSummary
import com.qtpie.simplepuzzle.core.learning.AttemptId
import com.qtpie.simplepuzzle.core.learning.AttemptOutcome
import com.qtpie.simplepuzzle.core.learning.AuthoredDifficultyBand
import com.qtpie.simplepuzzle.core.learning.ContentVersion
import com.qtpie.simplepuzzle.core.learning.GeneratorProvenance
import com.qtpie.simplepuzzle.core.learning.Grade2Quarter1Skills
import com.qtpie.simplepuzzle.core.learning.IntegerResponse
import com.qtpie.simplepuzzle.core.learning.LearningAttempt
import com.qtpie.simplepuzzle.core.learning.LearningItemId
import com.qtpie.simplepuzzle.core.learning.LearningItemRef
import com.qtpie.simplepuzzle.core.learning.RepresentationType
import com.qtpie.simplepuzzle.core.learning.SessionId
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LearningDaoTest {
    private lateinit var database: JigsawMathDatabase
    private lateinit var dao: LearningDao

    @Before
    fun createDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            JigsawMathDatabase::class.java,
        ).build()
        dao = database.learningDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun duplicateAttemptIsIgnoredAndSessionCountsStayConsistent() = runTest {
        val attempt = attempt("attempt-dao-001", AttemptOutcome.CORRECT, 9)

        assertTrue(dao.recordAttempt(attempt.toEntity(), attempt.outcome))
        assertFalse(dao.recordAttempt(attempt.toEntity(), attempt.outcome))

        assertEquals(1, dao.attemptCount())
        val session = requireNotNull(dao.getSession(attempt.sessionId.value))
        assertEquals(1, session.attemptCount)
        assertEquals(1, session.correctCount)
        assertEquals(0, session.incorrectCount)
    }

    @Test
    fun correctAndIncorrectAttemptsUpdateSessionInSameTransaction() = runTest {
        val correct = attempt("attempt-dao-001", AttemptOutcome.CORRECT, 9)
        val incorrect = attempt("attempt-dao-002", AttemptOutcome.INCORRECT, 8)

        dao.recordAttempt(correct.toEntity(), correct.outcome)
        dao.recordAttempt(incorrect.toEntity(), incorrect.outcome)

        val session = requireNotNull(dao.getSession(correct.sessionId.value))
        assertEquals(2, session.attemptCount)
        assertEquals(1, session.correctCount)
        assertEquals(1, session.incorrectCount)
    }

    private fun attempt(id: String, outcome: AttemptOutcome, response: Int) = LearningAttempt(
        attemptId = AttemptId(id),
        sessionId = SessionId("session-dao-001"),
        activityId = ActivityId("jm.activity.jigsaw-math"),
        activityVersion = ActivityVersion(1),
        item = LearningItemRef(
            itemId = LearningItemId("jm.item.dao.001"),
            templateId = "jm.template.dao",
            activityId = ActivityId("jm.activity.jigsaw-math"),
            activityVersion = ActivityVersion(1),
            contentVersion = ContentVersion(1),
            primarySkillId = Grade2Quarter1Skills.ADD_WITHOUT_REGROUPING,
            authoredDifficulty = AuthoredDifficultyBand.CORE,
            representation = RepresentationType.SYMBOLIC_EQUATION,
            provenance = GeneratorProvenance("test", 1, 1, mapOf("difficulty" to "MEDIUM")),
        ),
        selectedResponse = IntegerResponse(response),
        expectedResponse = IntegerResponse(9),
        outcome = outcome,
        assistance = AssistanceSummary(),
        attemptOrdinal = 1,
        occurredAtEpochMillis = if (id.endsWith("1")) 10 else 20,
        applicationProvenance = ApplicationProvenance("test", 1),
    )
}
