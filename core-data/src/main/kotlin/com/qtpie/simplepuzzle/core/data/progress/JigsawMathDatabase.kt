package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [PuzzleProgressEntity::class, GameSessionEntity::class, ModeBestEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class JigsawMathDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
}

object JigsawMathMigrations {
    val Migration1To2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN modeId TEXT NOT NULL DEFAULT 'classic'",
            )
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN outcome TEXT NOT NULL DEFAULT 'abandoned'",
            )
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN timeoutCount INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN maximumCombo INTEGER NOT NULL DEFAULT 1",
            )
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN piecesRevealed INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN piecesRemoved INTEGER NOT NULL DEFAULT 0",
            )
            db.execSQL(
                "ALTER TABLE game_sessions ADD COLUMN livesRemaining INTEGER",
            )
            db.execSQL(
                "UPDATE game_sessions SET outcome = CASE WHEN completed = 1 THEN 'completed' ELSE 'abandoned' END, piecesRevealed = correctAnswers",
            )
            db.execSQL(
                """
                INSERT OR IGNORE INTO puzzle_progress (
                    puzzleId, revealedPieces, totalPieces, completed, bestScore,
                    attempts, completedAtEpochMillis, lastPlayedAtEpochMillis
                )
                SELECT 'cosmic-journey', revealedPieces, totalPieces, completed, bestScore,
                    attempts, completedAtEpochMillis, lastPlayedAtEpochMillis
                FROM puzzle_progress WHERE puzzleId = 'puzzle-1'
                """.trimIndent(),
            )
            db.execSQL(
                "UPDATE game_sessions SET puzzleId = 'cosmic-journey' WHERE puzzleId = 'puzzle-1'",
            )
            db.execSQL("DELETE FROM puzzle_progress WHERE puzzleId = 'puzzle-1'")
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS mode_bests (
                    puzzleId TEXT NOT NULL,
                    modeId TEXT NOT NULL,
                    bestScore INTEGER NOT NULL,
                    fastestCompletionMillis INTEGER,
                    highestCombo INTEGER NOT NULL,
                    fewestMistakes INTEGER,
                    mostRecentCompletionEpochMillis INTEGER,
                    PRIMARY KEY(puzzleId, modeId),
                    FOREIGN KEY(puzzleId) REFERENCES puzzle_progress(puzzleId) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS index_mode_bests_puzzleId ON mode_bests(puzzleId)",
            )
        }
    }
}
