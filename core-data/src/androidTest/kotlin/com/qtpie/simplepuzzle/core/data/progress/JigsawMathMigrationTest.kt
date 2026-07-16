package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.IOException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JigsawMathMigrationTest {
    private val databaseName = "jigsaw-math-migration-test"

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        JigsawMathDatabase::class.java,
    )

    @After
    fun deleteDatabase() {
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(databaseName)
    }

    @Test
    @Throws(IOException::class)
    fun migration1To2PreservesClassicProgressAndSession() {
        helper.createDatabase(databaseName, 1).apply {
            execSQL(
                """
                INSERT INTO puzzle_progress (
                    puzzleId, revealedPieces, totalPieces, completed, bestScore,
                    attempts, completedAtEpochMillis, lastPlayedAtEpochMillis
                ) VALUES ('puzzle-1', 30, 30, 1, 420, 3, 1000, 1200)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO game_sessions (
                    id, puzzleId, score, correctAnswers, incorrectAnswers,
                    durationMillis, completed, endedAtEpochMillis
                ) VALUES (7, 'puzzle-1', 420, 30, 2, 90000, 1, 1200)
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            databaseName,
            2,
            true,
            JigsawMathMigrations.Migration1To2,
        )

        migrated.query("SELECT * FROM puzzle_progress WHERE puzzleId = 'cosmic-journey'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(30, cursor.getInt(cursor.getColumnIndexOrThrow("revealedPieces")))
            assertEquals(420, cursor.getInt(cursor.getColumnIndexOrThrow("bestScore")))
            assertEquals(3, cursor.getInt(cursor.getColumnIndexOrThrow("attempts")))
        }
        migrated.query("SELECT * FROM game_sessions WHERE id = 7").use { cursor ->
            cursor.moveToFirst()
            assertEquals("classic", cursor.getString(cursor.getColumnIndexOrThrow("modeId")))
            assertEquals("completed", cursor.getString(cursor.getColumnIndexOrThrow("outcome")))
            assertEquals(30, cursor.getInt(cursor.getColumnIndexOrThrow("piecesRevealed")))
            assertEquals(0, cursor.getInt(cursor.getColumnIndexOrThrow("piecesRemoved")))
            assertEquals(1, cursor.getInt(cursor.getColumnIndexOrThrow("maximumCombo")))
            assertEquals("cosmic-journey", cursor.getString(cursor.getColumnIndexOrThrow("puzzleId")))
        }
        migrated.close()
    }
}
