package com.qtpie.simplepuzzle.core.data.progress

import androidx.room.Database
import androidx.room.RoomDatabase
import com.qtpie.simplepuzzle.core.data.learning.LearningAttemptEntity
import com.qtpie.simplepuzzle.core.data.learning.LearningDao
import com.qtpie.simplepuzzle.core.data.learning.LearningSessionEntity

@Database(
    entities = [
        PuzzleProgressEntity::class,
        GameSessionEntity::class,
        LearningAttemptEntity::class,
        LearningSessionEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class JigsawMathDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao

    abstract fun learningDao(): LearningDao
}
