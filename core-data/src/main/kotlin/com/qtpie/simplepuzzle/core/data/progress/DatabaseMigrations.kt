package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `learning_attempts` (
                `attemptId` TEXT NOT NULL,
                `sessionId` TEXT NOT NULL,
                `activityId` TEXT NOT NULL,
                `activityVersion` INTEGER NOT NULL,
                `skillId` TEXT NOT NULL,
                `templateId` TEXT NOT NULL,
                `occurredAtEpochMillis` INTEGER NOT NULL,
                `supersedesAttemptId` TEXT,
                `payloadJson` TEXT NOT NULL,
                PRIMARY KEY(`attemptId`)
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_attempts_sessionId` ON `learning_attempts` (`sessionId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_attempts_skillId` ON `learning_attempts` (`skillId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_attempts_templateId` ON `learning_attempts` (`templateId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_learning_attempts_occurredAtEpochMillis` ON `learning_attempts` (`occurredAtEpochMillis`)")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `learning_sessions` (
                `sessionId` TEXT NOT NULL,
                `activityId` TEXT NOT NULL,
                `activityVersion` INTEGER NOT NULL,
                `startedAtEpochMillis` INTEGER NOT NULL,
                `lastAttemptAtEpochMillis` INTEGER NOT NULL,
                `attemptCount` INTEGER NOT NULL,
                `correctCount` INTEGER NOT NULL,
                `incorrectCount` INTEGER NOT NULL,
                PRIMARY KEY(`sessionId`)
            )
            """.trimIndent(),
        )
    }
}
