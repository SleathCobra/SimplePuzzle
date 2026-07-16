package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class JigsawMathMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun removeOldTestDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @After
    fun removeTestDatabase() {
        context.deleteDatabase(TEST_DATABASE)
    }

    @Test
    fun migrationOneToTwoPreservesProgressAndAddsEmptyLearningTables() = runTest {
        createVersionOneDatabase()

        val database = Room.databaseBuilder(context, JigsawMathDatabase::class.java, TEST_DATABASE)
            .addMigrations(MIGRATION_1_2)
            .build()
        try {
            // Opening the generated Room database validates the complete v2
            // schema after the explicit migration.
            val progress = requireNotNull(database.progressDao().get("puzzle-1"))
            assertEquals(12, progress.revealedPieces)
            assertEquals(120, progress.bestScore)
            assertEquals(2, progress.attempts)
            assertEquals(0, database.learningDao().attemptCount())
            assertEquals(0, database.learningDao().sessionCount())
        } finally {
            database.close()
        }
    }

    private fun createVersionOneDatabase() {
        val configuration = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(TEST_DATABASE)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `puzzle_progress` (
                            `puzzleId` TEXT NOT NULL,
                            `revealedPieces` INTEGER NOT NULL,
                            `totalPieces` INTEGER NOT NULL,
                            `completed` INTEGER NOT NULL,
                            `bestScore` INTEGER NOT NULL,
                            `attempts` INTEGER NOT NULL,
                            `completedAtEpochMillis` INTEGER,
                            `lastPlayedAtEpochMillis` INTEGER NOT NULL,
                            PRIMARY KEY(`puzzleId`)
                        )
                        """.trimIndent(),
                    )
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS `game_sessions` (
                            `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            `puzzleId` TEXT NOT NULL,
                            `score` INTEGER NOT NULL,
                            `correctAnswers` INTEGER NOT NULL,
                            `incorrectAnswers` INTEGER NOT NULL,
                            `durationMillis` INTEGER NOT NULL,
                            `completed` INTEGER NOT NULL,
                            `endedAtEpochMillis` INTEGER NOT NULL,
                            FOREIGN KEY(`puzzleId`) REFERENCES `puzzle_progress`(`puzzleId`) ON UPDATE NO ACTION ON DELETE CASCADE
                        )
                        """.trimIndent(),
                    )
                    db.execSQL("CREATE INDEX IF NOT EXISTS `index_game_sessions_puzzleId` ON `game_sessions` (`puzzleId`)")
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
            })
            .build()
        val helper = FrameworkSQLiteOpenHelperFactory().create(configuration)
        try {
            helper.writableDatabase.execSQL(
                """
                INSERT INTO puzzle_progress (
                    puzzleId, revealedPieces, totalPieces, completed, bestScore,
                    attempts, completedAtEpochMillis, lastPlayedAtEpochMillis
                ) VALUES ('puzzle-1', 12, 30, 0, 120, 2, NULL, 44)
                """.trimIndent(),
            )
        } finally {
            helper.close()
        }
    }

    private companion object {
        const val TEST_DATABASE = "academy-migration-test"
    }
}
